package com.sequenceiq.cloudbreak.javaversion;

public record DefaultJavaVersion(int jdk, String from) {

    @Override
    public String toString() {
        return "Java " + jdk + " from " + from;
    }
}
