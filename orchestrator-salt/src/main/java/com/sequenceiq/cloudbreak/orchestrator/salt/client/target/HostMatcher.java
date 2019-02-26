package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

import java.util.List;

public class HostMatcher implements Target<String> {

    private final List<String> addresses;

    public HostMatcher(List<String> addresses) {
        this.addresses = addresses;
    }

    @Override
    public String getTarget() {
        return String.join(",", addresses);
    }

    @Override
    public String getType() {
        return "match";
    }
}
