package com.sequenceiq.cloudbreak.javaversion;

public record SupportedJavaVersion(int jdk, String from, String until) implements RuntimeVersionRange {

    @Override
    public String toString() {
        return "Java " + jdk + " " + rangeDescription();
    }
}
