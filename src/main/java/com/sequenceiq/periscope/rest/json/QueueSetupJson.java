package com.sequenceiq.periscope.rest.json;

import java.util.List;
import java.util.Map;

public class QueueSetupJson implements Json {

    private List<QueueJson> setup;
    private Map<String, String> properties;

    public QueueSetupJson() {
    }

    public QueueSetupJson(String message, List<QueueJson> setup, Map<String, String> properties) {
        this.setup = setup;
        this.properties = properties;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public List<QueueJson> getSetup() {
        return setup;
    }

    public void setSetup(List<QueueJson> setup) {
        this.setup = setup;
    }

}
