package com.sequenceiq.cloudbreak.service.cluster;

public class PackageName {
    private String name;

    private String pattern;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return "PackageName{" +
                "name='" + name + '\'' +
                ", pattern='" + pattern + '\'' +
                '}';
    }
}
