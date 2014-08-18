package com.sequenceiq.periscope.model;

public enum ComparisonOperator {

    LESS_THAN("<"),
    GREATER_THAN(">"),
    LESS_OR_EQUAL_THAN("<="),
    GREATER_OR_EQUAL_THAN(">="),
    EQUALS("=");

    private final String operator;

    private ComparisonOperator(String operator) {
        this.operator = operator;
    }

    public String operator() {
        return operator;
    }

}
