package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.environment.service.LoadBalancerPollerService.LOAD_BALANCER_UPDATE_FAILED_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.exception.UpdateFailedException;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@ExtendWith(MockitoExtension.class)
public class LoadBalancerPollerServiceTest {

    private static final String LOAD_BALANCER_UPDATE_FINISHED_STATE = "LOAD_BALANCER_UPDATE_FINISHED_STATE";

    private static final Long ENV_ID = 1L;

    private static final String ENV_CRN = "envCrn";

    private static final String ENV_NAME = "envName";

    private static final String DL_NAME = "datalakeName";

    private static final String DH_NAME1 = "datahubName1";

    private static final String DH_NAME2 = "datahubName2";

    private final DatahubService datahubService = Mockito.mock(DatahubService.class);

    private final SdxService sdxService = Mockito.mock(SdxService.class);

    private final StackService stackService = Mockito.mock(StackService.class);

    private final FlowEndpoint flowEndpoint = Mockito.mock(FlowEndpoint.class);

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory
            = Mockito.mock(RegionAwareInternalCrnGeneratorFactory.class);

    private final RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator
            = Mockito.mock(RegionAwareInternalCrnGenerator.class);

    private final LoadBalancerPollerService underTest =
        new LoadBalancerPollerService(datahubService, sdxService, stackService, flowEndpoint, regionAwareInternalCrnGeneratorFactory);

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "sleepTime", 1);
        ReflectionTestUtils.setField(underTest, "maxTime", 1);
    }

    @Test
    public void testPollingNoStacks() {
        when(sdxService.list(ENV_NAME)).thenReturn(List.of());
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses());

        underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.DISABLED);

        verify(stackService, never()).updateLoadBalancer(anySet());
    }

    @Test
    public void testPollingForSingleDatalake() {
        setupDatalakeResponse();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses());
        when(stackService.updateLoadBalancer(anySet())).thenReturn(setupFlowIdentifiers(1));
        when(flowEndpoint.hasFlowRunningByFlowId(anyString())).thenReturn(setupFinishedFlowCheckResponse());
        when(flowEndpoint.getFlowLogsByFlowId(anyString())).thenReturn(List.of(setupSuccessFlowLogResponse()));

        underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.ENABLED);

        verify(stackService, times(1)).updateLoadBalancer(eq(Set.of(DL_NAME)));
        verify(flowEndpoint, times(1)).hasFlowRunningByFlowId(anyString());
        verify(flowEndpoint, times(1)).getFlowLogsByFlowId(anyString());
    }

    @Test
    public void testPollingForDatalakeAndDatahubs() {
        setupDatalakeResponse();
        setupDatahubResponse();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(stackService.updateLoadBalancer(eq(Set.of(DL_NAME, DH_NAME1, DH_NAME2)))).thenReturn(setupFlowIdentifiers(3));
        when(flowEndpoint.hasFlowRunningByFlowId(anyString())).thenReturn(setupFinishedFlowCheckResponse());
        when(flowEndpoint.getFlowLogsByFlowId(anyString())).thenReturn(List.of(setupSuccessFlowLogResponse()));

        underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.ENABLED);

        verify(stackService, times(1)).updateLoadBalancer(eq(Set.of(DL_NAME, DH_NAME1, DH_NAME2)));
        verify(flowEndpoint, times(3)).hasFlowRunningByFlowId(anyString());
        verify(flowEndpoint, times(3)).getFlowLogsByFlowId(anyString());
    }

    @Test
    public void testPollingForDatalakeOnly() {
        setupDatalakeResponse();
        setupDatahubResponse();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(stackService.updateLoadBalancer(eq(Set.of(DL_NAME)))).thenReturn(setupFlowIdentifiers(1));
        when(flowEndpoint.hasFlowRunningByFlowId(anyString())).thenReturn(setupFinishedFlowCheckResponse());
        when(flowEndpoint.getFlowLogsByFlowId(anyString())).thenReturn(List.of(setupSuccessFlowLogResponse()));

        underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.DISABLED);

        verify(stackService, times(1)).updateLoadBalancer(eq(Set.of(DL_NAME)));
        verify(flowEndpoint, times(1)).hasFlowRunningByFlowId(anyString());
        verify(flowEndpoint, times(1)).getFlowLogsByFlowId(anyString());
    }

    @Test
    public void testPollingSingleFailure() {
        List<FlowIdentifier> flowIdentifiers = setupFlowIdentifiers(3);
        Iterator<FlowIdentifier> iterator = flowIdentifiers.iterator();
        FlowIdentifier failFlowId = iterator.next();
        String successFlowId1 = iterator.next().getPollableId();
        String successFlowId2 = iterator.next().getPollableId();
        String expectedError = "Data Lake or Data Hub update flows failed: " + List.of(failFlowId);

        setupDatalakeResponse();
        setupDatahubResponse();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(stackService.updateLoadBalancer(eq(Set.of(DL_NAME, DH_NAME1, DH_NAME2)))).thenReturn(flowIdentifiers);
        when(flowEndpoint.hasFlowRunningByFlowId(anyString())).thenReturn(setupFinishedFlowCheckResponse());
        when(flowEndpoint.getFlowLogsByFlowId(failFlowId.getPollableId())).thenReturn(List.of(setupFailFlowLogResponse()));
        when(flowEndpoint.getFlowLogsByFlowId(successFlowId1)).thenReturn(List.of(setupSuccessFlowLogResponse()));
        when(flowEndpoint.getFlowLogsByFlowId(successFlowId2)).thenReturn(List.of(setupSuccessFlowLogResponse()));

        UpdateFailedException exception =
            assertThrows(UpdateFailedException.class, () ->
                underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.ENABLED));

        verify(flowEndpoint, times(3)).hasFlowRunningByFlowId(anyString());
        verify(flowEndpoint, times(3)).getFlowLogsByFlowId(anyString());
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testPollingTimeout() {
        ReflectionTestUtils.setField(underTest, "maxTime", 5);
        setupDatalakeResponse();
        setupDatahubResponse();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(stackService.updateLoadBalancer(eq(Set.of(DL_NAME, DH_NAME1, DH_NAME2)))).thenReturn(setupFlowIdentifiers(3));
        when(flowEndpoint.hasFlowRunningByFlowId(anyString())).thenReturn(setupActiveFlowCheckResponse());
        String expectedError = "Stack update poller reached timeout.";

        UpdateFailedException exception =
            assertThrows(UpdateFailedException.class, () ->
                underTest.updateStackWithLoadBalancer(ENV_ID, ENV_CRN, ENV_NAME, PublicEndpointAccessGateway.ENABLED));

        verify(flowEndpoint, times(18)).hasFlowRunningByFlowId(anyString());
        assertEquals(expectedError, exception.getMessage());
    }

    private void setupDatalakeResponse() {
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setName(DL_NAME);
        when(sdxService.list(ENV_NAME)).thenReturn(List.of(sdxClusterResponse));
    }

    private void setupDatahubResponse() {
        StackViewV4Response response1 = new StackViewV4Response();
        response1.setName(DH_NAME1);
        StackViewV4Response response2 = new StackViewV4Response();
        response2.setName(DH_NAME2);
        when(datahubService.list(ENV_CRN)).thenReturn(new StackViewV4Responses(Set.of(response1, response2)));
    }

    private List<FlowIdentifier> setupFlowIdentifiers(int count) {
        List<FlowIdentifier> flowIdentifiers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            flowIdentifiers.add(new FlowIdentifier(FlowType.FLOW, UUID.randomUUID().toString()));
        }
        return flowIdentifiers;
    }

    private FlowLogResponse setupSuccessFlowLogResponse() {
        FlowLogResponse flowLogResponse = new FlowLogResponse();
        flowLogResponse.setCurrentState(LOAD_BALANCER_UPDATE_FINISHED_STATE);
        return flowLogResponse;
    }

    private FlowLogResponse setupFailFlowLogResponse() {
        FlowLogResponse flowLogResponse = new FlowLogResponse();
        flowLogResponse.setCurrentState(LOAD_BALANCER_UPDATE_FAILED_STATE);
        return flowLogResponse;
    }

    private FlowCheckResponse setupFinishedFlowCheckResponse() {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setHasActiveFlow(false);
        return flowCheckResponse;
    }

    private FlowCheckResponse setupActiveFlowCheckResponse() {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setHasActiveFlow(true);
        return flowCheckResponse;
    }
}
