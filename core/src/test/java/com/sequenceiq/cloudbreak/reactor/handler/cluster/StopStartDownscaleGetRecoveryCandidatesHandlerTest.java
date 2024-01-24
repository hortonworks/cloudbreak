package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleGetRecoveryCandidatesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleGetRecoveryCandidatesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stopstart.RecoveryCandidateCollectionService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class StopStartDownscaleGetRecoveryCandidatesHandlerTest {

    private static final String MOCK_INSTANCEID_PREFIX = "i-";

    private static final String MOCK_FQDN_PREFIX = "fqdn-";

    private static final String INSTANCE_GROUP_NAME = "compute";

    private static final Integer ALL_INSTANCES_IN_HG_COUNT = 20;

    private static final Integer RUNNING_INSTANCES_COUNT = 16;

    private static final Integer UNHEALTHY_RUNNING_INSTANCES_COUNT = 3;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private InstanceConnector instanceConnector;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private RecoveryCandidateCollectionService recoveryCandidateCollectionService;

    @Mock
    private StackDto stackDto;

    @Captor
    private ArgumentCaptor<Event> argumentCaptor;

    @InjectMocks
    private StopStartDownscaleGetRecoveryCandidatesHandler underTest;

    @BeforeEach
    void setup() {
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        CloudPlatformVariant cloudPlatformVariant = mock(CloudPlatformVariant.class);

        lenient().doReturn(cloudPlatformVariant).when(cloudContext).getPlatformVariant();
        lenient().doReturn(cloudConnector).when(cloudPlatformConnectors).get(any(CloudPlatformVariant.class));
        lenient().doReturn(authenticator).when(cloudConnector).authentication();
        lenient().doReturn(ac).when(authenticator).authenticate(any(CloudContext.class), any(CloudCredential.class));
        lenient().doReturn(instanceConnector).when(cloudConnector).instances();
    }

    @Test
    void testType() {
        assertEquals(StopStartDownscaleGetRecoveryCandidatesRequest.class, underTest.type());
    }

    @Test
    void testGetRecoveryCandidates() {
        List<CloudInstance> allCloudInstancesInHg = generateCloudInstances(ALL_INSTANCES_IN_HG_COUNT);
        List<InstanceMetadataView> recoveryCandidates = generateInstanceMetadata(UNHEALTHY_RUNNING_INSTANCES_COUNT);
        Set<Long> hostIds = generateInstanceMetadata(ALL_INSTANCES_IN_HG_COUNT)
                .stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toSet());
        StopStartDownscaleGetRecoveryCandidatesRequest request = new StopStartDownscaleGetRecoveryCandidatesRequest(cloudContext, cloudCredential,
                cloudStack, INSTANCE_GROUP_NAME, allCloudInstancesInHg, hostIds, Boolean.TRUE);

        Event event = new Event(request);

        setupBasicMocks(stackDto, recoveryCandidates, UNHEALTHY_RUNNING_INSTANCES_COUNT);

        underTest.accept(event);

        verify(eventBus).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        StopStartDownscaleGetRecoveryCandidatesResult result = (StopStartDownscaleGetRecoveryCandidatesResult) resultEvent.getData();

        makeBasicAssertions(resultEvent, result, hostIds);
        assertThat(result.getStartedInstancesWithServicesNotRunning()).hasSize(UNHEALTHY_RUNNING_INSTANCES_COUNT);
    }

    @Test
    void testGetNoRecoveryCandidates() {
        List<CloudInstance> allCloudInstancesInHg = generateCloudInstances(ALL_INSTANCES_IN_HG_COUNT);
        List<InstanceMetadataView> recoveryCandidates = generateInstanceMetadata(UNHEALTHY_RUNNING_INSTANCES_COUNT);
        Set<Long> hostIds = generateInstanceMetadata(ALL_INSTANCES_IN_HG_COUNT)
                .stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toSet());
        StopStartDownscaleGetRecoveryCandidatesRequest request = new StopStartDownscaleGetRecoveryCandidatesRequest(cloudContext, cloudCredential,
                cloudStack, INSTANCE_GROUP_NAME, allCloudInstancesInHg, hostIds, Boolean.TRUE);

        Event event = new Event(request);

        setupBasicMocks(stackDto, recoveryCandidates, UNHEALTHY_RUNNING_INSTANCES_COUNT);
        doReturn(Collections.emptyList()).when(recoveryCandidateCollectionService).getStartedInstancesWithServicesNotRunning(any(StackDto.class),
                anyString(), anySet(), anyBoolean());

        underTest.accept(event);

        verify(eventBus).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        StopStartDownscaleGetRecoveryCandidatesResult result = (StopStartDownscaleGetRecoveryCandidatesResult) resultEvent.getData();

        makeBasicAssertions(resultEvent, result, hostIds);
        assertThat(result.getStartedInstancesWithServicesNotRunning()).isEmpty();
        verify(instanceMetaDataToCloudInstanceConverter, never()).convert(eq(recoveryCandidates), any(StackView.class));
    }

    @Test
    void testGetRecoveryCandidatesThrowsExceptionButFlowIsNotCancelled() {
        List<CloudInstance> allCloudInstancesInHg = generateCloudInstances(ALL_INSTANCES_IN_HG_COUNT);
        Set<Long> hostIds = generateInstanceMetadata(ALL_INSTANCES_IN_HG_COUNT)
                .stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toSet());
        StopStartDownscaleGetRecoveryCandidatesRequest request = new StopStartDownscaleGetRecoveryCandidatesRequest(cloudContext, cloudCredential,
                cloudStack, INSTANCE_GROUP_NAME, allCloudInstancesInHg, hostIds, Boolean.TRUE);

        Event event = new Event(request);

        doThrow(RuntimeException.class).when(stackDtoService).getById(anyLong());

        underTest.accept(event);

        verify(eventBus).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        StopStartDownscaleGetRecoveryCandidatesResult result = (StopStartDownscaleGetRecoveryCandidatesResult) resultEvent.getData();

        assertThat(resultEvent.getData().getClass()).isEqualTo(StopStartDownscaleGetRecoveryCandidatesResult.class);
        assertThat(result.getHostGroupName()).isEqualTo(INSTANCE_GROUP_NAME);
        assertThat(result.getStartedInstancesWithServicesNotRunning().size()).isZero();
        assertThat(result.getHostIds()).isEqualTo(hostIds);
    }

    @Test
    void testGetRecoveryCandidatesWhenEntitlementDisabled() {
        List<CloudInstance> allCloudInstancesInHg = generateCloudInstances(ALL_INSTANCES_IN_HG_COUNT);
        List<InstanceMetadataView> allInstancesMetadata = generateInstanceMetadata(ALL_INSTANCES_IN_HG_COUNT);
        Set<Long> hostIds = allInstancesMetadata.stream().map(InstanceMetadataView::getPrivateId).collect(Collectors.toSet());
        StopStartDownscaleGetRecoveryCandidatesRequest request = new StopStartDownscaleGetRecoveryCandidatesRequest(cloudContext, cloudCredential,
                cloudStack, INSTANCE_GROUP_NAME, allCloudInstancesInHg, hostIds, Boolean.FALSE);

        Event event = new Event(request);

        underTest.accept(event);

        verify(eventBus).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        StopStartDownscaleGetRecoveryCandidatesResult result = (StopStartDownscaleGetRecoveryCandidatesResult) resultEvent.getData();

        verify(instanceConnector, never()).checkWithoutRetry(any(AuthenticatedContext.class), eq(allCloudInstancesInHg));
        assertThat(resultEvent.getData().getClass()).isEqualTo(StopStartDownscaleGetRecoveryCandidatesResult.class);
        assertThat(result.getHostGroupName()).isEqualTo(INSTANCE_GROUP_NAME);
        assertThat(result.getHostIds()).isEqualTo(hostIds);
        assertThat(result.getStartedInstancesWithServicesNotRunning()).isEmpty();
    }

    private void setupBasicMocks(StackDto stack, List<InstanceMetadataView> recoveryCandidates, int unhealthyCount) {
        StackView stackView = mock(StackView.class);
        doReturn(stack).when(stackDtoService).getById(anyLong());
        lenient().doReturn(recoveryCandidates).when(recoveryCandidateCollectionService).getStartedInstancesWithServicesNotRunning(any(StackDto.class),
                anyString(), anySet(), anyBoolean());
        lenient().doReturn(stackView).when(stack).getStack();
        lenient().doReturn(generateCloudInstances(unhealthyCount))
                .when(instanceMetaDataToCloudInstanceConverter).convert(anyList(), any(StackView.class));
    }

    private void makeBasicAssertions(Event resultEvent, StopStartDownscaleGetRecoveryCandidatesResult result,
            Set<Long> hostIds) {
        assertThat(resultEvent.getData().getClass()).isEqualTo(StopStartDownscaleGetRecoveryCandidatesResult.class);
        assertThat(result.getHostGroupName()).isEqualTo(INSTANCE_GROUP_NAME);
        assertThat(result.getHostIds()).isEqualTo(hostIds);
    }

    private List<CloudInstance> generateCloudInstances(int numInstances) {
        List<CloudInstance> instances = new LinkedList<>();
        IntStream.range(0, numInstances).forEach(i -> {
            CloudInstance instance = mock(CloudInstance.class);
            lenient().doReturn(MOCK_INSTANCEID_PREFIX + i).when(instance).getInstanceId();
            instances.add(instance);
        });
        return instances;
    }

    private List<InstanceMetadataView> generateInstanceMetadata(int count) {
        List<InstanceMetadataView> instances = new ArrayList<>(count);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(INSTANCE_GROUP_NAME);
        IntStream.range(0, count).forEach(i -> {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId(MOCK_INSTANCEID_PREFIX + i);
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setDiscoveryFQDN(MOCK_FQDN_PREFIX + i);
            instanceMetaData.setPrivateId((long) i);
            instances.add(instanceMetaData);
        });

        return instances;
    }

}