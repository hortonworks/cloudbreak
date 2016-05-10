package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class Compound implements Target<String> {

    private final Collection<String> nodeIPs;

    public Compound(String nodeIP) {
        this(Collections.singletonList(nodeIP));
    }

    public Compound(Collection<String> nodeIPs) {
        this.nodeIPs = nodeIPs;
    }

    @Override
    public String getTarget() {
        return "S@" + nodeIPs.stream().collect(Collectors.joining(" or S@"));
    }

    @Override
    public String getType() {
        return "compound";
    }
}