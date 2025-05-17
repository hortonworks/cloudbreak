package com.sequenceiq.cloudbreak.service.instancechecker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.conf.InstanceCheckerConfig;
import com.sequenceiq.cloudbreak.converter.spi.CloudContextProvider;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@ExtendWith(MockitoExtension.class)
class InstanceCheckerServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String INSTANCE_ID1 = "instanceId1";

    private static final String INSTANCE_ID2 = "instanceId2";

    private static final String INSTANCE_ID3 = "instanceId3";

    private static final String INSTANCE_ID4 = "instanceId4";

    private static final String INSTANCE_ID5 = "instanceId5";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private CloudContextProvider cloudContextProvider;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private StackService stackService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private InstanceCheckerConfig instanceCheckerConfig;

    @Mock
    private MeteringConfig meteringConfig;

    @Mock
    private StackToCloudStackConverter stackToCloudStackConverter;

    @InjectMocks
    private InstanceCheckerService underTest;

    @Captor
    private ArgumentCaptor<InstanceMetaData> instanceMetaDataCaptor;

    @Captor
    private ArgumentCaptor<List<String>> knownInstancesCaptor;

    static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of(true, true, true),
                Arguments.of(true, true, false),
                Arguments.of(true, false, true),
                Arguments.of(true, false, false),
                Arguments.of(false, true, true)
        );
    }

    @MethodSource("testCases")
    @ParameterizedTest
    void testCheckInstances(boolean instanceCheckerConfigEnabled, boolean meteringEnabled, boolean meteringInstanceCheckerEnabled) {
        MetadataCollector metadataCollector = mock(MetadataCollector.class);
        Stack stack = stack();
        List<InstanceCheckMetadata> providerInstances = providerInstances();
        when(instanceCheckerConfig.isEnabled()).thenReturn(instanceCheckerConfigEnabled);
        when(meteringConfig.isEnabled()).thenReturn(meteringEnabled);
        lenient().when(meteringConfig.isInstanceCheckerEnabled()).thenReturn(meteringInstanceCheckerEnabled);
        setupCommonMocks(stack, providerInstances, metadataCollector);

        underTest.checkInstances(STACK_ID);

        if (instanceCheckerConfigEnabled) {
            verify(metadataCollector)
                    .collectCdpInstances(any(AuthenticatedContext.class), eq(RESOURCE_CRN), any(CloudStack.class), knownInstancesCaptor.capture());
            assertThat(knownInstancesCaptor.getValue()).containsExactlyInAnyOrder(INSTANCE_ID1, INSTANCE_ID3);
            verify(cloudbreakEventService).fireCloudbreakEvent(STACK_ID, "POSSIBLE_ORPHAN_INSTANCES",
                    ResourceEvent.STACK_POSSIBLE_ORPHAN_INSTANCES, Set.of(List.of(INSTANCE_ID2).toString()));
        }
        if (meteringEnabled && meteringInstanceCheckerEnabled) {
            verify(instanceMetaDataService).save(instanceMetaDataCaptor.capture());
            assertEquals(INSTANCE_ID3, instanceMetaDataCaptor.getValue().getInstanceId());
            verify(cloudbreakEventService).fireCloudbreakEvent(STACK_ID, "PROVIDER_INSTANCES_ARE_DIFFERENT",
                    ResourceEvent.STACK_PROVIDER_INSTANCE_TYPE_MISMATCH, Set.of(Set.of(INSTANCE_ID3).toString()));
        }
    }

    private void setupCommonMocks(Stack stack, List<InstanceCheckMetadata> providerInstances, MetadataCollector metadataCollectorMock) {
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudPlatformVariant cloudPlatformVariant = mock(CloudPlatformVariant.class);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(cloudContextProvider.getCloudContext(stack)).thenReturn(cloudContext);
        when(credentialClientService.getCloudCredential(ENVIRONMENT_CRN)).thenReturn(cloudCredential);
        when(cloudContext.getPlatformVariant()).thenReturn(cloudPlatformVariant);
        when(cloudPlatformConnectors.get(cloudPlatformVariant)).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContext, cloudCredential)).thenReturn(ac);
        when(cloudConnector.metadata()).thenReturn(metadataCollectorMock);
        when(stackToCloudStackConverter.convert(stack)).thenReturn(cloudStack);
        when(metadataCollectorMock.collectCdpInstances(eq(ac), eq(RESOURCE_CRN), eq(cloudStack), anyList())).thenReturn(providerInstances);
        when(cloudbreakEventService.cloudbreakLastEventsForStack(STACK_ID, "datahub", 2)).thenReturn(List.of());
    }

    private static Stack stack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setResourceCrn(RESOURCE_CRN);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setType(StackType.WORKLOAD);

        InstanceMetaData instance1 = new InstanceMetaData();
        instance1.setDiscoveryFQDN("host1");
        instance1.setInstanceId(INSTANCE_ID1);
        instance1.setProviderInstanceType("small");

        InstanceMetaData instance3 = new InstanceMetaData();
        instance3.setDiscoveryFQDN("host3");
        instance3.setInstanceId(INSTANCE_ID3);
        instance3.setProviderInstanceType("small");

        InstanceGroup instanceGroup = new InstanceGroup();
        stack.setInstanceGroups(Set.of(instanceGroup));
        instanceGroup.setInstanceMetaData(Set.of(instance1, instance3));

        Template template = new Template();
        template.setInstanceType("small");
        instanceGroup.setTemplate(template);
        return stack;
    }

    private static List<InstanceCheckMetadata> providerInstances() {
        return List.of(
                InstanceCheckMetadata.builder().withInstanceId(INSTANCE_ID1).withInstanceType("small").withStatus(InstanceStatus.CREATED).build(),
                InstanceCheckMetadata.builder().withInstanceId(INSTANCE_ID2).withInstanceType("small").withStatus(InstanceStatus.CREATED).build(),
                InstanceCheckMetadata.builder().withInstanceId(INSTANCE_ID3).withInstanceType("large").withStatus(InstanceStatus.CREATED).build(),
                InstanceCheckMetadata.builder().withInstanceId(INSTANCE_ID4).withInstanceType("small").withStatus(InstanceStatus.IN_PROGRESS).build(),
                InstanceCheckMetadata.builder().withInstanceId(INSTANCE_ID5).withInstanceType("small").withStatus(InstanceStatus.TERMINATED).build()
        );
    }
}