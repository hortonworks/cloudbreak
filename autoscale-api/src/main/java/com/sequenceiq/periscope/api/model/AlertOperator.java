package com.sequenceiq.periscope.api.model;

public enum AlertOperator {

    LESS_THAN("<"),
    MORE_THAN(">");

    private final String operator;

    AlertOperator(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }
}
