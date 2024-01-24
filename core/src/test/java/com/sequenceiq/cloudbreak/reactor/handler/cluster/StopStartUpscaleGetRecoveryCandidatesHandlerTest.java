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
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleGetRecoveryCandidatesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleGetRecoveryCandidatesResult;
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
class StopStartUpscaleGetRecoveryCandidatesHandlerTest {

    private static final String INSTANCE_GROUP_NAME = "compute";

    private static final String MOCK_INSTANCEID_PREFIX = "i-";

    private static final String MOCK_FQDN_PREFIX = "fqdn-";

    private static final Integer ALL_INSTANCES_IN_HG_COUNT = 20;

    private static final Integer RUNNING_INSTANCES_COUNT = 10;

    private static final Integer RECOVERY_CANDIDATES_COUNT = 3;

    private static final Integer ADJUSTMENT = 7;

    @InjectMocks
    private StopStartUpscaleGetRecoveryCandidatesHandler underTest;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

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

    @BeforeEach
    void setUp() {
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
    void type() {
        assertEquals(StopStartUpscaleGetRecoveryCandidatesRequest.class, underTest.type());
    }

    @Test
    void testGetRecoveryCandidates() {
        List<CloudInstance> allCloudInstancesInHg = generateCloudInstances(ALL_INSTANCES_IN_HG_COUNT);
        List<InstanceMetadataView> recoveryCandidates = generateInstanceMetadata(RECOVERY_CANDIDATES_COUNT);

        StopStartUpscaleGetRecoveryCandidatesRequest request = new StopStartUpscaleGetRecoveryCandidatesRequest(cloudContext, cloudCredential, cloudStack,
                INSTANCE_GROUP_NAME, ADJUSTMENT, allCloudInstancesInHg, Boolean.TRUE);

        Event event = new Event(request);

        setupBasicMocks(stackDto, recoveryCandidates, RECOVERY_CANDIDATES_COUNT);

        underTest.accept(event);
        verify(eventBus).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        StopStartUpscaleGetRecoveryCandidatesResult result = (StopStartUpscaleGetRecoveryCandidatesResult) resultEvent.getData();

        makeBasicAssertions(resultEvent, result, request);
        assertThat(result.getStartedInstancesWithServicesNotRunning()).hasSize(RECOVERY_CANDIDATES_COUNT);
    }

    @Test
    void testGetNoRecoveryCandidates() {
        List<CloudInstance> allCloudInstancesInHg = generateCloudInstances(ALL_INSTANCES_IN_HG_COUNT);
        List<InstanceMetadataView> recoveryCandidates = generateInstanceMetadata(RECOVERY_CANDIDATES_COUNT);

        StopStartUpscaleGetRecoveryCandidatesRequest request = new StopStartUpscaleGetRecoveryCandidatesRequest(cloudContext, cloudCredential, cloudStack,
                INSTANCE_GROUP_NAME, ADJUSTMENT, allCloudInstancesInHg, Boolean.TRUE);

        Event event = new Event(request);

        setupBasicMocks(stackDto, recoveryCandidates, RECOVERY_CANDIDATES_COUNT);
        doReturn(Collections.emptyList()).when(recoveryCandidateCollectionService).getStartedInstancesWithServicesNotRunning(any(StackDto.class),
                anyString(), anySet(), anyBoolean());

        underTest.accept(event);
        verify(eventBus).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        StopStartUpscaleGetRecoveryCandidatesResult result = (StopStartUpscaleGetRecoveryCandidatesResult) resultEvent.getData();

        makeBasicAssertions(resultEvent, result, request);
        verify(instanceMetaDataToCloudInstanceConverter, never()).convert(eq(recoveryCandidates), any(StackView.class));
        assertThat(result.getStartedInstancesWithServicesNotRunning()).isEmpty();
    }

    @Test
    void testGetRecoveryCandidatesThrowsExceptionButFlowIsNotCancelled() {
        List<CloudInstance> allCloudInstancesInHg = generateCloudInstances(ALL_INSTANCES_IN_HG_COUNT);

        StopStartUpscaleGetRecoveryCandidatesRequest request = new StopStartUpscaleGetRecoveryCandidatesRequest(cloudContext, cloudCredential, cloudStack,
                INSTANCE_GROUP_NAME, ADJUSTMENT, allCloudInstancesInHg, Boolean.TRUE);

        Event event = new Event(request);

        doThrow(RuntimeException.class).when(stackDtoService).getById(anyLong());

        underTest.accept(event);

        verify(eventBus).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        StopStartUpscaleGetRecoveryCandidatesResult result = (StopStartUpscaleGetRecoveryCandidatesResult) resultEvent.getData();

        assertThat(resultEvent.getData().getClass()).isEqualTo(StopStartUpscaleGetRecoveryCandidatesResult.class);
        assertThat(result.getHostGroupName()).isEqualTo(INSTANCE_GROUP_NAME);
        assertThat(result.getStartedInstancesWithServicesNotRunning()).isEmpty();
        assertThat(result.getGetStartedInstancesRequest()).isEqualTo(request);
        assertThat(result.getAdjustment()).isEqualTo(ADJUSTMENT);
    }

    @Test
    void testGetRecoveryCandidatesWhenEntitlementDisabled() {
        List<CloudInstance> allCloudInstancesInHg = generateCloudInstances(ALL_INSTANCES_IN_HG_COUNT);

        StopStartUpscaleGetRecoveryCandidatesRequest request = new StopStartUpscaleGetRecoveryCandidatesRequest(cloudContext, cloudCredential, cloudStack,
                INSTANCE_GROUP_NAME, ADJUSTMENT, allCloudInstancesInHg, Boolean.FALSE);

        Event event = new Event(request);

        underTest.accept(event);

        verify(eventBus).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        StopStartUpscaleGetRecoveryCandidatesResult result = (StopStartUpscaleGetRecoveryCandidatesResult) resultEvent.getData();

        verify(instanceConnector, never()).checkWithoutRetry(any(AuthenticatedContext.class), anyList());
        assertThat(resultEvent.getData().getClass()).isEqualTo(StopStartUpscaleGetRecoveryCandidatesResult.class);
        assertThat(result.getHostGroupName()).isEqualTo(INSTANCE_GROUP_NAME);
        assertThat(result.getGetStartedInstancesRequest()).isEqualTo(request);
        assertThat(result.getAdjustment()).isEqualTo(ADJUSTMENT);
        assertThat(result.getStartedInstancesWithServicesNotRunning()).isEmpty();
    }

    private void setupBasicMocks(StackDto stack, List<InstanceMetadataView> recoveryCandidates, int unhealthyCount) {
        StackView stackView = mock(StackView.class);
        doReturn(stack).when(stackDtoService).getById(anyLong());
        lenient().doReturn(stackView).when(stack).getStack();
        lenient().doReturn(recoveryCandidates).when(recoveryCandidateCollectionService).getStartedInstancesWithServicesNotRunning(any(StackDto.class),
                anyString(), anySet(), anyBoolean());
        lenient().doReturn(generateCloudInstances(unhealthyCount))
                .when(instanceMetaDataToCloudInstanceConverter).convert(anyList(), any(StackView.class));
    }

    private void makeBasicAssertions(Event resultEvent, StopStartUpscaleGetRecoveryCandidatesResult result,
            StopStartUpscaleGetRecoveryCandidatesRequest request) {
        assertThat(resultEvent.getData().getClass()).isEqualTo(StopStartUpscaleGetRecoveryCandidatesResult.class);
        assertThat(result.getHostGroupName()).isEqualTo(INSTANCE_GROUP_NAME);
        assertThat(result.getGetStartedInstancesRequest()).isEqualTo(request);
        assertThat(result.getAdjustment()).isEqualTo(ADJUSTMENT);
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