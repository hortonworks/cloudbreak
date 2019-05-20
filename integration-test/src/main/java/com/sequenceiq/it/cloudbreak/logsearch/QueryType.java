package com.sequenceiq.it.cloudbreak.logsearch;

public class QueryType {

    private String id;

    private String name;

    private String label;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
