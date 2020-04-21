package com.sequenceiq.authorization.service;

import java.util.List;
import java.util.Objects;

public class CrnsByCategory {

    private final List<String> defaultResourceCrns;

    private final List<String> notDefaultResourceCrns;

    private CrnsByCategory(List<String> defaultResourceCrns, List<String> notDefaultResourceCrns) {
        this.defaultResourceCrns = Objects.requireNonNull(defaultResourceCrns);
        this.notDefaultResourceCrns = Objects.requireNonNull(notDefaultResourceCrns);
    }

    public List<String> getDefaultResourceCrns() {
        return defaultResourceCrns;
    }

    public List<String> getNotDefaultResourceCrns() {
        return notDefaultResourceCrns;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> defaultResourceCrns = List.of();

        private List<String> notDefaultResourceCrns = List.of();

        public Builder defaultResourceCrns(List<String> defaultResourceCrns) {
            this.defaultResourceCrns = defaultResourceCrns;
            return this;
        }

        public Builder notDefaultResourceCrns(List<String> notDefaultResourceCrns) {
            this.notDefaultResourceCrns = notDefaultResourceCrns;
            return this;
        }

        public CrnsByCategory build() {
            return new CrnsByCategory(defaultResourceCrns, notDefaultResourceCrns);
        }
    }
}
