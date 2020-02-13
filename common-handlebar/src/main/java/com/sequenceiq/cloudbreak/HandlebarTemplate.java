package com.sequenceiq.cloudbreak;

public enum HandlebarTemplate {
    DEFAULT_PREFIX("{{{"),
    DEFAULT_POSTFIX("}}}");

    private final String key;

    HandlebarTemplate(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    @Override
    public String toString() {
        return key;
    }
}
