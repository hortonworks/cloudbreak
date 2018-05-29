package com.sequenceiq.cloudbreak.service.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.UpdateGatewayTopologiesJson;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.controller.validation.Validator;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

public class UpdateGatewayTopologiesJsonValidatorTest {

    private static final long STACK_ID = 0L;

    @Test
    public void testWithNullRequest() {
        Validator<GatewayTopologyJson> mockValidator = subject -> ValidationResult.builder().build();

        UpdateGatewayTopologiesJsonValidator underTest = new UpdateGatewayTopologiesJsonValidator(mockValidator, STACK_ID, new Stack());
        ValidationResult validationResult = underTest.validate(null);

        assertEquals(State.ERROR, validationResult.getState());
        assertTrue(validationResult.getErrors().get(0).contains("Request is empty."));
    }

    @Test
    public void testWithEmptyRequest() {
        Validator<GatewayTopologyJson> mockValidator = subject -> ValidationResult.builder().build();

        UpdateGatewayTopologiesJsonValidator underTest = new UpdateGatewayTopologiesJsonValidator(mockValidator, STACK_ID, new Stack());
        ValidationResult validationResult = underTest.validate(null);

        assertEquals(State.ERROR, validationResult.getState());
        assertTrue(validationResult.getErrors().get(0).contains("Request is empty."));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithIllegalReuse() {
        Validator<GatewayTopologyJson> mockValidator = subject -> ValidationResult.builder().build();

        UpdateGatewayTopologiesJsonValidator underTest = new UpdateGatewayTopologiesJsonValidator(mockValidator, STACK_ID, null);
        underTest.validate(getRequest());
        underTest.validate(getRequest());
    }

    @Test
    public void testWithNullStack() {
        Validator<GatewayTopologyJson> mockValidator = subject -> ValidationResult.builder().build();

        UpdateGatewayTopologiesJsonValidator underTest = new UpdateGatewayTopologiesJsonValidator(mockValidator, 0, null);
        ValidationResult validationResult = underTest.validate(getRequest());

        assertEquals(State.ERROR, validationResult.getState());
        assertTrue(validationResult.getErrors().get(0).contains("Stack with id '0' does not exist."));
    }

    @Test
    public void testWithNullCluster() {
        Validator<GatewayTopologyJson> mockValidator = subject -> ValidationResult.builder().build();
        Stack stack = new Stack();
        stack.setName("testStack");

        UpdateGatewayTopologiesJsonValidator underTest = new UpdateGatewayTopologiesJsonValidator(mockValidator, STACK_ID, stack);
        ValidationResult validationResult = underTest.validate(getRequest());

        assertEquals(State.ERROR, validationResult.getState());
        assertTrue(validationResult.getErrors().get(0).contains("Stack 'testStack' does not have a cluster."));
    }

    @Test
    public void testWithNullGateway() {
        Validator<GatewayTopologyJson> mockValidator = subject -> ValidationResult.builder().build();
        Stack stack = new Stack();
        stack.setName("testStack");

        Cluster cluster = new Cluster();
        cluster.setName("testCluster");
        stack.setCluster(cluster);

        UpdateGatewayTopologiesJsonValidator underTest = new UpdateGatewayTopologiesJsonValidator(mockValidator, STACK_ID, stack);
        ValidationResult validationResult = underTest.validate(getRequest());

        assertEquals(State.ERROR, validationResult.getState());
        assertTrue(validationResult.getErrors().get(0).contains("Cluster 'testCluster' does not have a gateway."));
    }

    @Test
    public void testNoSuchRequestedTopology() {
        Validator<GatewayTopologyJson> mockValidator = subject -> ValidationResult.builder().build();
        Stack stack = new Stack();
        stack.setName("testStack");

        Cluster cluster = new Cluster();
        cluster.setName("testCluster");
        stack.setCluster(cluster);

        Gateway gateway = new Gateway();
        cluster.setGateway(gateway);


        UpdateGatewayTopologiesJson validationSubject = new UpdateGatewayTopologiesJson();
        GatewayTopologyJson topology = new GatewayTopologyJson();
        topology.setTopologyName("topology1");
        topology.setExposedServices(Collections.singletonList("ALL"));
        validationSubject.setTopologies(Collections.singletonList(topology));

        UpdateGatewayTopologiesJsonValidator underTest = new UpdateGatewayTopologiesJsonValidator(mockValidator, STACK_ID, stack);
        ValidationResult validationResult = underTest.validate(validationSubject);

        assertEquals(State.ERROR, validationResult.getState());
        assertTrue(validationResult.getErrors().get(0).contains("No such topology in stack"));
    }

    @Test
    public void testValidRequest() throws JsonProcessingException {
        Validator<GatewayTopologyJson> mockValidator = subject -> ValidationResult.builder().build();
        Stack stack = new Stack();
        stack.setName("testStack");

        Cluster cluster = new Cluster();
        cluster.setName("testCluster");
        stack.setCluster(cluster);

        Gateway gateway = new Gateway();
        cluster.setGateway(gateway);

        GatewayTopology gatewayTopology1 = new GatewayTopology();
        gatewayTopology1.setTopologyName("topology1");
        ExposedServices exposedServices1 = new ExposedServices();
        exposedServices1.setServices(Collections.singletonList("AMBARI"));
        gatewayTopology1.setExposedServices(new Json(exposedServices1));

        GatewayTopology gatewayTopology2 = new GatewayTopology();
        gatewayTopology2.setTopologyName("topology2");
        ExposedServices exposedServices2 = new ExposedServices();
        exposedServices2.setServices(Collections.singletonList("ZEPPELIN"));
        gatewayTopology2.setExposedServices(new Json(exposedServices2));

        gateway.setTopologies(Sets.newHashSet(gatewayTopology1, gatewayTopology2));


        UpdateGatewayTopologiesJson validationSubject = new UpdateGatewayTopologiesJson();
        GatewayTopologyJson topology = new GatewayTopologyJson();
        topology.setTopologyName("topology1");
        topology.setExposedServices(Collections.singletonList("ALL"));
        validationSubject.setTopologies(Collections.singletonList(topology));

        UpdateGatewayTopologiesJsonValidator underTest = new UpdateGatewayTopologiesJsonValidator(mockValidator, STACK_ID, stack);
        ValidationResult validationResult = underTest.validate(validationSubject);

        assertEquals(State.VALID, validationResult.getState());
    }

    private UpdateGatewayTopologiesJson getRequest() {
        UpdateGatewayTopologiesJson request = new UpdateGatewayTopologiesJson();
        GatewayTopologyJson gatewayTopologyJson = new GatewayTopologyJson();
        gatewayTopologyJson.setTopologyName("topology0");
        gatewayTopologyJson.setExposedServices(Collections.singletonList("ALL"));
        request.setTopologies(Collections.singletonList(gatewayTopologyJson));
        return request;
    }
}