package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

import java.util.Collection;
import java.util.Objects;

public class HostList implements Target<String> {

    private final Collection<String> targets;

    public HostList(Collection<String> targets) {
        this.targets = targets;
    }

    @Override
    public String getTarget() {
        return String.join(",", targets);
    }

    @Override
    public String getType() {
        return "list";
    }

    @Override
    public String toString() {
        return "HostList{" +
                "targets=" + (Objects.equals(targets, null) ? "null" : String.join(", ", targets)) +
                '}';
    }
}
