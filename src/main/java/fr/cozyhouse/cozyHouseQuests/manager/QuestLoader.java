package fr.cozyhouse.cozyHouseQuests.manager;

import fr.cozyhouse.cozyHouseQuests.CozyHouseQuests;
import fr.cozyhouse.cozyHouseQuests.enums.ObjectiveType;
import fr.cozyhouse.cozyHouseQuests.enums.QuestCondition;
import fr.cozyhouse.cozyHouseQuests.enums.RewardType;
import fr.cozyhouse.cozyHouseQuests.enums.TargetType;
import fr.cozyhouse.cozyHouseQuests.model.*;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;


public class QuestLoader {

    private final CozyHouseQuests plugin;
    private final File questFolder;
    private final Logger log;

    private final NamespacedKey ID;

    public QuestLoader(CozyHouseQuests plugin){
        this.questFolder = new File(plugin.getDataFolder(), "Quests");
        this.log = plugin.getLogger();
        this.plugin = plugin;

        if (!questFolder.exists() && !questFolder.mkdirs()) {
            log.severe("Impossible to create Quests/ folder");
        }

        ID = key("id");
    }

    public Map<String, Quest> loadQuests(){
        Map<String, Quest> quests = new LinkedHashMap<>();

        if (!questFolder.exists()) {
            questFolder.mkdirs();
            log.info("[QuestLoader] Dossier /quests/ créé. Ajoutez vos fichiers YAML.");
            return quests;
        }

        File[] files = questFolder.listFiles(f -> f.getName().endsWith(".yml"));
        if (files == null || files.length == 0) {
            log.warning("[QuestLoader] Aucun fichier de quête trouvé dans " + questFolder.getPath());
            return quests;
        }

        for (File file : files) {
            try {
                Quest quest = loadFromFile(file);
                if (quest != null){
                    quests.put(quest.getId(), quest);
                    log.info("[QuestLoader] ✓ Quête chargée : " + quest.getId());
                }
            } catch (Exception e){
                log.severe("[QuestLoader] ✗ Erreur dans " + file.getName() + " : " + e.getMessage());
            }
        }

        log.info("[QuestLoader] " + quests.size() + " quête(s) chargée(s).");
        return quests;
    }

    private Quest loadFromFile(File file){
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String id = config.getString("id");
        String name = config.getString("name");
        String description = config.getString("description");

        if (id == null || id.isBlank()) {
            log.warning("[QuestLoader] 'id' manquant dans " + file.getName() + " — ignoré.");
            return null;
        }
        if (name == null || name.isBlank()) {
            log.warning("[QuestLoader] 'name' manquant dans " + file.getName() + " — ignoré.");
            return null;
        }

        String startStage = config.getString("start_stage", "start");
        boolean repeatable = config.getBoolean("repeatable", false);
        int cooldownSeconds = config.getInt("cooldown_seconds", 0);

        ConfigurationSection stagesSection = config.getConfigurationSection("stages");
        if (stagesSection == null) {
            log.warning("[QuestLoader] Aucun 'stages' dans " + file.getName() + " — ignoré.");
            return null;
        }

        Map<String, QuestStage> stages = new LinkedHashMap<>();
        for (String stageId : stagesSection.getKeys(false)) {
            ConfigurationSection sec = stagesSection.getConfigurationSection(stageId);
            if (sec == null) continue;
            QuestStage stage = parseStage(stageId, sec, file.getName());
            stages.put(stageId, stage);
        }

        if (!stages.containsKey(startStage)) {
            log.severe("[QuestLoader] start_stage '" + startStage + "' introuvable dans "
                    + file.getName() + " — quête ignorée.");
            return null;
        }

        return new Quest(id, name, description, startStage, repeatable, cooldownSeconds, stages);
    }

    private QuestStage parseStage(String stageId, ConfigurationSection sec, String fileName){
        String dialogue = sec.getString("dialogue", null);
        String nextStage = sec.getString("next_stage", null);
        List<QuestObjective> objectives = parseObjectives(sec, stageId, fileName);
        List<QuestChoice>    choices    = parseChoices(sec, stageId, fileName);
        List<QuestReward>    rewards    = parseRewards(sec, stageId, fileName);

        if (choices.isEmpty() && nextStage == null && rewards.isEmpty() && objectives.isEmpty()) {
            log.warning("[QuestLoader] Stage '" + stageId + "' dans " + fileName
                    + " n'a ni objectif, ni choix, ni récompense — il sera un cul-de-sac.");
        }

        return new QuestStage(stageId, dialogue, objectives, choices, nextStage, rewards);
    }

    private List<QuestObjective> parseObjectives(ConfigurationSection stage, String stageId, String fileName){
        List<QuestObjective> list = new ArrayList<>();
        if (!stage.isList("objectives")) return list;

        List<Map<?, ?>> rawList = stage.getMapList("objectives");
        for (int i = 0; i < rawList.size(); i++) {
            Map<?, ?> map = rawList.get(i);
            try {
                String typeStr  = (String) map.get("type");
                int amount      = map.containsKey("amount") ? (int) map.get("amount") : 1;
                String targetstr   = map.containsKey("entity")   ? (String) map.get("entity")
                        : map.containsKey("material") ? (String) map.get("material")
                        : map.containsKey("block")    ? (String) map.get("block")
                        : null;
                String label    = map.containsKey("label") ? (String) map.get("label") : null;
                TargetType target = TargetType.valueOf(targetstr.toUpperCase());
                ObjectiveType type = ObjectiveType.valueOf(typeStr.toUpperCase());
                List<QuestCondition> conditions = parseConditions(map.get("conditions"), stageId, fileName);
                String objId = stageId + "_obj_" + i;


                list.add(new QuestObjective(objId, type, target, amount, label, conditions));
            } catch (Exception e) {
                log.warning("[QuestLoader] Objectif " + i + " invalide dans stage '"
                        + stageId + "' (" + fileName + ") : " + e.getMessage());
            }
        }
        return list;
    }

    private List<QuestChoice> parseChoices(ConfigurationSection stage, String stageId, String fileName) {
        List<QuestChoice> list = new ArrayList<>();
        if (!stage.isList("choices")) return list;

        List<Map<?, ?>> rawList = stage.getMapList("choices");
        for (int i = 0; i < rawList.size(); i++) {
            Map<?, ?> map = rawList.get(i);
            try {
                String text      = (String) map.get("text");
                String nextStage = (String) map.get("next_stage");
                List<QuestCondition> conditions = parseConditions(map.get("conditions"), stageId, fileName);

                if (text == null || nextStage == null) {
                    log.warning("[QuestLoader] Choix " + i + " dans stage '" + stageId
                            + "' (" + fileName + ") manque 'text' ou 'next_stage'.");
                    continue;
                }
                list.add(new QuestChoice(text, nextStage, conditions));
            } catch (Exception e) {
                log.warning("[QuestLoader] Choix " + i + " invalide dans stage '"
                        + stageId + "' (" + fileName + ") : " + e.getMessage());
            }
        }
        return list;
    }

    private List<QuestReward> parseRewards(ConfigurationSection stage, String stageId, String fileName){
        List<QuestReward> list = new ArrayList<>();
        if (!stage.isList("rewards")) return list;

        List<Map<?, ?>> rawList = stage.getMapList("rewards");
        for (int i = 0; i < rawList.size(); i++) {
            Map<?, ?> map = rawList.get(i);
            try {
                String typeStr = (String) map.get("type");
                RewardType type = RewardType.valueOf(typeStr.toUpperCase());

                String material = map.containsKey("material") ? (String) map.get("material") : null;
                int amount      = map.containsKey("amount") ? (int) map.get("amount") : 1;
                String command  = map.containsKey("command") ? (String) map.get("command") : null;

                list.add(new QuestReward(type, material, amount, command));
            } catch (Exception e) {
                log.warning("[QuestLoader] Récompense " + i + " invalide dans stage '"
                        + stageId + "' (" + fileName + ") : " + e.getMessage());
            }
        }
        return list;
    }

    private List<QuestCondition> parseConditions(Object raw, String stageId, String fileName){
        List<QuestCondition> list = new ArrayList<>();

        if (raw == null) return list;

        if (raw instanceof List<?>){
            for (Object item : (List<?>) raw){
                if (!(item instanceof String conditionStr)) continue;
                try{
                    QuestCondition condition = QuestCondition.valueOf(conditionStr.toUpperCase());
                    list.add(condition);
                } catch (Exception e) {
                    log.warning("[QuestLoader] Condition invalide dans stage "
                            + stageId + " (" + fileName + ") : " + e.getMessage());
                }
            }
        }
        return list;
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name);
    }
}
