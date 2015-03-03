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

    public List<String[]> parseCommaSeparatedRows(String source) {
        List<String[]> result = new ArrayList<>();
        String[] rows = source.split(";");
        for (String row : rows) {
            result.add(row.split(","));
        }
        return result;
    }
}
