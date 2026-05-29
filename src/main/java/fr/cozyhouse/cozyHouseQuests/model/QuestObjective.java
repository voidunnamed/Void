package fr.cozyhouse.cozyHouseQuests.model;

import fr.cozyhouse.cozyHouseQuests.enums.ObjectiveType;
import fr.cozyhouse.cozyHouseQuests.enums.QuestCondition;
import fr.cozyhouse.cozyHouseQuests.enums.TargetType;

import java.util.List;

public class QuestObjective {

    private String id;
    private ObjectiveType type;
    private TargetType target;
    private int amount;
    private String label;
    private List<QuestCondition> conditions;

    public QuestObjective(String id, ObjectiveType type, TargetType target, int amount, String label, List<QuestCondition> conditions) {
    }
}
