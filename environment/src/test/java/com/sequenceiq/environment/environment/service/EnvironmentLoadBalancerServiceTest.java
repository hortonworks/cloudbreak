package com.sequenceiq.environment.environment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentLoadBalancerDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.LoadBalancerEntitlementService;

@ExtendWith(MockitoExtension.class)
class EnvironmentLoadBalancerServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ENV_NAME = "environment-name";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:4c5ba74b-c35e-45e9-9f47-123456789876";

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
    void testNoEnvironmentFound() {
        EnvironmentLoadBalancerDto environmentLbDto = EnvironmentLoadBalancerDto.builder().build();
        NetworkDto network = NetworkDto.builder()
                .withEndpointGatewaySubnetMetas(Map.of(
                        "subnet1",
                        new CloudSubnet.Builder()
                                .id("11")
                                .name("subnet1")
                                .build(),
                        "subnet2",
                        new CloudSubnet.Builder()
                                .id("22")
                                .name("subnet2")
                                .build()
                ))
                .build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withName(ENV_NAME)
            .withResourceCrn(ENV_CRN)
            .withNetwork(network)
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
    void testEndpointGatewaySubnetsProvided() {
        EnvironmentLoadBalancerDto environmentLbDto = EnvironmentLoadBalancerDto.builder().build();
        NetworkDto network = NetworkDto.builder()
                .withEndpointGatewaySubnetMetas(Map.of(
                        "subnet1",
                        new CloudSubnet.Builder()
                                .id("11")
                                .name("subnet1")
                                .build(),
                        "subnet2",
                        new CloudSubnet.Builder()
                                .id("22")
                                .name("subnet2")
                                .build()
                ))
                .build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withResourceCrn(ENV_CRN)
            .withNetwork(network)
            .build();

        when(environmentService.findByResourceCrnAndAccountIdAndArchivedIsFalse(anyString(), anyString()))
            .thenReturn(Optional.of(new Environment()));
        doNothing().when(loadBalancerEntitlementService).validateNetworkForEndpointGateway(any(), any(), any());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLbDto);
        });

        verify(reactorFlowManager, times(1))
            .triggerLoadBalancerUpdateFlow(any(), any(), any(), any(), any(), any(), anyString());
    }

    @Test
    void testDataLakeLoadBalancerEnabled() {
        EnvironmentLoadBalancerDto environmentLbDto = EnvironmentLoadBalancerDto.builder().build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withResourceCrn(ENV_CRN)
            .build();

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(environmentService.findByResourceCrnAndAccountIdAndArchivedIsFalse(anyString(), anyString()))
            .thenReturn(Optional.of(new Environment()));
        doNothing().when(loadBalancerEntitlementService).validateNetworkForEndpointGateway(any(), any(), any());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLbDto);
        });

        verify(reactorFlowManager, times(1))
            .triggerLoadBalancerUpdateFlow(any(), any(), any(), any(), any(), any(), anyString());
    }

    @Test
    void testNoEntitlements() {
        EnvironmentLoadBalancerDto environmentLbDto = EnvironmentLoadBalancerDto.builder().build();
        EnvironmentDto environmentDto = EnvironmentDto.builder()
            .withResourceCrn(ENV_CRN)
            .build();
        String expectedError = "Neither Endpoint Gateway nor Data Lake load balancer is enabled. Nothing to do.";

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(false);

        BadRequestException[] exception = new BadRequestException[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            exception[0] = assertThrows(BadRequestException.class, () ->
                underTest.updateLoadBalancerInEnvironmentAndStacks(environmentDto, environmentLbDto));
        });

        assertEquals(expectedError, exception[0].getMessage());
    }
}
