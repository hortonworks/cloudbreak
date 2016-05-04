package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

import java.util.List;
import java.util.stream.Collectors;

public class HostMatcher implements Target<String> {

    private List<String> addresses;

    public HostMatcher(List<String> addresses) {
        this.addresses = addresses;
    }

    @Override
    public String getTarget() {
        return addresses.stream().collect(Collectors.joining(","));
    }

    @Override
    public String getType() {
        return "match";
    }
}
