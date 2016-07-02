package com.sequenceiq.cloudbreak.shell.completion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecurityRules extends AbstractCompletion {
    private List<Map<String, String>> rules = new ArrayList<>();

    public SecurityRules(String rulesString) {
        super(rulesString);
        parseRules(rulesString);
    }

    public List<Map<String, String>> getRules() {
        return rules;
    }

    private void parseRules(String rulesString) {
        String[] ruleStringArray = rulesString.split(";");
        for (String ruleString : ruleStringArray) {
            String[] ruleParams = ruleString.split(":");
            Map<String, String> ruleMap = new HashMap<>();
            ruleMap.put("subnet", ruleParams[0]);
            ruleMap.put("protocol", ruleParams[1]);
            ruleMap.put("ports", ruleParams[2]);
            rules.add(ruleMap);
        }
    }

}
