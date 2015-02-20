package com.sequenceiq.cloudbreak.service.cluster.flow;

public enum ConsulPluginEvent {

    PRE_INSTALL("recipe-pre-install"),
    POST_INSTALL("recipe-post-install"),
    START_AMBARI_EVENT("ambari-start"),
    STOP_AMBARI_EVENT("ambari-stop"),
    RESTART_AMBARI_EVENT("ambari-restart");

    private final String name;

    private ConsulPluginEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
