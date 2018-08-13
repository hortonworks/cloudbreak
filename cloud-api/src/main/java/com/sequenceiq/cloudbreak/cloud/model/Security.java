package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;

public class Security {

    private final List<SecurityRule> rules;

    private final List<String> cloudSecurityIds;

    public Security(@Nonnull Collection<SecurityRule> rules, @Nonnull Collection<String> cloudSecurityIds) {
        this.rules = ImmutableList.copyOf(rules);
        this.cloudSecurityIds = ImmutableList.copyOf(cloudSecurityIds);
    }

    public List<SecurityRule> getRules() {
        return rules;
    }

    public String getCloudSecurityId() {
        return cloudSecurityIds.isEmpty() ? null : cloudSecurityIds.get(0);
    }

    public List<String> getCloudSecurityIds() {
        return cloudSecurityIds;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Security{");
        sb.append("rules=").append(rules);
        sb.append('}');
        return sb.toString();
    }
}
