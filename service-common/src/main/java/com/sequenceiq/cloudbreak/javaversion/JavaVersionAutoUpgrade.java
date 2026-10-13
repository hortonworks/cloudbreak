package com.sequenceiq.cloudbreak.javaversion;

public record JavaVersionAutoUpgrade(String targetFrom, String targetUntil, int whenJdk, int setJdk) implements RuntimeVersionRange {

    @Override
    public String from() {
        return targetFrom;
    }

    @Override
    public String until() {
        return targetUntil;
    }

    @Override
    public String toString() {
        return "Java " + whenJdk + " -> " + setJdk + " on target runtimes " + rangeDescription();
    }
}
