package com.sequenceiq.cloudbreak.service.network;

public class EndpointRule {

    public static final EndpointRule DENY_RULE = new EndpointRule(Action.DENY.getText(), NetworkConfig.OPEN_NETWORK);

    private final String action;
    private final String remoteSubNet;
    private final String description;

    public EndpointRule(String action, String remoteSubNet) {
        this.action = action;
        this.remoteSubNet = remoteSubNet;
        this.description = "Added by Cloudbreak";
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

    public enum Action {
        PERMIT("permit"),
        DENY("deny");

        private final String text;

        private Action(String value) {
            this.text = value;
        }

        public String getText() {
            return text;
        }
    }
}
