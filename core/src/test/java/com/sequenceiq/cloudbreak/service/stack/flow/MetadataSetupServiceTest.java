package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY_PRIMARY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ZOMBIE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceLifeCycle;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigConverter;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.TargetGroupPersistenceService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;

@ExtendWith(MockitoExtension.class)
public class MetadataSetupServiceTest {

    private static final Long STACK_ID = 1L;

    private static final Long OLD_STACK_ID = 4L;

    private static final String STACK_NAME = "STACK_NAME";

    private static final String STACK_DISPLAY_NAME = "STACK_DISPLAY_NAME";

    private static final String OLD_STACK_NAME = "OLD_STACK_NAME";

    private static final String STACK_CRN = "STACK_CRN";

    private static final String GROUP_NAME = "GROUP_NAME";

    private static final Long PRIVATE_ID = 2L;

    private static final String SUBNET_ID = "SUBNET_ID";

    private static final String INSTANCE_NAME = "INSTANCE_NAME";

    private static final String AVAILABILITY_ZONE = "AVAILABILITY_ZONE";

    private static final String RACK_ID = "/RACK_ID";

    private static final String PRIVATE_IP = "PRIVATE_IP";

    private static final String PUBLIC_IP = "PUBLIC_IP";

    private static final Integer SSH_PORT = 22;

    private static final String LOCALITY_INDICATOR = "LOCALITY_INDICATOR";

    private static final Long INSTANCE_GROUP_ID = 3L;

    private static final Long CURRENT_TIME = System.currentTimeMillis();

    private static final com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus CREATED =
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.CREATED;

    private static final com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus TERMINATED =
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.TERMINATED;

    private static final com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus SERVICES_RUNNING =
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;

    private static final String CLOUD_DNS = "CLOUD_DNS";

    private static final String HOSTED_ZONE = "HOSTED_ZONE";

    private static final String LB_NAME = "LB_NAME";

    @Mock
    private ImageService imageService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private LoadBalancerConfigService loadBalancerConfigService;

    @Mock
    private TargetGroupPersistenceService targetGroupPersistenceService;

    @Mock
    private LoadBalancerConfigConverter loadBalancerConfigConverter;

    @Mock
    private StackService stackService;

    @Mock
    private Clock clock;

    @InjectMocks
    private MetadataSetupService underTest;

    @Captor
    private ArgumentCaptor<InstanceMetaData> instanceMetaDataCaptor;

    @Captor
    private ArgumentCaptor<LoadBalancer> loadBalancerCaptor;

    @Test
    public void saveInstanceMetaDataTestShouldNotSaveInstancesWhenImageNotFound() throws CloudbreakImageNotFoundException {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = getCloudVmMetaDataStatuses(InstanceStatus.CREATED);
        CloudbreakImageNotFoundException exception = new CloudbreakImageNotFoundException("Image does not exist");
        doThrow(exception).when(imageService).getImage(STACK_ID);

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, CREATED));

        assertThat(cloudbreakServiceException).hasMessage("Instance metadata collection failed");
        assertThat(cloudbreakServiceException.getCause()).isSameAs(exception);
    }

    @Test
    public void saveInstanceMetaDataTestOneNewInstance()
            throws CloudbreakImageNotFoundException {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Image image = getEmptyImage();
        when(imageService.getImage(STACK_ID)).thenReturn(image);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(INSTANCE_GROUP_ID);
        instanceGroup.setGroupName(GROUP_NAME);
        Set<InstanceGroup> instanceGroupSet = new TreeSet<>();
        instanceGroupSet.add(instanceGroup);
        when(instanceGroupService.findByStackId(STACK_ID)).thenReturn(instanceGroupSet);
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME);
        Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = getCloudVmMetaDataStatuses(InstanceStatus.CREATED);

        InstanceMetaData pgwInstanceMetadata = new InstanceMetaData();
        pgwInstanceMetadata.setInstanceMetadataType(GATEWAY_PRIMARY);
        when(instanceMetaDataService.findNotTerminatedForStack(1L)).thenReturn(Set.of(pgwInstanceMetadata));

        int newInstances = underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, CREATED);

        assertEquals(1, newInstances);
        verify(imageService).getImage(STACK_ID);
        verify(instanceMetaDataService).save(instanceMetaDataCaptor.capture());
        InstanceMetaData instanceMetaData = instanceMetaDataCaptor.getValue();
        assertThat(instanceMetaData.getInstanceGroup()).isSameAs(instanceGroup);
        assertCommonProperties(instanceMetaData);
        assertEquals(CREATED, instanceMetaData.getInstanceStatus());
        assertNotNull(instanceMetaData.getImage());
    }

    @Test
    public void saveLoadBalancerMetadata() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
        stack.setCloudPlatform("DEFAULT");
        stack.setEnvironmentCrn(STACK_CRN);

        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setStack(stack);
        loadBalancer.setType(LoadBalancerType.PUBLIC);

        Set<LoadBalancer> loadBalancerSet = new HashSet<>();
        loadBalancerSet.add(loadBalancer);
        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(loadBalancerSet);
        when(loadBalancerConfigService.generateLoadBalancerEndpoint(stack)).thenCallRealMethod();

        TestStackStatusView stackStatusView = new TestStackStatusView();
        stackStatusView.setId(STACK_ID);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stackStatusView.setStatus(stackStatus);

        when(stackService.getByEnvironmentCrnAndStackType(STACK_CRN, StackType.DATALAKE)).thenReturn(List.of(stackStatusView));

        when(targetGroupPersistenceService.findByLoadBalancerId(any())).thenReturn(Set.of());
        Iterable<CloudLoadBalancerMetadata> cloudLoadBalancerMetaDataStatuses = getCloudLoadBalancerMetaDataStatuses();

        underTest.saveLoadBalancerMetadata(stack, cloudLoadBalancerMetaDataStatuses);

        verify(loadBalancerPersistenceService).save(loadBalancerCaptor.capture());
        LoadBalancer loadBalancerValue = loadBalancerCaptor.getValue();
        assertThat(loadBalancerValue.getHostedZoneId()).isSameAs(HOSTED_ZONE);
        assertThat(loadBalancerValue.getIp()).isSameAs(PUBLIC_IP);
        assertThat(loadBalancerValue.getDns()).isSameAs(CLOUD_DNS);
        assertThat(loadBalancerValue.getEndpoint()).isEqualTo("STACK_NAME-gateway");
    }

    @Test
    public void saveLoadBalancerMetadataAndSetEndpointToOldStack() {
        Stack oldStack = new Stack();
        oldStack.setId(OLD_STACK_ID);
        oldStack.setName(OLD_STACK_NAME);
        oldStack.setCloudPlatform("DEFAULT");
        oldStack.setEnvironmentCrn(STACK_CRN);
        oldStack.setDisplayName(STACK_DISPLAY_NAME);

        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(INSTANCE_GROUP_ID);
        instanceGroup.setGroupName(GROUP_NAME);
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData pgwInstanceMetadata = new InstanceMetaData();
        pgwInstanceMetadata.setInstanceStatus(STOPPED);
        pgwInstanceMetadata.setDiscoveryFQDN("master0.subdomain.cldr.work");
        pgwInstanceMetadata.setInstanceMetadataType(GATEWAY_PRIMARY);
        instanceGroup.setInstanceMetaData(Set.of(pgwInstanceMetadata));
        oldStack.setInstanceGroups(Set.of(instanceGroup));

        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
        stack.setCloudPlatform("DEFAULT");
        stack.setEnvironmentCrn(STACK_CRN);
        stack.setDisplayName(STACK_DISPLAY_NAME);

        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setStack(stack);
        loadBalancer.setType(LoadBalancerType.PUBLIC);

        Set<LoadBalancer> loadBalancerSet = new HashSet<>();
        loadBalancerSet.add(loadBalancer);
        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(loadBalancerSet);
        when(loadBalancerPersistenceService.findByStackId(OLD_STACK_ID)).thenReturn(new HashSet<>());
        when(loadBalancerConfigService.generateLoadBalancerEndpoint(stack)).thenCallRealMethod();

        TestStackStatusView stackStatusView = new TestStackStatusView();
        stackStatusView.setId(STACK_ID);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stackStatusView.setStatus(stackStatus);
        TestStackStatusView stackStatusViewOld = new TestStackStatusView();

        stackStatusViewOld.setId(OLD_STACK_ID);
        StackStatus stoppedStackStatus = new StackStatus();
        stoppedStackStatus.setStatus(Status.STOPPED);
        stackStatusViewOld.setStatus(stoppedStackStatus);

        when(stackService.getByEnvironmentCrnAndStackType(STACK_CRN, StackType.DATALAKE)).thenReturn(List.of(stackStatusView, stackStatusViewOld));
        when(stackService.getByIdWithGatewayInTransaction(OLD_STACK_ID)).thenReturn(oldStack);

        when(targetGroupPersistenceService.findByLoadBalancerId(any())).thenReturn(Set.of());
        Iterable<CloudLoadBalancerMetadata> cloudLoadBalancerMetaDataStatuses = getCloudLoadBalancerMetaDataStatuses();

        underTest.saveLoadBalancerMetadata(stack, cloudLoadBalancerMetaDataStatuses);

        verify(loadBalancerPersistenceService).save(loadBalancerCaptor.capture());
        LoadBalancer loadBalancerValue = loadBalancerCaptor.getValue();
        assertThat(loadBalancerValue.getHostedZoneId()).isSameAs(HOSTED_ZONE);
        assertThat(loadBalancerValue.getIp()).isSameAs(PUBLIC_IP);
        assertThat(loadBalancerValue.getDns()).isSameAs(CLOUD_DNS);
        assertThat(loadBalancerValue.getEndpoint()).isEqualTo("master0");

    }

    @Test
    public void saveInstanceMetaDataTestOneTerminatedInstance()
            throws CloudbreakImageNotFoundException {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Image image = getEmptyImage();
        when(imageService.getImage(STACK_ID)).thenReturn(image);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(INSTANCE_GROUP_ID);
        instanceGroup.setGroupName(GROUP_NAME);
        Set<InstanceGroup> instanceGroupSet = new TreeSet<>();
        instanceGroupSet.add(instanceGroup);
        when(instanceGroupService.findByStackId(STACK_ID)).thenReturn(instanceGroupSet);
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME);
        Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = getCloudVmMetaDataStatuses(InstanceStatus.TERMINATED);

        InstanceMetaData pgwInstanceMetadata = new InstanceMetaData();
        pgwInstanceMetadata.setInstanceMetadataType(GATEWAY_PRIMARY);
        when(instanceMetaDataService.findNotTerminatedForStack(1L)).thenReturn(Set.of(pgwInstanceMetadata));

        int newInstances = underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, SERVICES_RUNNING);

        assertEquals(0, newInstances);
        verify(imageService).getImage(STACK_ID);
        verify(instanceMetaDataService).save(instanceMetaDataCaptor.capture());
        InstanceMetaData instanceMetaData = instanceMetaDataCaptor.getValue();
        assertThat(instanceMetaData.getInstanceGroup()).isSameAs(instanceGroup);
        assertCommonProperties(instanceMetaData);
        assertEquals(TERMINATED, instanceMetaData.getInstanceStatus());
        assertNull(instanceMetaData.getImage());
        assertFalse(instanceMetaData.getClusterManagerServer());
    }

    @Test
    public void saveInstanceMetaDataTestOneZombieInstance()
            throws CloudbreakImageNotFoundException {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Image image = getEmptyImage();
        when(imageService.getImage(STACK_ID)).thenReturn(image);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(INSTANCE_GROUP_ID);
        instanceGroup.setGroupName(GROUP_NAME);
        Set<InstanceGroup> instanceGroupSet = new TreeSet<>();
        instanceGroupSet.add(instanceGroup);
        when(instanceGroupService.findByStackId(STACK_ID)).thenReturn(instanceGroupSet);
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME);
        Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = getCloudVmMetaDataStatuses(InstanceStatus.CREATED);

        InstanceMetaData pgwInstanceMetadata = new InstanceMetaData();
        pgwInstanceMetadata.setInstanceMetadataType(GATEWAY_PRIMARY);
        InstanceMetaData zombieInstanceMetadata = new InstanceMetaData();
        zombieInstanceMetadata.setInstanceStatus(ZOMBIE);
        zombieInstanceMetadata.setPrivateId(PRIVATE_ID);
        when(instanceMetaDataService.findNotTerminatedForStack(1L)).thenReturn(Set.of(zombieInstanceMetadata, pgwInstanceMetadata));

        int newInstances = underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, SERVICES_RUNNING);

        assertEquals(1, newInstances);
        verify(imageService).getImage(STACK_ID);
        verify(instanceMetaDataService).save(instanceMetaDataCaptor.capture());
        InstanceMetaData instanceMetaData = instanceMetaDataCaptor.getValue();
        assertThat(instanceMetaData.getInstanceGroup()).isSameAs(instanceGroup);
        assertCommonProperties(instanceMetaData);
        assertEquals(ZOMBIE, instanceMetaData.getInstanceStatus());
        assertNull(instanceMetaData.getImage());
        assertFalse(instanceMetaData.getClusterManagerServer());
    }

    static Object[][] saveInstanceMetaDataTestServerFlagIsAlreadySetDataProvider() {
        return new Object[][]{
                // testCaseName subnetId availabilityZone rackId
                {"subnetId=null, availabilityZone=null, rackId=null", null, null, null},
                {"subnetId=\"\", availabilityZone=\"\", rackId=\"\"", "", "", ""},
                {"subnetId=SUBNET_ID, availabilityZone=AVAILABILITY_ZONE, rackId=RACK_ID", SUBNET_ID, AVAILABILITY_ZONE, RACK_ID},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("saveInstanceMetaDataTestServerFlagIsAlreadySetDataProvider")
    public void saveInstanceMetaDataTestServerFlagIsAlreadySet(String testCaseName, String subnetId, String availabilityZone, String rackId)
            throws CloudbreakImageNotFoundException {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Image image = getEmptyImage();
        when(imageService.getImage(STACK_ID)).thenReturn(image);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(INSTANCE_GROUP_ID);
        instanceGroup.setGroupName(GROUP_NAME);
        Set<InstanceGroup> instanceGroupSet = new TreeSet<>();
        instanceGroupSet.add(instanceGroup);
        when(instanceGroupService.findByStackId(STACK_ID)).thenReturn(instanceGroupSet);
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME);
        Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = getCloudVmMetaDataStatuses(InstanceStatus.STOPPED);
        InstanceMetaData originalInstanceMetadata = new InstanceMetaData();
        originalInstanceMetadata.setServer(true);
        originalInstanceMetadata.setInstanceId("instanceId");
        originalInstanceMetadata.setPrivateId(PRIVATE_ID);
        originalInstanceMetadata.setInstanceGroup(instanceGroup);
        originalInstanceMetadata.setInstanceMetadataType(GATEWAY_PRIMARY);
        originalInstanceMetadata.setSubnetId(subnetId);
        originalInstanceMetadata.setAvailabilityZone(availabilityZone);
        originalInstanceMetadata.setRackId(rackId);
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(Set.of(originalInstanceMetadata));

        int newInstances = underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, SERVICES_RUNNING);

        assertEquals(0, newInstances);
        verify(imageService).getImage(STACK_ID);
        verify(instanceMetaDataService).save(instanceMetaDataCaptor.capture());
        InstanceMetaData instanceMetaData = instanceMetaDataCaptor.getValue();
        assertThat(instanceMetaData.getInstanceGroup()).isSameAs(instanceGroup);
        assertTrue(instanceMetaData.getAmbariServer());
        assertTrue(instanceMetaData.getClusterManagerServer());
        assertThat(instanceMetaData.getSubnetId()).isEqualTo(subnetId);
        assertThat(instanceMetaData.getAvailabilityZone()).isEqualTo(availabilityZone);
        assertThat(instanceMetaData.getRackId()).isEqualTo(rackId);
    }

    @Test
    public void testSaveInstanceMetadataAndSelectTheRightPGW() {
        Stack stack = new Stack();
        stack.setId(1L);
        List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = new ArrayList<>();
        cloudVmMetaDataStatuses.add(new CloudVmMetaDataStatus(new CloudVmInstanceStatus(new CloudInstance("id1", new InstanceTemplate("medium", "gateway",
                10L, Collections.emptyList(), InstanceStatus.CREATED, Map.of(), 40L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L), null, "subnet", "az"),
                InstanceStatus.CREATED),
                new CloudInstanceMetaData("1.1.1.1", "1.1.1.1")));
        cloudVmMetaDataStatuses.add(new CloudVmMetaDataStatus(new CloudVmInstanceStatus(new CloudInstance("id2", new InstanceTemplate("medium", "gateway",
                11L, Collections.emptyList(), InstanceStatus.CREATED, Map.of(), 40L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L), null, "subnet", "az"),
                InstanceStatus.CREATED),
                new CloudInstanceMetaData("1.1.1.2", "1.1.1.2")));
        cloudVmMetaDataStatuses.add(new CloudVmMetaDataStatus(new CloudVmInstanceStatus(new CloudInstance("id3", new InstanceTemplate("medium", "worker",
                12L, Collections.emptyList(), InstanceStatus.CREATED, Map.of(), 41L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L), null, "subnet", "az"),
                InstanceStatus.CREATED),
                new CloudInstanceMetaData("1.1.1.2", "1.1.1.2")));
        InstanceMetaData lastTerminatedPGW = new InstanceMetaData();
        String primaryGWDiscoveryFQDN = "primarygw.example.com";
        lastTerminatedPGW.setDiscoveryFQDN(primaryGWDiscoveryFQDN);
        when(instanceMetaDataService.getLastTerminatedPrimaryGatewayInstanceMetadata(1L)).thenReturn(Optional.of(lastTerminatedPGW));

        InstanceMetaData gwInstanceMetadata1 = new InstanceMetaData();
        InstanceGroup gwInstanceGroup = new InstanceGroup();
        gwInstanceGroup.setGroupName("gateway");
        gwInstanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        gwInstanceMetadata1.setInstanceGroup(gwInstanceGroup);
        gwInstanceMetadata1.setPrivateId(10L);
        gwInstanceMetadata1.setDiscoveryFQDN(primaryGWDiscoveryFQDN);

        InstanceMetaData gwInstanceMetadata2 = new InstanceMetaData();
        gwInstanceMetadata2.setInstanceGroup(gwInstanceGroup);
        gwInstanceMetadata2.setPrivateId(11L);
        String gw1DiscoveryFQDN = "gw1.example.com";
        gwInstanceMetadata2.setDiscoveryFQDN(gw1DiscoveryFQDN);

        InstanceGroup workerInstanceGroup = new InstanceGroup();
        workerInstanceGroup.setGroupName("worker");
        workerInstanceGroup.setInstanceGroupType(InstanceGroupType.CORE);

        InstanceMetaData gwInstanceMetadata3 = new InstanceMetaData();
        gwInstanceMetadata3.setInstanceGroup(workerInstanceGroup);
        gwInstanceMetadata3.setPrivateId(12L);
        String gw2DiscoveryFQDN = "gw2.example.com";
        gwInstanceMetadata3.setDiscoveryFQDN(gw2DiscoveryFQDN);

        when(instanceMetaDataService.findNotTerminatedForStack(1L))
                .thenReturn(Set.of(gwInstanceMetadata1, gwInstanceMetadata2, gwInstanceMetadata3));
        underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, CREATED);

        verify(instanceMetaDataService, times(3)).save(instanceMetaDataCaptor.capture());
        List<InstanceMetaData> savedInstanceMetadatas = instanceMetaDataCaptor.getAllValues();
        List<InstanceMetaData> primaryGWs = savedInstanceMetadatas.stream()
                .filter(instanceMetaData -> GATEWAY_PRIMARY.equals(instanceMetaData.getInstanceMetadataType()))
                .collect(Collectors.toList());
        assertEquals(1, primaryGWs.size());
        assertEquals(primaryGWDiscoveryFQDN, primaryGWs.get(0).getDiscoveryFQDN());
        List<InstanceMetaData> gws = savedInstanceMetadatas.stream()
                .filter(instanceMetaData -> GATEWAY.equals(instanceMetaData.getInstanceMetadataType()))
                .collect(Collectors.toList());
        assertEquals(1, gws.size());
        assertEquals(gw1DiscoveryFQDN, gws.get(0).getDiscoveryFQDN());
    }

    @Test
    public void testSaveInstanceMetadataAndSelectTheRightPGWButFQDNDidNotMatchSoFallback() {
        Stack stack = new Stack();
        stack.setId(1L);
        List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = new ArrayList<>();
        cloudVmMetaDataStatuses.add(new CloudVmMetaDataStatus(new CloudVmInstanceStatus(new CloudInstance("id1", new InstanceTemplate("medium", "gateway",
                10L, Collections.emptyList(), InstanceStatus.CREATED, Map.of(), 40L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L), null, "subnet", "az"),
                InstanceStatus.CREATED),
                new CloudInstanceMetaData("1.1.1.1", "1.1.1.1")));
        cloudVmMetaDataStatuses.add(new CloudVmMetaDataStatus(new CloudVmInstanceStatus(new CloudInstance("id2", new InstanceTemplate("medium", "gateway",
                11L, Collections.emptyList(), InstanceStatus.CREATED, Map.of(), 40L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L), null, "subnet", "az"),
                InstanceStatus.CREATED),
                new CloudInstanceMetaData("1.1.1.2", "1.1.1.2")));
        cloudVmMetaDataStatuses.add(new CloudVmMetaDataStatus(new CloudVmInstanceStatus(new CloudInstance("id3", new InstanceTemplate("medium", "worker",
                12L, Collections.emptyList(), InstanceStatus.CREATED, Map.of(), 41L, "imageid", TemporaryStorage.ATTACHED_VOLUMES, 0L), null, "subnet", "az"),
                InstanceStatus.CREATED),
                new CloudInstanceMetaData("1.1.1.3", "1.1.1.3")));
        InstanceMetaData lastTerminatedPGW = new InstanceMetaData();
        String primaryGWDiscoveryFQDN = "primarygw.example.com";
        lastTerminatedPGW.setDiscoveryFQDN(primaryGWDiscoveryFQDN);
        when(instanceMetaDataService.getLastTerminatedPrimaryGatewayInstanceMetadata(1L)).thenReturn(Optional.of(lastTerminatedPGW));

        InstanceMetaData gwInstanceMetadata1 = new InstanceMetaData();
        InstanceGroup gwInstanceGroup = new InstanceGroup();
        gwInstanceGroup.setGroupName("gateway");
        gwInstanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        gwInstanceMetadata1.setInstanceGroup(gwInstanceGroup);
        gwInstanceMetadata1.setPrivateId(10L);
        String gw1DiscoveryFQDN = "gw1.example.com";
        gwInstanceMetadata1.setDiscoveryFQDN(gw1DiscoveryFQDN);

        InstanceMetaData gwInstanceMetadata2 = new InstanceMetaData();
        gwInstanceMetadata2.setInstanceGroup(gwInstanceGroup);
        gwInstanceMetadata2.setPrivateId(11L);
        String gw2DiscoveryFQDN = "gw2.example.com";
        gwInstanceMetadata2.setDiscoveryFQDN(gw2DiscoveryFQDN);

        gwInstanceGroup.setInstanceMetaData(Set.of(gwInstanceMetadata1, gwInstanceMetadata2));

        InstanceGroup workerInstanceGroup = new InstanceGroup();
        workerInstanceGroup.setGroupName("worker");
        workerInstanceGroup.setInstanceGroupType(InstanceGroupType.CORE);

        InstanceMetaData workerInstanceMetadata3 = new InstanceMetaData();
        workerInstanceMetadata3.setInstanceGroup(workerInstanceGroup);
        workerInstanceMetadata3.setPrivateId(12L);
        String worker1DiscoveryFQDN = "worker1.example.com";
        workerInstanceMetadata3.setDiscoveryFQDN(worker1DiscoveryFQDN);

        workerInstanceGroup.setInstanceMetaData(Set.of(workerInstanceMetadata3));

        when(instanceGroupService.findByStackId(1L)).thenReturn(Set.of(gwInstanceGroup, workerInstanceGroup));
        when(instanceMetaDataService.findAllByInstanceGroupAndInstanceStatusOrdered(gwInstanceGroup,
                com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.CREATED))
                .thenReturn(List.of(gwInstanceMetadata1, gwInstanceMetadata2));
        when(instanceMetaDataService.findNotTerminatedForStack(1L)).
                thenReturn(Set.of(gwInstanceMetadata1, gwInstanceMetadata2, workerInstanceMetadata3));
        underTest.saveInstanceMetaData(stack, cloudVmMetaDataStatuses, CREATED);

        verify(instanceMetaDataService, times(4)).save(instanceMetaDataCaptor.capture());
        List<InstanceMetaData> savedInstanceMetadatas = instanceMetaDataCaptor.getAllValues();
        List<InstanceMetaData> primaryGWs = savedInstanceMetadatas.stream()
                .filter(instanceMetaData -> GATEWAY_PRIMARY.equals(instanceMetaData.getInstanceMetadataType()))
                .distinct()
                .collect(Collectors.toList());
        assertEquals(1, primaryGWs.size());
        assertEquals(gw1DiscoveryFQDN, primaryGWs.get(0).getDiscoveryFQDN());
        List<InstanceMetaData> gws = savedInstanceMetadatas.stream()
                .filter(instanceMetaData -> GATEWAY.equals(instanceMetaData.getInstanceMetadataType()))
                .collect(Collectors.toList());
        assertEquals(1, gws.size());
        assertEquals(gw2DiscoveryFQDN, gws.get(0).getDiscoveryFQDN());
    }

    private Image getEmptyImage() {
        return new Image(null, null, null, null, null, null, null, null);
    }

    private Iterable<CloudVmMetaDataStatus> getCloudVmMetaDataStatuses(InstanceStatus instanceStatus) {
        InstanceTemplate instanceTemplate = new InstanceTemplate(null, GROUP_NAME, PRIVATE_ID, List.of(), null, Map.of(), null, null,
                TemporaryStorage.ATTACHED_VOLUMES, 0L);
        Map<String, Object> params = new HashMap<>();
        params.put(CloudInstance.INSTANCE_NAME, INSTANCE_NAME);
        CloudInstance cloudInstance = new CloudInstance(null, instanceTemplate, null, null, null, params);
        CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, instanceStatus);
        CloudInstanceMetaData cloudInstanceMetaData =
                new CloudInstanceMetaData(PRIVATE_IP, PUBLIC_IP, SSH_PORT, LOCALITY_INDICATOR, CloudInstanceLifeCycle.SPOT);
        CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, cloudInstanceMetaData);
        return List.of(cloudVmMetaDataStatus);
    }

    private Iterable<CloudLoadBalancerMetadata> getCloudLoadBalancerMetaDataStatuses() {
        return List.of(new CloudLoadBalancerMetadata.Builder().withType(LoadBalancerType.PUBLIC)
                .withCloudDns(CLOUD_DNS)
                .withHostedZoneId(HOSTED_ZONE)
                .withIp(PUBLIC_IP).withName(LB_NAME).build());
    }

    private void assertCommonProperties(InstanceMetaData instanceMetaData) {
        assertEquals(PRIVATE_IP, instanceMetaData.getPrivateIp());
        assertEquals(PUBLIC_IP, instanceMetaData.getPublicIp());
        assertEquals(SSH_PORT, instanceMetaData.getSshPort());
        assertEquals(LOCALITY_INDICATOR, instanceMetaData.getLocalityIndicator());
        assertEquals(INSTANCE_GROUP_ID, instanceMetaData.getInstanceGroup().getId());
        assertNull(instanceMetaData.getInstanceId());
        assertEquals(PRIVATE_ID, instanceMetaData.getPrivateId());
        assertEquals(CURRENT_TIME, instanceMetaData.getStartDate());
        assertNull(instanceMetaData.getSubnetId());
        assertThat(instanceMetaData.getAvailabilityZone()).isNull();
        assertThat(instanceMetaData.getRackId()).isNull();
        assertEquals(INSTANCE_NAME, instanceMetaData.getInstanceName());
        assertEquals(Boolean.FALSE, instanceMetaData.getAmbariServer());
        assertEquals(Boolean.FALSE, instanceMetaData.getClusterManagerServer());
        assertEquals(InstanceMetadataType.CORE, instanceMetaData.getInstanceMetadataType());
        assertEquals(InstanceLifeCycle.SPOT, instanceMetaData.getLifeCycle());
    }

    private static class TestStackStatusView implements StackStatusView {
        private Long id;

        private StackStatus status;

        public Long getId() {
            return id;
        }

        @Override
        public String getName() {
            return null;
        }

        public StackStatus getStatus() {
            return status;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public void setStatus(StackStatus status) {
            this.status = status;
        }
    }
}