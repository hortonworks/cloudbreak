package com.sequenceiq.cloudbreak.service.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.convert.ConversionService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.UpdateGatewayTopologiesJson;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceListValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayTopologyV4RequestValidator;
import com.sequenceiq.cloudbreak.converter.stack.cluster.gateway.GatewayTopologyJsonToExposedServicesConverter;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.repository.GatewayRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class GatewayServiceTest {

    private static final long STACK_ID = 0L;

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private StackService stackService;

    @Mock
    private GatewayRepository gatewayRepository;

    @Mock
    private ConversionService conversionService;

    @Spy
    private final TransactionService transactionService = new TransactionService();

    @Spy
    private final GatewayTopologyV4RequestValidator gatewayTopologyJsonValidator = new GatewayTopologyV4RequestValidator(new ExposedServiceListValidator());

    @Mock
    private ReactorFlowManager reactorFlowManager;

    @InjectMocks
    private final GatewayService underTest = new GatewayService();

    @Spy
    private final ExposedServiceListValidator exposedServiceListValidator = new ExposedServiceListValidator();

    @InjectMocks
    private final GatewayTopologyJsonToExposedServicesConverter exposedServicesConverter = new GatewayTopologyJsonToExposedServicesConverter();

    @Before
    public void setup() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
    }

    @Test
    public void testWithEmtyRequest() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Request is empty.");

        underTest.updateGatewayTopologies(STACK_ID, new UpdateGatewayTopologiesJson());
    }

    @Test
    public void testWithNonExistingStack() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Stack with id '0' does not exist.");
        when(stackService.getById(anyLong())).thenReturn(null);

        underTest.updateGatewayTopologies(STACK_ID, getRequest());
    }

    @Test
    public void testWithFlowRunning() throws IOException {
        doThrow(FlowsAlreadyRunningException.class).when(reactorFlowManager).triggerEphemeralUpdate(anyLong());
        when(stackService.getById(anyLong())).thenReturn(getStack());
        when(conversionService.convert(any(GatewayTopologyJson.class), eq(ExposedServices.class)))
                .thenAnswer((Answer<ExposedServices>) invocation -> {
                    GatewayTopologyJson topologyJson = invocation.getArgument(0);
                    return exposedServicesConverter.convert(topologyJson);
                });

        List<Gateway> gatewayRepositoryInvocations = new ArrayList<>();
        when(gatewayRepository.save(any(Gateway.class))).thenAnswer((Answer<Gateway>) invocation -> {
            Gateway gatewayCopy = ((Gateway) invocation.getArgument(0)).copy();
            gatewayRepositoryInvocations.add(gatewayCopy);
            return gatewayCopy;
        });

        try {
            underTest.updateGatewayTopologies(STACK_ID, getRequest());
        } catch (FlowsAlreadyRunningException e) {
            assertEquals(2L, gatewayRepositoryInvocations.size());
            assertInitialSave(gatewayRepositoryInvocations);
            assertRevertSave(gatewayRepositoryInvocations);
            return;
        }
        fail();
    }

    private void assertInitialSave(List<Gateway> gatewayRepositoryInvocations) throws IOException {
        Gateway initialSave = gatewayRepositoryInvocations.get(0);
        GatewayTopology initialTop1 = getTopologyByName("top1", initialSave);
        ExposedServices initialServicesTop1 = initialTop1.getExposedServices().get(ExposedServices.class);
        assertTrue(initialServicesTop1.getServices().containsAll(ExposedService.getAllKnoxExposed()));

        GatewayTopology initialTop2 = getTopologyByName("top2", initialSave);
        ExposedServices initialServicesTop2 = initialTop2.getExposedServices().get(ExposedServices.class);
        assertTrue(initialServicesTop2.getServices().containsAll(Arrays.asList("WEBHDFS", "HDFSUI")));
        assertEquals(2L, initialServicesTop2.getServices().size());
    }

    private void assertRevertSave(List<Gateway> gatewayRepositoryInvocations) throws IOException {
        Gateway revertSave = gatewayRepositoryInvocations.get(1);
        GatewayTopology revertTop1 = getTopologyByName("top1", revertSave);
        ExposedServices revertServicesTop1 = revertTop1.getExposedServices().get(ExposedServices.class);
        assertTrue(revertServicesTop1.getServices().containsAll(Arrays.asList("AMBARI", "ZEPPELIN")));
        assertEquals(2L, revertServicesTop1.getServices().size());

        GatewayTopology revertTop2 = getTopologyByName("top2", revertSave);
        ExposedServices revertServicesTop2 = revertTop2.getExposedServices().get(ExposedServices.class);
        assertTrue(revertServicesTop2.getServices().containsAll(Arrays.asList("WEBHDFS", "HDFSUI")));
        assertEquals(2L, revertServicesTop2.getServices().size());
    }

    private GatewayTopology getTopologyByName(String name, Gateway initialSave) {
        return initialSave.getTopologies().stream().filter(t -> name.equals(t.getTopologyName())).findFirst().get();
    }

    private UpdateGatewayTopologiesJson getRequest() {
        UpdateGatewayTopologiesJson request = new UpdateGatewayTopologiesJson();
        GatewayTopologyJson gatewayTopologyJson = new GatewayTopologyJson();
        gatewayTopologyJson.setTopologyName("top1");
        gatewayTopologyJson.setExposedServices(Collections.singletonList("ALL"));
        request.setTopologies(Collections.singletonList(gatewayTopologyJson));
        return request;
    }

    private Stack getStack() throws JsonProcessingException {
        GatewayTopology topology1 = getTopology("top1", "AMBARI", "ZEPPELIN");
        GatewayTopology topology2 = getTopology("top2", "WEBHDFS", "HDFSUI");

        Gateway gateway = new Gateway();
        gateway.setTopologies(Sets.newHashSet(topology1, topology2));

        Cluster cluster = new Cluster();
        cluster.setName("cluster0");
        cluster.setGateway(gateway);

        Stack stack = new Stack();
        stack.setName("stack0");
        stack.setId(STACK_ID);
        stack.setCluster(cluster);

        return stack;
    }

    private GatewayTopology getTopology(String name, String... services) throws JsonProcessingException {
        GatewayTopology topology = new GatewayTopology();
        topology.setTopologyName(name);
        ExposedServices exposedServices = new ExposedServices();
        exposedServices.setServices(Arrays.asList(services));
        topology.setExposedServices(new Json(exposedServices));
        return topology;
    }
}