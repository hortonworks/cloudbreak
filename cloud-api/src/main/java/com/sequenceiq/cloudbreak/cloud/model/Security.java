package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

public class Security {

    private List<SecurityRule> rules;

    public Security(List<SecurityRule> rules) {
        this.rules = rules;
    }

    public List<SecurityRule> getRules() {
        return rules;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Security{");
        sb.append("rules=").append(rules);
        sb.append('}');
        return sb.toString();
    }
}
