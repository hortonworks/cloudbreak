package com.sequenceiq.cloudbreak.cloud.gcp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

public class MachineDefinitionWrapperTest {

    @Test
    public void testMachineDefinitionWrapper() {
        MachineDefinitionWrapper machineDefinitionWrapper = new MachineDefinitionWrapper();
        machineDefinitionWrapper.setId("1");
        machineDefinitionWrapper.setItems(new HashMap<>());
        machineDefinitionWrapper.setSelfLink("link");
        machineDefinitionWrapper.setKind("kind");

        assertEquals("1", machineDefinitionWrapper.getId());
        assertEquals(true, machineDefinitionWrapper.getItems().isEmpty());
        assertEquals("link", machineDefinitionWrapper.getSelfLink());
        assertEquals("kind", machineDefinitionWrapper.getKind());
    }

}