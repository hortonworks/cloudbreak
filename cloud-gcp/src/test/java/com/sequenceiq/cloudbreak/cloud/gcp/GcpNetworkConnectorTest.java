package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.network.GcpNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.network.GcpSubnetResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpSubnetSelectorService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.ResourceChecker;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;

@ExtendWith(MockitoExtension.class)
public class GcpNetworkConnectorTest {

    @Mock
    private GcpCloudSubnetProvider gcpCloudSubnetProvider;

    @Mock
    private GcpNetworkResourceBuilder gcpNetworkResourceBuilder;

    @Mock
    private GcpSubnetResourceBuilder gcpSubnetResourceBuilder;

    @Mock
    private GcpContextBuilder contextBuilders;

    @Mock
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Mock
    private ResourcePollTaskFactory statusCheckFactory;

    @Mock
    private GcpSubnetSelectorService gcpSubnetSelectorService;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @InjectMocks
    private GcpNetworkConnector underTest;

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "minSubnetCountInDifferentAz", 1);
        ReflectionTestUtils.setField(underTest, "maxSubnetCountInDifferentAz", 3);
    }

    @Test
    public void testCreateNetworkWithSubnetsWhenCreationGoesFine() throws Exception {
        NetworkCreationRequest networkCreationRequest = new NetworkCreationRequest.Builder()
                .withNetworkCidr("16.0.0.0/16")
                .withAccountId("account-id")
                .withVariant("GCP")
                .withEnvCrn("env-crn")
                .withEnvName("super-env")
                .withPrivateSubnetEnabled(true)
                .withTags(new HashMap<>())
                .withRegion(Region.region("us-west-1"))
                .withPublicSubnets(new HashSet<>())
                .withPrivateSubnets(new HashSet<>())
                .build();
        GcpContext gcpContext = mock(GcpContext.class);
        PollTask pollTask = mock(PollTask.class);
        CloudResource cloudResource = mock(CloudResource.class);

        when(cloudResource.getName())
                .thenReturn("network");

        when(contextBuilders.contextInit(
                any(CloudContext.class),
                any(AuthenticatedContext.class),
                any(Network.class),
                anyBoolean()))
                .thenReturn(gcpContext);

        when(gcpNetworkResourceBuilder.create(
                any(GcpContext.class),
                any(AuthenticatedContext.class),
                any(Network.class)))
                .thenReturn(cloudResource);

        when(gcpNetworkResourceBuilder.build(
                any(GcpContext.class),
                any(AuthenticatedContext.class),
                any(Network.class),
                any(Security.class),
                any(CloudResource.class)))
                .thenReturn(cloudResource);

        when(statusCheckFactory.newPollResourceTask(
                any(ResourceChecker.class),
                any(AuthenticatedContext.class),
                anyList(),
                any(ResourceBuilderContext.class),
                anyBoolean()))
                .thenReturn(pollTask);

        when(syncPollingScheduler.schedule(
                any(PollTask.class)))
                .thenReturn(new ArrayList());

        when(gcpCloudSubnetProvider.provide(
                any(NetworkCreationRequest.class),
                anyList()))
                .thenReturn(List.of(createdSubnet()));

        when(gcpSubnetResourceBuilder.create(
                any(GcpContext.class), any(AuthenticatedContext.class), any(Network.class)))
                .thenReturn(cloudResource);

        when(gcpSubnetResourceBuilder.build(
                any(GcpContext.class), any(AuthenticatedContext.class), any(Network.class), any(Security.class), any(CloudResource.class)))
                .thenReturn(cloudResource);

        CreatedCloudNetwork networkWithSubnets = underTest.createNetworkWithSubnets(networkCreationRequest);

        assertEquals(1, networkWithSubnets.getSubnets().size());
    }

    @Test
    public void testCreateNetworkWithSubnetsWhenCreationThrowTokenExceptionShouldThrowGcpResourceException() throws Exception {
        NetworkCreationRequest networkCreationRequest = new NetworkCreationRequest.Builder()
                .withNetworkCidr("16.0.0.0/16")
                .withAccountId("account-id")
                .withVariant("GCP")
                .withEnvCrn("env-crn")
                .withEnvName("super-env")
                .withPrivateSubnetEnabled(true)
                .withTags(new HashMap<>())
                .withRegion(Region.region("us-west-1"))
                .withPublicSubnets(new HashSet<>())
                .withPrivateSubnets(new HashSet<>())
                .build();
        GcpContext gcpContext = mock(GcpContext.class);
        PollTask pollTask = mock(PollTask.class);
        CloudResource cloudResource = mock(CloudResource.class);

        when(cloudResource.getName())
                .thenReturn("network");

        when(contextBuilders.contextInit(
                any(CloudContext.class),
                any(AuthenticatedContext.class),
                any(Network.class),
                anyBoolean()))
                .thenReturn(gcpContext);

        TokenResponseException tokenResponseException = mock(TokenResponseException.class);

        when(gcpNetworkResourceBuilder.create(
                any(GcpContext.class),
                any(AuthenticatedContext.class),
                any(Network.class)))
                .thenReturn(cloudResource);

        when(gcpNetworkResourceBuilder.build(
                any(GcpContext.class),
                any(AuthenticatedContext.class),
                any(Network.class),
                any(Security.class),
                any(CloudResource.class)))
                .thenReturn(cloudResource);

        when(statusCheckFactory.newPollResourceTask(
                any(ResourceChecker.class),
                any(AuthenticatedContext.class),
                anyList(),
                any(ResourceBuilderContext.class),
                anyBoolean()))
                .thenReturn(pollTask);

        when(syncPollingScheduler.schedule(
                any(PollTask.class)))
                .thenReturn(new ArrayList());

        when(gcpCloudSubnetProvider.provide(
                any(NetworkCreationRequest.class),
                anyList()))
                .thenThrow(tokenResponseException);

        when(gcpSubnetResourceBuilder.create(
                any(GcpContext.class), any(AuthenticatedContext.class), any(Network.class)))
                .thenReturn(cloudResource);

        when(gcpSubnetResourceBuilder.build(
                any(GcpContext.class), any(AuthenticatedContext.class), any(Network.class), any(Security.class), any(CloudResource.class)))
                .thenReturn(cloudResource);

        when(gcpStackUtil.getMissingServiceAccountKeyError(any(TokenResponseException.class), anyString()))
                .thenThrow(new GcpResourceException("error"));

        when(gcpContext.getProjectId()).thenReturn("id");

        GcpResourceException gcpResourceException = assertThrows(GcpResourceException.class,
                () -> underTest.createNetworkWithSubnets(networkCreationRequest));
        assertEquals("error", gcpResourceException.getMessage());
        verify(gcpStackUtil, times(1))
                .getMissingServiceAccountKeyError(any(TokenResponseException.class), anyString());
    }

    @Test
    public void testCreateNetworkWithSubnetsWhenCreationThrowGoogleJsonResponseExceptionShouldThrowGcpResourceException() throws Exception {
        NetworkCreationRequest networkCreationRequest = new NetworkCreationRequest.Builder()
                .withNetworkCidr("16.0.0.0/16")
                .withAccountId("account-id")
                .withVariant("GCP")
                .withEnvCrn("env-crn")
                .withEnvName("super-env")
                .withPrivateSubnetEnabled(true)
                .withTags(new HashMap<>())
                .withRegion(Region.region("us-west-1"))
                .withPublicSubnets(new HashSet<>())
                .withPrivateSubnets(new HashSet<>())
                .build();
        GcpContext gcpContext = mock(GcpContext.class);
        PollTask pollTask = mock(PollTask.class);
        CloudResource cloudResource = mock(CloudResource.class);

        when(cloudResource.getName())
                .thenReturn("network");

        when(contextBuilders.contextInit(
                any(CloudContext.class),
                any(AuthenticatedContext.class),
                any(Network.class),
                anyBoolean()))
                .thenReturn(gcpContext);

        GoogleJsonResponseException googleJsonResponseException = mock(GoogleJsonResponseException.class);
        GoogleJsonError googleJsonError = mock(GoogleJsonError.class);

        when(googleJsonResponseException.getDetails())
                .thenReturn(googleJsonError);
        when(googleJsonError.getMessage())
                .thenReturn("google-error");

        when(gcpNetworkResourceBuilder.create(
                any(GcpContext.class),
                any(AuthenticatedContext.class),
                any(Network.class)))
                .thenReturn(cloudResource);

        when(gcpNetworkResourceBuilder.build(
                any(GcpContext.class),
                any(AuthenticatedContext.class),
                any(Network.class),
                any(Security.class),
                any(CloudResource.class)))
                .thenReturn(cloudResource);

        when(statusCheckFactory.newPollResourceTask(
                any(ResourceChecker.class),
                any(AuthenticatedContext.class),
                anyList(),
                any(ResourceBuilderContext.class),
                anyBoolean()))
                .thenReturn(pollTask);

        when(syncPollingScheduler.schedule(
                any(PollTask.class)))
                .thenReturn(new ArrayList());

        when(gcpCloudSubnetProvider.provide(
                any(NetworkCreationRequest.class),
                anyList()))
                .thenThrow(googleJsonResponseException);

        when(gcpSubnetResourceBuilder.create(
                any(GcpContext.class), any(AuthenticatedContext.class), any(Network.class)))
                .thenReturn(cloudResource);

        when(gcpSubnetResourceBuilder.build(
                any(GcpContext.class), any(AuthenticatedContext.class), any(Network.class), any(Security.class), any(CloudResource.class)))
                .thenReturn(cloudResource);

        when(gcpContext.getProjectId()).thenReturn("id");

        GcpResourceException gcpResourceException = assertThrows(GcpResourceException.class,
                () -> underTest.createNetworkWithSubnets(networkCreationRequest));
        assertEquals("google-error: [ resourceType: GCP_NETWORK,  resourceName: super-env ]", gcpResourceException.getMessage());
        verify(gcpStackUtil, times(0))
                .getMissingServiceAccountKeyError(any(TokenResponseException.class), anyString());
    }

    @Test
    public void testCreateNetworkWithSubnetsWhenCreationThrowIoExceptionShouldThrowGcpResourceException() throws Exception {
        NetworkCreationRequest networkCreationRequest = new NetworkCreationRequest.Builder()
                .withNetworkCidr("16.0.0.0/16")
                .withAccountId("account-id")
                .withVariant("GCP")
                .withEnvCrn("env-crn")
                .withEnvName("super-env")
                .withPrivateSubnetEnabled(true)
                .withTags(new HashMap<>())
                .withRegion(Region.region("us-west-1"))
                .withPublicSubnets(new HashSet<>())
                .withPrivateSubnets(new HashSet<>())
                .build();
        GcpContext gcpContext = mock(GcpContext.class);
        PollTask pollTask = mock(PollTask.class);
        CloudResource cloudResource = mock(CloudResource.class);

        when(cloudResource.getName())
                .thenReturn("network");

        when(contextBuilders.contextInit(
                any(CloudContext.class),
                any(AuthenticatedContext.class),
                any(Network.class),
                anyBoolean()))
                .thenReturn(gcpContext);

        IOException ioException = mock(IOException.class);

        when(gcpNetworkResourceBuilder.create(
                any(GcpContext.class),
                any(AuthenticatedContext.class),
                any(Network.class)))
                .thenReturn(cloudResource);

        when(gcpNetworkResourceBuilder.build(
                any(GcpContext.class),
                any(AuthenticatedContext.class),
                any(Network.class),
                any(Security.class),
                any(CloudResource.class)))
                .thenReturn(cloudResource);

        when(statusCheckFactory.newPollResourceTask(
                any(ResourceChecker.class),
                any(AuthenticatedContext.class),
                anyList(),
                any(ResourceBuilderContext.class),
                anyBoolean()))
                .thenReturn(pollTask);

        when(syncPollingScheduler.schedule(
                any(PollTask.class)))
                .thenReturn(new ArrayList());

        when(gcpCloudSubnetProvider.provide(
                any(NetworkCreationRequest.class),
                anyList()))
                .thenThrow(ioException);

        when(gcpSubnetResourceBuilder.create(
                any(GcpContext.class), any(AuthenticatedContext.class), any(Network.class)))
                .thenReturn(cloudResource);

        when(gcpSubnetResourceBuilder.build(
                any(GcpContext.class), any(AuthenticatedContext.class), any(Network.class), any(Security.class), any(CloudResource.class)))
                .thenReturn(cloudResource);

        when(gcpContext.getProjectId()).thenReturn("id");

        GcpResourceException gcpResourceException = assertThrows(GcpResourceException.class,
                () -> underTest.createNetworkWithSubnets(networkCreationRequest));
        assertEquals("Network creation failed due to IO exception: [ resourceType: GCP_NETWORK,  resourceName: super-env ]",
                gcpResourceException.getMessage());
        verify(gcpStackUtil, times(0))
                .getMissingServiceAccountKeyError(any(TokenResponseException.class), anyString());
    }

    @Test
    public void testDeleteNetworkWithSubnetsWhenDeletionGoesFine() throws Exception {
        NetworkDeletionRequest networkDeletionRequest = new NetworkDeletionRequest.Builder()
                .withAccountId("account-id")
                .withEnvName("super-env")
                .withRegion("us-west-1")
                .withSubnetIds(Set.of("subnet-1"))
                .build();
        GcpContext gcpContext = mock(GcpContext.class);
        PollTask pollTask = mock(PollTask.class);
        CloudResource cloudResource = mock(CloudResource.class);

        when(contextBuilders.contextInit(
                any(CloudContext.class),
                any(AuthenticatedContext.class),
                any(Network.class),
                anyBoolean()))
                .thenReturn(gcpContext);

        when(statusCheckFactory.newPollResourceTask(
                any(ResourceChecker.class),
                any(AuthenticatedContext.class),
                anyList(),
                any(ResourceBuilderContext.class),
                anyBoolean()))
                .thenReturn(pollTask);

        when(syncPollingScheduler.schedule(
                any(PollTask.class)))
                .thenReturn(new ArrayList());

        when(gcpSubnetResourceBuilder.delete(
                any(GcpContext.class), any(AuthenticatedContext.class), any(CloudResource.class), any(Network.class)))
                .thenReturn(cloudResource);

        underTest.deleteNetworkWithSubnets(networkDeletionRequest);
        verify(gcpSubnetResourceBuilder, times(1)).delete(
                any(GcpContext.class), any(AuthenticatedContext.class), any(CloudResource.class), any(Network.class));
    }

    @Test
    public void testFilterSubnets() {
        when(gcpSubnetSelectorService.select(anyCollection(), any(SubnetSelectionParameters.class)))
                .thenReturn(new SubnetSelectionResult(new ArrayList<>()));

        underTest.filterSubnets(new ArrayList<>(), SubnetSelectionParameters.builder().build());

        verify(gcpSubnetSelectorService, times(1)).select(anyCollection(), any(SubnetSelectionParameters.class));
    }

    @Test
    public void testSubnetCountInDifferentAzMax() {
        int subnetCountInDifferentAzMax = underTest.subnetCountInDifferentAzMax();

        assertEquals(3, subnetCountInDifferentAzMax);
    }

    @Test
    public void testSubnetCountInDifferentAzMin() {
        int subnetCountInDifferentAzMin = underTest.subnetCountInDifferentAzMin();

        assertEquals(1, subnetCountInDifferentAzMin);
    }

    private CreatedSubnet createdSubnet() {
        CreatedSubnet createdSubnet = new CreatedSubnet();
        createdSubnet.setCidr("0.0.0.0/0");
        createdSubnet.setSubnetId("subnet-1");
        createdSubnet.setPublicSubnet(true);
        createdSubnet.setIgwAvailable(true);
        createdSubnet.setMapPublicIpOnLaunch(true);
        createdSubnet.setType(SubnetType.DWX);
        return createdSubnet;
    }
}