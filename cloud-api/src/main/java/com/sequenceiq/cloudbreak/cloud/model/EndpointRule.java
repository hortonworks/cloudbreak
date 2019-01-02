package com.sequenceiq.cloudbreak.cloud.model;

public class EndpointRule {

    public static final EndpointRule DENY_RULE = new EndpointRule(EndpointRuleAction.DENY.getText(), NetworkConfig.OPEN_NETWORK.getCidr());

    private final String action;

    private final String remoteSubNet;

    private final String description;

    public EndpointRule(String action, String remoteSubNet) {
        this.action = action;
        this.remoteSubNet = remoteSubNet;
        description = "Added by Cloudbreak";
    }

    public String getAction() {
        return action;
    }

    public String getRemoteSubNet() {
        return remoteSubNet;
    }

    public String getDescription() {
        return description;
    }

}
