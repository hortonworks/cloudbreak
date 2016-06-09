package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class Compound implements Target<String> {

    private final Collection<String> targets;
    private final CompoundType type;

    public Compound(String nodeIP) {
        this(Collections.singletonList(nodeIP), CompoundType.IP);
    }

    public Compound(Collection<String> targets) {
        this(targets, CompoundType.IP);
    }

    public Compound(Collection<String> targets, CompoundType type) {
        this.targets = targets;
        this.type = type;
    }

    @Override
    public String getTarget() {
        return type.delimiter + targets.stream().collect(Collectors.joining(" or " + type.delimiter));
    }

    @Override
    public String getType() {
        return "compound";
    }

    public enum CompoundType {
        IP("S@"),
        HOST("L@"),
        GRAINS("G@"),
        PILLAR("I@");

        private String delimiter;

        CompoundType(String delimiter) {
            this.delimiter = delimiter;
        }

        public String delimiter() {
            return delimiter;
        }
    }
}