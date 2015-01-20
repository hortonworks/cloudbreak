package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.List;

public class TemplateAdditionParser {
    public List<TemplateAddition> parseTemplateAdditions(String additionString) {
        List<TemplateAddition> additions = new ArrayList<>();
        String[] additionsArray = additionString.split(";");
        for (String additionsString : additionsArray) {
            String[] additionArray = additionsString.split(",");
            additions.add(new TemplateAddition(additionArray[0], Integer.valueOf(additionArray[1])));
        }
        return additions;
    }

    public List<String[]> parseInstanceGroups(String instanceGroupsString) {
        List<String[]> instanceGroups = new ArrayList<>();
        String[] instanceGroupStrings = instanceGroupsString.split(";");
        for (String instanceGroupString : instanceGroupStrings) {
            instanceGroups.add(instanceGroupString.split(","));
        }
        return instanceGroups;
    }
}
