package com.sequenceiq.environment.environment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.base.LoadBalancerUpdateStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentLoadBalancerUpdateResponse;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentLoadBalancerDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.network.service.LoadBalancerEntitlementService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(SpringExtension.class)
public class EnvironmentLoadBalancerServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ENV_NAME = "environment-name";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:4c5ba74b-c35e-45e9-9f47-123456789876";

    private static final String FLOW_ID = "flowid-1";

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EnvironmentReactorFlowManager reactorFlowManager;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private LoadBalancerEntitlementService loadBalancerEntitlementService;

    @InjectMocks
    private EnvironmentLoadBalancerService underTest;

    @Test
    public void testNoEnvironmentFound() {
        EnvironmentLoadBalancerDto environmentLbDto = EnvironmentLoadBalancerDto.builder()
            .withEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED)
            .build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .build();
        String expectedError = String.format("Could not find environment '%s' using crn '%s'", ENV_NAME, ENV_CRN);

        when(environmentService.findByResourceCrnAndAccountIdAndArchivedIsFalse(anyString(), anyString()))
            .thenReturn(Optional.empty());
        doNothing().when(loadBalancerEntitlementService).validateNetworkForEndpointGateway(any(), any(), any());

        final NotFoundException[] exception = new NotFoundException[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            exception[0] = assertThrows(NotFoundException.class, () ->
                underTest.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLbDto));
        });

        assertEquals(expectedError, exception[0].getMessage());
    }

    @Test
    public void testEndpointGatewayEnabled() {
        EnvironmentLoadBalancerDto environmentLbDto = EnvironmentLoadBalancerDto.builder()
            .withEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED)
            .build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withResourceCrn(ENV_CRN)
            .build();
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        when(environmentService.findByResourceCrnAndAccountIdAndArchivedIsFalse(anyString(), anyString()))
            .thenReturn(Optional.of(new Environment()));
        when(reactorFlowManager.triggerLoadBalancerUpdateFlow(any(), any(), any(), any(), any(), any(), anyString()))
            .thenReturn(Optional.of(flowIdentifier));
        doNothing().when(loadBalancerEntitlementService).validateNetworkForEndpointGateway(any(), any(), any());

        EnvironmentLoadBalancerUpdateResponse[] updateResponse = new EnvironmentLoadBalancerUpdateResponse[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            updateResponse[0] = underTest.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLbDto);
        });

        verify(reactorFlowManager, times(1))
            .triggerLoadBalancerUpdateFlow(any(), any(), any(), any(), any(), any(), anyString());
        assertEquals(flowIdentifier, updateResponse[0].getFlowId());
        assertEquals(PublicEndpointAccessGateway.ENABLED, updateResponse[0].getRequestedPublicEndpointGateway());
        assertEquals(LoadBalancerUpdateStatus.IN_PROGRESS, updateResponse[0].getStatus());
    }

    @Test
    public void testDataLakeLoadBalancerEnabled() {
        EnvironmentLoadBalancerDto environmentLbDto = EnvironmentLoadBalancerDto.builder()
            .withEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED)
            .build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withResourceCrn(ENV_CRN)
            .build();
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(environmentService.findByResourceCrnAndAccountIdAndArchivedIsFalse(anyString(), anyString()))
            .thenReturn(Optional.of(new Environment()));
        when(reactorFlowManager.triggerLoadBalancerUpdateFlow(any(), any(), any(), any(), any(), any(), anyString()))
            .thenReturn(Optional.of(flowIdentifier));
        doNothing().when(loadBalancerEntitlementService).validateNetworkForEndpointGateway(any(), any(), any());

        EnvironmentLoadBalancerUpdateResponse[] updateResponse = new EnvironmentLoadBalancerUpdateResponse[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            updateResponse[0] = underTest.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLbDto);
        });

        verify(reactorFlowManager, times(1))
            .triggerLoadBalancerUpdateFlow(any(), any(), any(), any(), any(), any(), anyString());
        assertEquals(flowIdentifier, updateResponse[0].getFlowId());
        assertEquals(PublicEndpointAccessGateway.DISABLED, updateResponse[0].getRequestedPublicEndpointGateway());
        assertEquals(LoadBalancerUpdateStatus.IN_PROGRESS, updateResponse[0].getStatus());
    }

    @Test
    public void testNoEntitlements() {
        EnvironmentLoadBalancerDto environmentLbDto = EnvironmentLoadBalancerDto.builder()
            .withEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED)
            .build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withResourceCrn(ENV_CRN)
            .build();
        String expectedError = "Neither Endpoint Gateway nor Data Lake load balancer is enabled. Nothing to do.";

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(false);
        doNothing().when(loadBalancerEntitlementService).validateNetworkForEndpointGateway(any(), any(), any());

        final BadRequestException[] exception = new BadRequestException[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            exception[0] = assertThrows(BadRequestException.class, () ->
                underTest.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLbDto));
        });

        assertEquals(expectedError, exception[0].getMessage());
    }

    @Test
    public void testUnableToFindFlow() {
        EnvironmentLoadBalancerDto environmentLbDto = EnvironmentLoadBalancerDto.builder()
            .withEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED)
            .build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withResourceCrn(ENV_CRN)
            .build();
        String expectedError = "Unable to initiate update flow.";

        when(environmentService.findByResourceCrnAndAccountIdAndArchivedIsFalse(anyString(), anyString()))
            .thenReturn(Optional.of(new Environment()));
        when(reactorFlowManager.triggerLoadBalancerUpdateFlow(any(), any(), any(), any(), any(), any(), anyString()))
            .thenReturn(Optional.empty());
        doNothing().when(loadBalancerEntitlementService).validateNetworkForEndpointGateway(any(), any(), any());

        final IllegalStateException[] exception = new IllegalStateException[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            exception[0] = assertThrows(IllegalStateException.class, () ->
                underTest.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLbDto));
        });

        assertEquals(expectedError, exception[0].getMessage());
    }
}
