package com.sequenceiq.periscope.rest.json;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class QueueSetupJson implements Json {

    private String message;
    private List<QueueJson> newSetup;
    private Map<String, String> properties;

    public QueueSetupJson() {
    }

    public QueueSetupJson(String message, List<QueueJson> newSetup, Map<String, String> properties) {
        this.message = message;
        this.newSetup = newSetup;
        this.properties = properties;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public List<QueueJson> getNewSetup() {
        return newSetup;
    }

    public void setNewSetup(List<QueueJson> newSetup) {
        this.newSetup = newSetup;
    }

    public static QueueSetupJson emptyJson() {
        return new QueueSetupJson("", Collections.<QueueJson>emptyList(), Collections.<String, String>emptyMap());
    }

    public QueueSetupJson withMessage(String message) {
        this.message = message;
        return this;
    }

}
