package fr.cozyhouse.cozyHouseQuests.model;

import lombok.Getter;

import java.util.Map;

@Getter
public class Quest {
    private String id;
    private String name;
    private String description;
    private String startStage;
    private boolean repeatable;
    private int cooldownSeconds;
    private Map<String, QuestStage> stages;

    public Quest(String id, String name, String description, String startStage, boolean repeatable, int cooldownSeconds, Map<String, QuestStage> stages) {
    }

}
