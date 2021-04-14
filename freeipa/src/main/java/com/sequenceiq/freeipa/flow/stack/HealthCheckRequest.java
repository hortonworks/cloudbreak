package com.sequenceiq.freeipa.flow.stack;

import java.util.List;

public class HealthCheckRequest extends StackEvent {

    private boolean waitForFreeIpaAvailability;

    private List<String> instanceIds;

    public HealthCheckRequest(Long stackId, boolean waitForFreeIpaAvailability) {
        this(stackId, waitForFreeIpaAvailability, null);
    }

    public HealthCheckRequest(Long stackId, boolean waitForFreeIpaAvailability, List<String> instanceIds) {
        super(stackId);
        this.waitForFreeIpaAvailability = waitForFreeIpaAvailability;
        this.instanceIds = instanceIds;
    }

    public boolean getWaitForFreeIpaAvailability() {
        return waitForFreeIpaAvailability;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }
}
