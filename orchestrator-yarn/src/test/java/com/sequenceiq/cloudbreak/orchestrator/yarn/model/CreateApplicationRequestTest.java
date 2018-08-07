package com.sequenceiq.cloudbreak.orchestrator.yarn.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.YarnComponent;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.CreateApplicationRequest;

public class CreateApplicationRequestTest {

    private static final String NAME = "testApp";

    private static final int LIFETIME = 12;

    @Test
    public void testName() {
        CreateApplicationRequest createApplicationRequest = new CreateApplicationRequest();
        createApplicationRequest.setName(NAME);
        assertEquals(NAME, createApplicationRequest.getName());
    }

    @Test
    public void testLifetime() {
        CreateApplicationRequest createApplicationRequest = new CreateApplicationRequest();
        createApplicationRequest.setLifetime(LIFETIME);
        assertEquals(LIFETIME, createApplicationRequest.getLifetime());
    }

    @Test
    public void testComponents() {
        YarnComponent component = new YarnComponent();
        CreateApplicationRequest createApplicationRequest = new CreateApplicationRequest();
        List<YarnComponent> components = new ArrayList<>();
        components.add(component);
        createApplicationRequest.setComponents(components);
        assertEquals(1L, createApplicationRequest.getComponents().size());
    }
}