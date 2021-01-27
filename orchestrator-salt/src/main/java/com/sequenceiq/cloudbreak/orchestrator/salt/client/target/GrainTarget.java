package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

public class GrainTarget implements Target<String> {

    private final String target;

    public GrainTarget(String target) {
        this.target = target;
    }

    @Override
    public String getTarget() {
        return target;
    }

    @Override
    public String getType() {
        return "grain";
    }

    @Override
    public String toString() {
        return "GrainTarget{" +
                "target='" + target + '\'' +
                '}';
    }
}
