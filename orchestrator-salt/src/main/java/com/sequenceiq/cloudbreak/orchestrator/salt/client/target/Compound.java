package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

public class Compound implements Target<String> {

    private final String compound;

    public Compound(String compound) {
        this.compound = compound;
    }


    @Override
    public String getTarget() {
        return compound;
    }

    @Override
    public String getType() {
        return "compound";
    }
}