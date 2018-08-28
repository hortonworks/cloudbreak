package com.sequenceiq.cloudbreak.authorization;

public enum SpecialScopes {
    AUTO_SCALE("cloudbreak.autoscale");

    private final String scope;

    SpecialScopes(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }
}
