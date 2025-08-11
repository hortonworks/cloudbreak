package com.sequenceiq.cloudbreak.cloud.template.loadbalancer;

import static com.sequenceiq.common.api.type.ResourceType.GCP_HEALTH_CHECK;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class LoadBalancerResourceServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    @InjectMocks
    private LoadBalancerResourceService underTest;

    private CloudStack cloudStack;

    private AuthenticatedContext authenticatedContext;

    private ResourceBuilderContext context;

    @Mock
    private ResourceBuilders resourceBuilders;

    @Mock
    private PersistenceNotifier resourceNotifier;

    @Mock
    private ResourcePollTaskFactory statusCheckFactory;

    @Mock
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    private long privateId;

    private Image image;

    private String privateCrn;

    @BeforeEach
    public void setup() {
        privateId = 0L;
        privateCrn = "crn";
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        Map<InstanceGroupType, String> userData = ImmutableMap.of(InstanceGroupType.CORE, "CORE", InstanceGroupType.GATEWAY, "GATEWAY");
        image = new Image("cb-centos66-amb200-2015-05-25",
                userData, "redhat6", "redhat6", "", "", "default", "default-id", new HashMap<>(), null, null, null);
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(privateId)
                .withName("testname")
                .withCrn("crn")
                .withPlatform("GCP")
                .withWorkspaceId(WORKSPACE_ID)
                .build();
        CloudCredential cloudCredential = new CloudCredential(privateCrn, "credentialname", "account");
        cloudCredential.putParameter("projectId", "projectId");
        authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        context = new ResourceBuilderContext(cloudContext.getName(), location, 30, false);
        List<CloudResource> networkResources = Collections.singletonList(CloudResource.builder()
                .withType(ResourceType.GCP_NETWORK).withName("network-test").build());
        context.addNetworkResources(networkResources);
        Network network = new Network(null);
        cloudStack = CloudStack.builder()
                .groups(Collections.emptyList())
                .network(network)
                .image(image)
                .build();
    }

    @Test
    public void variantHasNoBuilders() throws Exception {
        when(resourceBuilders.loadBalancer(any())).thenReturn(List.of());
        List<CloudResourceStatus> statuses = underTest.buildResources(context, authenticatedContext, cloudStack);
        assertTrue(statuses.isEmpty());
    }

    @Test
    public void variantHasNoDeleters() throws Exception {
        CloudResource instance = CloudResource.builder()
                .withType(GCP_HEALTH_CHECK)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .build();
        when(resourceBuilders.loadBalancer(any())).thenReturn(List.of());
        List<CloudResourceStatus> statuses = underTest.deleteResources(context, authenticatedContext, List.of(instance), false);
        assertTrue(statuses.isEmpty());

    }

    @Test
    public void variantHasNullDeleter() throws Exception {
        CloudResource instance = CloudResource.builder()
                .withType(GCP_HEALTH_CHECK)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .build();
        MockLoadBalancerResourceBuilder mockLoadBalancerResourceBuilder = new MockLoadBalancerResourceBuilder() {
            @Override
            public CloudResource delete(ResourceBuilderContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
                return null;
            }

            @Override
            public ResourceType resourceType() {
                return GCP_HEALTH_CHECK;
            }
        };
        when(resourceBuilders.loadBalancer(any())).thenReturn(List.of(mockLoadBalancerResourceBuilder));
        List<CloudResourceStatus> statuses = underTest.deleteResources(context, authenticatedContext, List.of(instance), false);
        assertTrue(statuses.isEmpty());

    }

    @Test
    public void variantHasNullBuilder() throws Exception {
        MockLoadBalancerResourceBuilder mockLoadBalancerResourceBuilder = new MockLoadBalancerResourceBuilder() {
            @Override
            public List<CloudResource> build(ResourceBuilderContext context, AuthenticatedContext auth,
                    List buildableResources, CloudLoadBalancer loadBalancer, CloudStack cloudStack) throws Exception {
                return null;
            }

            @Override
            public ResourceType resourceType() {
                return GCP_HEALTH_CHECK;
            }
        };
        when(resourceBuilders.loadBalancer(any())).thenReturn(List.of(mockLoadBalancerResourceBuilder));
        List<CloudResourceStatus> statuses = underTest.buildResources(context, authenticatedContext, cloudStack);
        assertTrue(statuses.isEmpty());

    }

    @Test
    public void variantHasErrorDeleter() throws Exception {
        CloudResource instance = CloudResource.builder()
                .withType(GCP_HEALTH_CHECK)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .build();
        MockLoadBalancerResourceBuilder mockLoadBalancerResourceBuilder = new MockLoadBalancerResourceBuilder() {
            @Override
            public CloudResource delete(ResourceBuilderContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
                throw new RuntimeException("bad");
            }

            @Override
            public ResourceType resourceType() {
                return GCP_HEALTH_CHECK;
            }
        };
        when(resourceBuilders.loadBalancer(any())).thenReturn(List.of(mockLoadBalancerResourceBuilder));
        assertThrows(RuntimeException.class, () ->
                underTest.deleteResources(context, authenticatedContext, List.of(instance), false));
    }

    @Test
    public void variantHasErrorBuilder() throws Exception {

        MockLoadBalancerResourceBuilder mockLoadBalancerResourceBuilder = new MockLoadBalancerResourceBuilder() {
            @Override
            public List<CloudResource> build(ResourceBuilderContext context, AuthenticatedContext auth,
                    List buildableResources, CloudLoadBalancer loadBalancer, CloudStack cloudStack) throws Exception {
                throw new RuntimeException("bad");
            }

            @Override
            public ResourceType resourceType() {
                return GCP_HEALTH_CHECK;
            }
        };
        when(resourceBuilders.loadBalancer(any())).thenReturn(List.of(mockLoadBalancerResourceBuilder));
        List<CloudResourceStatus> statuses = underTest.buildResources(context, authenticatedContext, cloudStack);
        assertTrue(statuses.isEmpty());
    }
}
