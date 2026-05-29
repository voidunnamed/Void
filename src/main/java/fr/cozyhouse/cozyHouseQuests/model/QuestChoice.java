package fr.cozyhouse.cozyHouseQuests.model;

import fr.cozyhouse.cozyHouseQuests.enums.QuestCondition;

import java.util.List;

public class QuestChoice {

    private String text;
    private String nextStage;
    private List<QuestCondition> conditions;

    public QuestChoice(String text, String nextStage, List<QuestCondition> condition) {
    }
}
