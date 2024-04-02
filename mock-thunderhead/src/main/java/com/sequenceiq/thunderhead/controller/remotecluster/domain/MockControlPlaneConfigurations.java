package com.sequenceiq.thunderhead.controller.remotecluster.domain;

import java.util.ArrayList;
import java.util.List;

public class MockControlPlaneConfigurations {

    private List<MockPvcControlPlaneConfiguration> controlPlaneConfigurations = new ArrayList<>();

    public List<MockPvcControlPlaneConfiguration> getControlPlaneConfigurations() {
        return controlPlaneConfigurations;
    }

    public void setControlPlaneConfigurations(List<MockPvcControlPlaneConfiguration> controlPlaneConfigurations) {
        this.controlPlaneConfigurations = controlPlaneConfigurations;
    }
}
