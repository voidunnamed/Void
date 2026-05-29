package fr.cozyhouse.cozyHouseQuests.model;

import java.util.List;

public class QuestStage {

    private String id;
    private String dialogue;
    private List<QuestObjective> objectives;
    private List<QuestChoice> choices;
    private String nextStage;
    private List<QuestReward> rewards;

    public QuestStage(String stageId, String dialogue, List<QuestObjective> objectives, List<QuestChoice> choices, String nextStage, List<QuestReward> rewards) {
    }
}
