package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StackWithFingerprintsEvent extends StackEvent {

    private final Set<String> sshFingerprints;

    public StackWithFingerprintsEvent(Long stackId, Collection<String> sshFingerprints) {
        super(stackId);
        this.sshFingerprints = ImmutableSet.copyOf(sshFingerprints);
    }

    public Set<String> getSshFingerprints() {
        return sshFingerprints;
    }
}
