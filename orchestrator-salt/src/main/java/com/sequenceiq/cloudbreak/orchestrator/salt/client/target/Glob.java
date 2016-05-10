package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

public class Glob implements Target<String> {

    public static final Glob ALL = new Glob("*");

    private final String glob;

    public Glob(String glob) {
        this.glob = glob;
    }

    @Override
    public String getTarget() {
        return glob;
    }

    @Override
    public String getType() {
        return "glob";
    }
}