package com.sequenceiq.cloudbreak.cloud.gcp.model;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class MachineDefinitionWrapperTest {

    @Test
    public void testMachineDefinitionWrapper() {
        MachineDefinitionWrapper machineDefinitionWrapper = new MachineDefinitionWrapper();
        machineDefinitionWrapper.setId("1");
        machineDefinitionWrapper.setItems(new HashMap<>());
        machineDefinitionWrapper.setSelfLink("link");
        machineDefinitionWrapper.setKind("kind");

        Assert.assertEquals("1", machineDefinitionWrapper.getId());
        Assert.assertEquals(true, machineDefinitionWrapper.getItems().isEmpty());
        Assert.assertEquals("link", machineDefinitionWrapper.getSelfLink());
        Assert.assertEquals("kind", machineDefinitionWrapper.getKind());
    }

}