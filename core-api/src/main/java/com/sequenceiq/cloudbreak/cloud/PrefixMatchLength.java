package com.sequenceiq.cloudbreak.cloud;

public enum PrefixMatchLength {
    // First digit must match  e.g. 1.x.x / 1.x.x
    MINOR(1),

    // First two digits must match  e.g. 1.0.x / 1.0.x
    MAINTENANCE(2),

    // First three digits must match  e.g. 1.0.0.x / 1.0.0.x
    PATCH(3);

    private final int matchLength;

    PrefixMatchLength(int matchLength) {
        this.matchLength = matchLength;
    }

    public int getMatchLength() {
        return matchLength;
    }
}
