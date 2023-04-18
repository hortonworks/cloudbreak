package com.sequenceiq.cloudbreak.template.model;

public class SafetyValveProperty {
    private String name;

    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "SafetyValveProperty{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

}
