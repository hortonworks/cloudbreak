package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class Security {

    private final List<SecurityRule> rules;
    private final String cloudSecurityId;

    public Security(List<SecurityRule> rules, String cloudSecurityId) {
        this.rules = ImmutableList.copyOf(rules);
        this.cloudSecurityId = cloudSecurityId;
    }

    public List<SecurityRule> getRules() {
        return rules;
    }

    public String getCloudSecurityId() {
        return cloudSecurityId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Security{");
        sb.append("rules=").append(rules);
        sb.append('}');
        return sb.toString();
    }
}
