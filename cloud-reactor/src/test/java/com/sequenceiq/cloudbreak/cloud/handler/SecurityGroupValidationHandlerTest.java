package com.sequenceiq.cloudbreak.cloud.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.validation.SecurityGroupValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.validation.SecurityGroupValidationResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@ExtendWith(MockitoExtension.class)
class SecurityGroupValidationHandlerTest {

    private static final Long RESOURCE_ID = 42L;

    private static final String REGION = "us-east-1";

    private static final String VPC_ID = "vpc-1";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private PlatformResources platformResources;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private SecurityGroupValidationHandler underTest;

    private ExtendedCloudCredential extendedCloudCredential;

    @BeforeEach
    void setUp() {
        extendedCloudCredential = mock(ExtendedCloudCredential.class);
    }

    @Test
    void type() {
        assertEquals(SecurityGroupValidationRequest.class, underTest.type());
    }

    @Test
    void emptyRequestedIdsReturnsEmptyResult() {
        SecurityGroupValidationRequest request = newRequest(Set.of(), VPC_ID);

        underTest.accept(new Event<>(request));

        SecurityGroupValidationResult result = captureResult();
        assertTrue(result.getMissingSecurityGroupIds().isEmpty());
        assertTrue(result.getNotInNetworkSecurityGroupIds().isEmpty());
        verify(cloudPlatformConnectors, times(0)).get(any(CloudPlatformVariant.class));
    }

    @Test
    void allIdsPresentInVpcReturnsEmptySets() throws Exception {
        stubProvider(Set.of(sg("sg-1", VPC_ID), sg("sg-2", VPC_ID)));
        SecurityGroupValidationRequest request = newRequest(Set.of("sg-1", "sg-2"), VPC_ID);

        underTest.accept(new Event<>(request));

        SecurityGroupValidationResult result = captureResult();
        assertTrue(result.getMissingSecurityGroupIds().isEmpty());
        assertTrue(result.getNotInNetworkSecurityGroupIds().isEmpty());
    }

    @Test
    void missingIdsShowUpUnderMissing() throws Exception {
        stubProvider(Set.of(sg("sg-1", VPC_ID)));
        SecurityGroupValidationRequest request = newRequest(Set.of("sg-1", "sg-missing"), VPC_ID);

        underTest.accept(new Event<>(request));

        SecurityGroupValidationResult result = captureResult();
        assertEquals(Set.of("sg-missing"), result.getMissingSecurityGroupIds());
        assertTrue(result.getNotInNetworkSecurityGroupIds().isEmpty());
    }

    @Test
    void wrongVpcIdsShowUpUnderNotInNetwork() throws Exception {
        // The classification MUST NOT collapse wrong-vpc into missing — the handler queries without a vpc filter for
        // exactly this reason. Verifies that decision doesn't regress.
        stubProvider(Set.of(sg("sg-1", VPC_ID), sg("sg-wrong-vpc", "vpc-other")));
        SecurityGroupValidationRequest request = newRequest(Set.of("sg-1", "sg-wrong-vpc"), VPC_ID);

        underTest.accept(new Event<>(request));

        SecurityGroupValidationResult result = captureResult();
        assertTrue(result.getMissingSecurityGroupIds().isEmpty());
        assertEquals(Set.of("sg-wrong-vpc"), result.getNotInNetworkSecurityGroupIds());
    }

    @Test
    void nullNetworkIdSkipsVpcCheck() throws Exception {
        stubProvider(Set.of(sg("sg-1", "vpc-other")));
        SecurityGroupValidationRequest request = newRequest(Set.of("sg-1"), null);

        underTest.accept(new Event<>(request));

        SecurityGroupValidationResult result = captureResult();
        assertTrue(result.getMissingSecurityGroupIds().isEmpty(), "sg-1 exists so should not be reported as missing");
        assertTrue(result.getNotInNetworkSecurityGroupIds().isEmpty(), "null networkId disables the VPC-membership check");
    }

    @Test
    void providerExceptionReturnsFailureResult() throws Exception {
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        when(platformResources.securityGroups(any(), any(), any())).thenThrow(new RuntimeException("boom"));

        SecurityGroupValidationRequest request = newRequest(Set.of("sg-1"), VPC_ID);
        underTest.accept(new Event<>(request));

        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventBus).notify(selectorCaptor.capture(), any(Event.class));
        assertEquals(CloudPlatformResult.failureSelector(SecurityGroupValidationResult.class), selectorCaptor.getValue());
    }

    private void stubProvider(Set<CloudSecurityGroup> groups) throws Exception {
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        Map<String, Set<CloudSecurityGroup>> byRegion = new HashMap<>();
        byRegion.put(REGION, groups);
        when(platformResources.securityGroups(eq(extendedCloudCredential), eq(Region.region(REGION)), any()))
                .thenReturn(new CloudSecurityGroups(byRegion));
    }

    private SecurityGroupValidationRequest newRequest(Set<String> ids, String networkId) {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(RESOURCE_ID)
                .withName("stack")
                .withCrn("crn:cdp:datalake:us-west-1:acct:datalake:x")
                .withPlatform("AWS")
                .withVariant("AWS_NATIVE")
                .withLocation(com.sequenceiq.cloudbreak.cloud.model.Location.location(Region.region(REGION)))
                .withAccountId("acct")
                .withTenantId(1L)
                .build();
        CloudCredential cloudCredential = new CloudCredential("id", "name", "acct");
        SecurityGroupValidationRequest request = new SecurityGroupValidationRequest(
                cloudContext, cloudCredential, extendedCloudCredential, REGION, ids, networkId);
        // The request's internal promise is created lazily by CloudPlatformRequest; force it so onNext() has a subscriber.
        request.getResult();
        return request;
    }

    private SecurityGroupValidationResult captureResult() {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(String.class), eventCaptor.capture());
        Object emitted = eventCaptor.getValue().getData();
        assertThat(emitted).isInstanceOf(SecurityGroupValidationResult.class);
        return (SecurityGroupValidationResult) emitted;
    }

    private CloudSecurityGroup sg(String id, String vpcId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("vpcId", vpcId);
        return new CloudSecurityGroup("name-" + id, id, properties);
    }
}
