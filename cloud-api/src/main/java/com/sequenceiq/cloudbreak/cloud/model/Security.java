package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class Security {

    private final List<SecurityRule> rules;

    public Security(List<SecurityRule> rules) {
        this.rules = ImmutableList.copyOf(rules);
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
