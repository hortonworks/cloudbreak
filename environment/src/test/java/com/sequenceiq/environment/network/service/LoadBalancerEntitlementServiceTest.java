package com.sequenceiq.environment.network.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;

@ExtendWith(MockitoExtension.class)
public class LoadBalancerEntitlementServiceTest {

    private static final String ENV_NAME = "myEnvironment";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private LoadBalancerEntitlementService underTest;

    @Test
    public void testAwsEndpointGatewayEnabled() throws BadRequestException {
        underTest.validateNetworkForEndpointGateway(CloudConstants.AWS, ENV_NAME, PublicEndpointAccessGateway.ENABLED);
    }

    @Test
    public void testAwsEndpointGatewayDisabled() throws BadRequestException {
        underTest.validateNetworkForEndpointGateway(CloudConstants.AWS, ENV_NAME, PublicEndpointAccessGateway.DISABLED);
    }

    @Test
    public void testAzureEndpointGatewayEnabledWithEntitlement() throws BadRequestException {
        when(entitlementService.azureEndpointGatewayEnabled(any())).thenReturn(true);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.validateNetworkForEndpointGateway(CloudConstants.AZURE, ENV_NAME, PublicEndpointAccessGateway.ENABLED);
        });
    }

    @Test
    public void testAzureEndpointGatewayDisabledWithEntitlement() throws BadRequestException {
        when(entitlementService.azureEndpointGatewayEnabled(any())).thenReturn(true);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            underTest.validateNetworkForEndpointGateway(CloudConstants.AZURE, ENV_NAME, PublicEndpointAccessGateway.DISABLED);
        });
    }

    @Test
    public void testAzureEndpointGatewayEnabledWithNoEntitlement() {
        when(entitlementService.azureEndpointGatewayEnabled(any())).thenReturn(false);
        assertThrows(BadRequestException.class, () ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
                underTest.validateNetworkForEndpointGateway(CloudConstants.AZURE, ENV_NAME, PublicEndpointAccessGateway.ENABLED);
            })
        );
    }

    @Test
    public void testGcpEndpointGatewayEnabledWithEntitlement() {
        when(entitlementService.gcpEndpointGatewayEnabled(any())).thenReturn(true);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.validateNetworkForEndpointGateway(CloudConstants.GCP, ENV_NAME, PublicEndpointAccessGateway.ENABLED));
    }

    @Test
    public void testGcpEndpointGatewayDisabledWithEntitlement() {
        when(entitlementService.gcpEndpointGatewayEnabled(any())).thenReturn(true);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.validateNetworkForEndpointGateway(CloudConstants.GCP, ENV_NAME, PublicEndpointAccessGateway.DISABLED));
    }

    @Test
    public void testGcpEndpointGatewayEnabledWithNoEntitlement() {
        when(entitlementService.gcpEndpointGatewayEnabled(any())).thenReturn(false);
        assertThrows(BadRequestException.class, () ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
                underTest.validateNetworkForEndpointGateway(CloudConstants.GCP, ENV_NAME, PublicEndpointAccessGateway.ENABLED);
            })
        );
    }

    @Test
    public void testGcpEndpointGatewayDisabledWithNoEntitlement() throws BadRequestException {
        when(entitlementService.gcpEndpointGatewayEnabled(any())).thenReturn(true);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
        underTest.validateNetworkForEndpointGateway(CloudConstants.GCP, ENV_NAME, PublicEndpointAccessGateway.DISABLED));
    }

    @Test
    public void testYarnEndpointGatewayEnabled() {
        assertThrows(BadRequestException.class, () ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
                underTest.validateNetworkForEndpointGateway(CloudConstants.YARN, ENV_NAME, PublicEndpointAccessGateway.ENABLED);
            })
        );
    }

    @Test
    public void testYarnEndpointGatewayDisabled() throws BadRequestException {
        underTest.validateNetworkForEndpointGateway(CloudConstants.YARN, ENV_NAME, PublicEndpointAccessGateway.DISABLED);
    }

    @Test
    public void testMockEndpointGatewayEnabled() {
        assertThrows(BadRequestException.class, () ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
                underTest.validateNetworkForEndpointGateway(CloudConstants.MOCK, ENV_NAME, PublicEndpointAccessGateway.ENABLED);
            })
        );
    }

    @Test
    public void testMockEndpointGatewayDisabled() throws BadRequestException {
        underTest.validateNetworkForEndpointGateway(CloudConstants.MOCK, ENV_NAME, PublicEndpointAccessGateway.DISABLED);
    }
}
