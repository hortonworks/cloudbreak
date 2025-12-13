package com.sequenceiq.cloudbreak.cloud.aws.resource.instance;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.NOT_FOUND;
import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.USERDATA_SECRET_ID;
import static com.sequenceiq.common.model.DefaultApplicationTag.RESOURCE_CRN;
import static com.sequenceiq.common.model.DefaultApplicationTag.RESOURCE_ID;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.VolumeBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsStackNameCommonUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util.SecurityGroupBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.template.init.SshKeyNameGenerator;
import com.sequenceiq.cloudbreak.cloud.util.UserdataSecretsUtil;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.ArchitectureValues;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.HttpTokensState;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceMetadataOptionsResponse;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceMetadataOptionsResponse;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;

@ExtendWith(MockitoExtension.class)
class AwsNativeInstanceResourceBuilderTest {

    private static final String INSTANCE_ID = "instanceId";

    private static final String TEST_USERDATA = """
            export NOT_SECRET1="not_a_secret1"
            ###SECRETS-START
            export SECRET1="secret1"
            export SECRET2="secret2"
            ###SECRETS-END
            export NOT_SECRET2="not_a_secret2"
            """;

    private static final String USERDATA_WITH_SECRETS_REPLACED = """
            export NOT_SECRET1="not_a_secret1"
            export USERDATA_SECRET_ID="testsecretid"
            export NOT_SECRET2="not_a_secret2"
            """;

    @InjectMocks
    private AwsNativeInstanceResourceBuilder underTest;

    @Mock
    private AwsContext awsContext;

    @Mock
    private AwsMethodExecutor awsMethodExecutor;

    @Spy
    private AwsTaggingService awsTaggingService;

    @Mock
    private SecurityGroupBuilderUtil securityGroupBuilderUtil;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private Network network;

    @Mock
    private Security security;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @Mock
    private Group group;

    @Mock
    private CloudInstance cloudInstance;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private Supplier<Object> supplier;

    @Mock
    private VolumeBuilderUtil volumeBuilderUtil;

    @Mock
    private InstanceTemplate instanceTemplate;

    @Mock
    private AwsStackNameCommonUtil awsStackNameCommonUtil;

    @Mock
    private SshKeyNameGenerator sshKeyNameGenerator;

    @Mock
    private UserdataSecretsUtil userdataSecretsUtil;

    static Object[][] testBuildWhenInstanceNoExistSource() {
        return new Object[][]{
                // supportedImdsVersionOfStack, expectedTokenState, secretEncryptionEnabled
                {"v2", HttpTokensState.REQUIRED, false},
                {null, null, false},
                {null, null, true}
        };
    }

    @BeforeEach
    void setup() {
        CloudContext cloudContext = mock(CloudContext.class);
        lenient().when(ac.getCloudContext()).thenReturn(cloudContext);
        lenient().when(cloudContext.getAccountId()).thenReturn("account");
    }

    @Test
    void testBuildWhenBuildableResorucesAreEmpty() {
        long privateId = 0;
        CloudConnectorException actual = assertThrows(CloudConnectorException.class,
                () -> underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.emptyList(), cloudStack));
        assertEquals("Buildable resources cannot be empty!", actual.getMessage());
    }

    @ParameterizedTest
    @MethodSource("testBuildWhenInstanceNoExistSource")
    void testBuildWhenInstanceNoExist(String supportedImdsVersionOfStack, HttpTokensState expectedTokenState,
            boolean secretEncryptionEnabled) throws Exception {
        Instance instance = Instance.builder().instanceId(INSTANCE_ID).architecture(ArchitectureValues.X86_64).build();
        RunInstancesResponse runInstancesResponse = RunInstancesResponse.builder().instances(instance).build();
        InstanceAuthentication authentication = mock(InstanceAuthentication.class);
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("groupName")
                .withParameters(emptyMap())
                .build();

        Image image = mock(Image.class);

        long privateId = 0;
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenReturn(Optional.empty());
        when(amazonEc2Client.createInstance(any())).thenReturn(runInstancesResponse);
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(group.getName()).thenReturn("groupName");
        when(cloudStack.getImage()).thenReturn(image);
        when(image.getImageName()).thenReturn("img-name");
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(securityGroupBuilderUtil.getSecurityGroupIds(awsContext, group)).thenReturn(List.of("sg-id"));
        when(awsStackNameCommonUtil.getInstanceName(ac, "groupName", privateId)).thenReturn("stackname");
        when(cloudStack.getSupportedImdsVersion()).thenReturn(supportedImdsVersionOfStack);
        when(cloudStack.getUserDataByType(any())).thenReturn(TEST_USERDATA);
        when(cloudInstance.hasParameter(USERDATA_SECRET_ID)).thenReturn(secretEncryptionEnabled);
        when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);
        if (secretEncryptionEnabled) {
            when(cloudInstance.getStringParameter(USERDATA_SECRET_ID)).thenReturn("testsecretid");
            when(userdataSecretsUtil.replaceSecretsWithSecretId(TEST_USERDATA, "testsecretid")).thenReturn(USERDATA_WITH_SECRETS_REPLACED);
        }

        ArgumentCaptor<RunInstancesRequest> runInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(RunInstancesRequest.class);
        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        verify(amazonEc2Client).createInstance(runInstancesRequestArgumentCaptor.capture());
        RunInstancesRequest runInstancesRequest = runInstancesRequestArgumentCaptor.getValue();
        assertEquals(actual.get(0).getInstanceId(), INSTANCE_ID);
        if (expectedTokenState != null) {
            assertEquals(runInstancesRequest.metadataOptions().httpTokens(), expectedTokenState);
        } else {
            assertNull(runInstancesRequest.metadataOptions());
        }
        if (secretEncryptionEnabled) {
            assertEquals(Base64Util.encode(USERDATA_WITH_SECRETS_REPLACED), runInstancesRequest.userData());
        } else {
            assertEquals(Base64Util.encode(TEST_USERDATA), runInstancesRequest.userData());
        }
        assertEquals("sg-id", runInstancesRequest.securityGroupIds().get(0));
        assertThat(runInstancesRequest.tagSpecifications().get(0)).matches(ts -> ts.tags().stream()
                .anyMatch(t -> "Name".equals(t.key()) && "stackname".equals(t.value())));
        verify(awsTaggingService, times(1)).prepareEc2TagSpecification(anyMap(),
                eq(software.amazon.awssdk.services.ec2.model.ResourceType.INSTANCE));
        verify(sshKeyNameGenerator, times(1)).getKeyPairName(any(), any());
        verify(awsTaggingService, times(1)).prepareEc2TagSpecification(anyMap(),
                eq(software.amazon.awssdk.services.ec2.model.ResourceType.VOLUME));
        verify(instanceTemplate, times(1)).putParameter(eq(CloudResource.ARCHITECTURE), eq(ArchitectureValues.X86_64.name()));
    }

    @Test
    void testBuildWhenExistingNameTagShouldNotOverride() throws Exception {
        Instance instance = Instance.builder().instanceId(INSTANCE_ID).architecture(ArchitectureValues.X86_64).build();
        RunInstancesResponse runInstancesResponse = RunInstancesResponse.builder().instances(instance).build();
        InstanceAuthentication authentication = mock(InstanceAuthentication.class);
        String groupName = "groupName";
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withGroup(groupName)
                .withParameters(emptyMap())
                .build();

        Image image = mock(Image.class);

        long privateId = 0;
        String instanceName = "instanceName1";
        when(awsStackNameCommonUtil.getInstanceName(ac, groupName, privateId)).thenReturn(instanceName);
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).then(InvocationOnMock::callRealMethod);
        when(amazonEc2Client.describeInstances(any())).thenReturn(DescribeInstancesResponse.builder().build());
        when(amazonEc2Client.createInstance(any())).thenReturn(runInstancesResponse);
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(group.getName()).thenReturn(groupName);
        when(cloudStack.getImage()).thenReturn(image);
        when(cloudStack.getTags()).thenReturn(Map.of("Name", "doNotOverride"));
        when(image.getImageName()).thenReturn("img-name");
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(securityGroupBuilderUtil.getSecurityGroupIds(awsContext, group)).thenReturn(List.of("sg-id"));
        when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);
        ArgumentCaptor<RunInstancesRequest> runInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(RunInstancesRequest.class);
        underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        verify(amazonEc2Client).createInstance(runInstancesRequestArgumentCaptor.capture());
        RunInstancesRequest runInstancesRequest = runInstancesRequestArgumentCaptor.getValue();
        assertThat(runInstancesRequest.tagSpecifications().get(0)).matches(ts -> ts.tags().stream()
                .anyMatch(t -> "Name".equals(t.key()) && "doNotOverride".equals(t.value())));
        verify(awsTaggingService, times(1)).prepareEc2TagSpecification(anyMap(),
                eq(software.amazon.awssdk.services.ec2.model.ResourceType.INSTANCE));
        verify(awsTaggingService, times(1)).prepareEc2TagSpecification(anyMap(),
                eq(software.amazon.awssdk.services.ec2.model.ResourceType.VOLUME));
        ArgumentCaptor<DescribeInstancesRequest> describeInstanceRequest = ArgumentCaptor.forClass(DescribeInstancesRequest.class);
        verify(amazonEc2Client, times(1)).describeInstances(describeInstanceRequest.capture());
        DescribeInstancesRequest describeInstancesRequestFor = describeInstanceRequest.getValue();
        assertEquals(instanceName, describeInstancesRequestFor.filters().get(0).values().get(0));
    }

    @Test
    void testBuildWhenInstanceExistAndRunningWithoutStackId() throws Exception {
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withParameters(emptyMap())
                .build();
        long privateId = 0;
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .architecture(ArchitectureValues.X86_64)
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE).build())
                .build();
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenCallRealMethod();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance).build())
                .build();
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(amazonEc2Client.describeInstances(any())).thenReturn(response);
        when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);

        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        assertEquals(INSTANCE_ID, actual.getFirst().getInstanceId());
        verify(amazonEc2Client, times(0)).startInstances(any());
    }

    @Test
    void testBuildWhenInstanceExistAndRunningWithSameStackId() throws Exception {
        String stackId = "id";
        String instanceName = "instance-name";
        when(cloudStack.getTags()).thenReturn(Map.of(RESOURCE_ID.key(), stackId));
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withParameters(emptyMap())
                .build();
        long privateId = 0;
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .architecture(ArchitectureValues.X86_64)
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE).build())
                .tags(Tag.builder().key(RESOURCE_ID.key()).value(stackId).build())
                .build();
        when(awsStackNameCommonUtil.getInstanceName(any(), any(), any())).thenReturn(instanceName);
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenCallRealMethod();
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResponse response = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance).build())
                .build();
        ArgumentCaptor<DescribeInstancesRequest> requestCaptor = ArgumentCaptor.forClass(DescribeInstancesRequest.class);
        when(amazonEc2Client.describeInstances(requestCaptor.capture())).thenReturn(response);
        when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);

        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        assertEquals(INSTANCE_ID, actual.getFirst().getInstanceId());
        verify(amazonEc2Client, times(0)).startInstances(any());
        DescribeInstancesRequest request = requestCaptor.getValue();
        assertThat(request.filters())
                .hasSize(2)
                .anyMatch(filter -> "tag:Name".equals(filter.name()) && filter.values().contains(instanceName))
                .anyMatch(filter -> ("tag:" + RESOURCE_ID.key()).equals(filter.name()) && filter.values().contains(stackId));
    }

    @Test
    void testBuildWhenInstanceExistAndRunningWithSameCrnNoStackId() throws Exception {
        String crn = "resourceCrn";
        String instanceName = "instance-name";
        when(cloudStack.getTags()).thenReturn(Map.of(RESOURCE_CRN.key(), crn));
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withParameters(emptyMap())
                .build();
        long privateId = 0;
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .architecture(ArchitectureValues.X86_64)
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE).build())
                .tags(Tag.builder().key(RESOURCE_CRN.key()).value(crn).build())
                .build();
        when(awsStackNameCommonUtil.getInstanceName(any(), any(), any())).thenReturn(instanceName);
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenCallRealMethod();
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResponse response = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance).build())
                .build();
        ArgumentCaptor<DescribeInstancesRequest> requestCaptor = ArgumentCaptor.forClass(DescribeInstancesRequest.class);
        when(amazonEc2Client.describeInstances(requestCaptor.capture())).thenReturn(response);
        when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);

        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        assertEquals(INSTANCE_ID, actual.getFirst().getInstanceId());
        verify(amazonEc2Client, times(0)).startInstances(any());
        DescribeInstancesRequest request = requestCaptor.getValue();
        assertThat(request.filters())
                .hasSize(2)
                .anyMatch(filter -> "tag:Name".equals(filter.name()) && filter.values().contains(instanceName))
                .anyMatch(filter -> ("tag:" + RESOURCE_CRN.key()).equals(filter.name()) && filter.values().contains(crn));
    }

    @Test
    void testBuildWhenInstanceExistAndNotRunningButNotTerminated() throws Exception {
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withParameters(emptyMap())
                .build();
        long privateId = 0;
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .architecture(ArchitectureValues.X86_64)
                .state(InstanceState.builder().code(0).build())
                .build();
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenReturn(Optional.of(instance));
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);

        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        assertEquals(actual.get(0).getInstanceId(), INSTANCE_ID);
        verify(amazonEc2Client, times(1)).startInstances(any());
    }

    @Test
    void testBuildWhenInstanceExistButTerminated() throws Exception {
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .architecture(ArchitectureValues.X86_64)
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_TERMINATED_CODE).build())
                .build();
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("groupName")
                .withParameters(emptyMap())
                .build();
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        InstanceAuthentication authentication = mock(InstanceAuthentication.class);
        Instance terminatedInstance = Instance.builder()
                .instanceId("terminatedInstanceId")
                .architecture(ArchitectureValues.X86_64)
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_TERMINATED_CODE).build())
                .build();
        RunInstancesResponse runInstancesResponse = RunInstancesResponse.builder().instances(instance).build();

        Image image = mock(Image.class);

        long privateId = 0;
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenCallRealMethod();
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResponse response = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance).build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenReturn(response);
        when(amazonEc2Client.createInstance(any())).thenReturn(runInstancesResponse);
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(cloudStack.getImage()).thenReturn(image);
        when(image.getImageName()).thenReturn("img-name");
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(group.getName()).thenReturn("groupName");
        when(awsStackNameCommonUtil.getInstanceName(ac, "groupName", privateId)).thenReturn("stackname");
        when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);

        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        assertEquals(actual.get(0).getInstanceId(), INSTANCE_ID);
        verify(awsTaggingService, times(1)).prepareEc2TagSpecification(anyMap(),
                eq(software.amazon.awssdk.services.ec2.model.ResourceType.INSTANCE));
        verify(awsTaggingService, times(1)).prepareEc2TagSpecification(anyMap(),
                eq(software.amazon.awssdk.services.ec2.model.ResourceType.VOLUME));
    }

    @Test
    void testGetResourceStatusWhenCreationAndInstanceRunning() {
        CloudResource cloudResource = createInstanceResource();
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE).build())
                .build();
        when(awsContext.isBuild()).thenReturn(true);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResponse describeInstanceResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance).build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstanceResponse);
        CloudResourceStatus resourceStatus = underTest.getResourceStatus(awsContext, ac, cloudResource);
        assertEquals(ResourceStatus.CREATED, resourceStatus.getStatus());
    }

    @Test
    void testGetResourceStatusWhenCreationAndInstanceNotRunningAndNotTerminated() {
        CloudResource cloudResource = createInstanceResource();
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .state(InstanceState.builder().code(0).build())
                .build();
        when(awsContext.isBuild()).thenReturn(true);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResponse describeInstanceResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance).build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstanceResponse);
        CloudResourceStatus resourceStatus = underTest.getResourceStatus(awsContext, ac, cloudResource);
        assertEquals(ResourceStatus.IN_PROGRESS, resourceStatus.getStatus());
    }

    @Test
    void testGetResourceStatusWhenCreationAndInstanceTerminated() {
        CloudResource cloudResource = createInstanceResource();
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_TERMINATED_CODE).build())
                .build();
        when(awsContext.isBuild()).thenReturn(true);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResponse describeInstanceResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance).build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstanceResponse);
        CloudResourceStatus resourceStatus = underTest.getResourceStatus(awsContext, ac, cloudResource);
        assertEquals(ResourceStatus.FAILED, resourceStatus.getStatus());
        assertEquals("Instance instanceId creation failed, instance is in terminated state. " +
                "It may have been terminated by an AWS policy or quota issue.", resourceStatus.getStatusReason());
    }

    @Test
    void testGetResourceStatusWhenCreationAndNotFoundException() {
        CloudResource cloudResource = createInstanceResource();
        when(awsContext.isBuild()).thenReturn(true);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        Ec2Exception amazonEC2Exception = (Ec2Exception) Ec2Exception.builder()
                .message("message")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(NOT_FOUND).build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenThrow(amazonEC2Exception);
        Ec2Exception actual = assertThrows(Ec2Exception.class, () -> underTest.getResourceStatus(awsContext, ac, cloudResource));
        assertEquals(NOT_FOUND, actual.awsErrorDetails().errorCode());
    }

    @Test
    void testGetResourceStatusWhenTerminationAndInstanceTerminated() {
        CloudResource cloudResource = createInstanceResource();
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_TERMINATED_CODE).build())
                .build();
        when(awsContext.isBuild()).thenReturn(false);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResponse describeInstanceResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance).build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstanceResponse);
        CloudResourceStatus resourceStatus = underTest.getResourceStatus(awsContext, ac, cloudResource);
        assertEquals(ResourceStatus.DELETED, resourceStatus.getStatus());
    }

    @Test
    void testGetResourceStatusWhenTerminationInstanceNotTerminated() {
        CloudResource cloudResource = createInstanceResource();
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .state(InstanceState.builder().code(0).build())
                .build();
        when(awsContext.isBuild()).thenReturn(false);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResponse describeInstanceResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance).build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstanceResponse);
        CloudResourceStatus resourceStatus = underTest.getResourceStatus(awsContext, ac, cloudResource);
        assertEquals(ResourceStatus.IN_PROGRESS, resourceStatus.getStatus());
    }

    @Test
    void testGetResourceStatusWhenTerminationAndNotFoundException() {
        CloudResource cloudResource = createInstanceResource();
        when(awsContext.isBuild()).thenReturn(false);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        Ec2Exception amazonEC2Exception = (Ec2Exception) Ec2Exception.builder()
                .message("message")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NotFound").build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenThrow(amazonEC2Exception);
        CloudResourceStatus resourceStatus = underTest.getResourceStatus(awsContext, ac, cloudResource);
        assertEquals(ResourceStatus.DELETED, resourceStatus.getStatus());
        assertEquals("AWS resource does not found", resourceStatus.getStatusReason());
    }

    @Test
    void testGetResourceStatusWhenTerminationAndOtherException() {
        CloudResource cloudResource = createInstanceResource();
        when(awsContext.isBuild()).thenReturn(false);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        Ec2Exception amazonEC2Exception = (Ec2Exception) Ec2Exception.builder()
                .message("message")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AnyOther").build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenThrow(amazonEC2Exception);
        Ec2Exception actual = assertThrows(Ec2Exception.class, () -> underTest.getResourceStatus(awsContext, ac, cloudResource));
        assertEquals("AnyOther", actual.awsErrorDetails().errorCode());
    }

    @Test
    void testGetResourceStatusWhenTerminationAndTheInstanceIdIsEmpty() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.DETACHED)
                .withParameters(emptyMap())
                .build();

        when(awsContext.isBuild()).thenReturn(false);
        CloudResourceStatus resourceStatus = underTest.getResourceStatus(awsContext, ac, cloudResource);
        verify(amazonEc2Client, times(0)).describeInstances(any());
        assertEquals(ResourceStatus.DELETED, resourceStatus.getStatus());
    }

    @Test
    void testGetResourceStatusWhenCreationAndTheInstanceIdIsEmpty() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withParameters(emptyMap())
                .build();

        when(awsContext.isBuild()).thenReturn(true);
        CloudResourceStatus resourceStatus = underTest.getResourceStatus(awsContext, ac, cloudResource);
        verify(amazonEc2Client, times(0)).describeInstances(any());
        assertEquals(ResourceStatus.CREATED, resourceStatus.getStatus());
    }

    @Test
    void testBlocksWhenEphemeralNull() {
        BlockDeviceMapping root = BlockDeviceMapping.builder().build();
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(volumeBuilderUtil.getEphemeral(any())).thenReturn(null);
        when(volumeBuilderUtil.getRootVolume(any(AwsInstanceView.class), eq(group), eq(cloudStack), eq(ac))).thenReturn(root);
        Collection<BlockDeviceMapping> actual = underTest.blocks(group, cloudStack, ac);
        assertEquals(1, actual.size());
    }

    @Test
    void testBlocksWhenEphemeralBlockDeviceMappingsListIsEmpty() {
        BlockDeviceMapping root = BlockDeviceMapping.builder().build();
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(volumeBuilderUtil.getEphemeral(any())).thenReturn(new ArrayList<>());
        when(volumeBuilderUtil.getRootVolume(any(AwsInstanceView.class), eq(group), eq(cloudStack), eq(ac))).thenReturn(root);
        Collection<BlockDeviceMapping> actual = underTest.blocks(group, cloudStack, ac);
        assertEquals(1, actual.size());
    }

    @Test
    void testBlocksWhenEphemeralBlockDeviceMappingsListIsNotEmpty() {
        BlockDeviceMapping root = BlockDeviceMapping.builder().build();
        BlockDeviceMapping ephemeralBlockDevice1 = BlockDeviceMapping.builder()
                .deviceName("/dev/xvdb")
                .virtualName("ephemeral0")
                .build();
        BlockDeviceMapping ephemeralBlockDevice2 = BlockDeviceMapping.builder()
                .deviceName("/dev/xvdc")
                .virtualName("ephemeral1")
                .build();
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(volumeBuilderUtil.getEphemeral(any())).thenReturn(List.of(ephemeralBlockDevice1, ephemeralBlockDevice2));
        when(volumeBuilderUtil.getRootVolume(any(AwsInstanceView.class), eq(group), eq(cloudStack), eq(ac))).thenReturn(root);
        Collection<BlockDeviceMapping> actual = underTest.blocks(group, cloudStack, ac);
        assertEquals(3, actual.size());
    }

    @Test
    void testUpdateWithImdsOptional() throws Exception {
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .instanceType(InstanceType.A1_MEDIUM)
                .metadataOptions(InstanceMetadataOptionsResponse.builder().httpTokens(HttpTokensState.REQUIRED).build())
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE).build())
                .build();
        CloudResource cloudResource = setupImdsTest(instance);

        underTest.update(awsContext, cloudResource, cloudInstance, ac, cloudStack, Optional.empty(),
                UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_OPTIONAL);

        verify(amazonEc2Client).modifyInstanceMetadataOptions(any());
    }

    @Test
    void testUpdateWithImdsRequired() throws Exception {
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .instanceType(InstanceType.A1_MEDIUM)
                .metadataOptions(InstanceMetadataOptionsResponse.builder().httpTokens(HttpTokensState.OPTIONAL).build())
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE).build())
                .build();
        CloudResource cloudResource = setupImdsTest(instance);
        Image image = mock(Image.class);
        when(image.getImageId()).thenReturn("image");
        when(image.getPackageVersions()).thenReturn(Map.of(ImagePackageVersion.IMDS_VERSION.getKey(), "v2"));
        when(cloudStack.getImage()).thenReturn(image);

        underTest.update(awsContext, cloudResource, cloudInstance, ac, cloudStack, Optional.empty(),
                UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED);

        verify(amazonEc2Client).modifyInstanceMetadataOptions(any());
    }

    @Test
    void testUpdateWithImdsIfMatching() throws Exception {
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .instanceType(InstanceType.A1_MEDIUM)
                .metadataOptions(InstanceMetadataOptionsResponse.builder().httpTokens(HttpTokensState.OPTIONAL).build())
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE).build())
                .build();
        CloudResource cloudResource = setupImdsTest(instance);

        underTest.update(awsContext, cloudResource, cloudInstance, ac, cloudStack, Optional.empty(),
                UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_OPTIONAL);

        verify(amazonEc2Client, times(0)).modifyInstanceMetadataOptions(any());
    }

    @Test
    void testUpdateWithImdsIfNotSupported() {
        Instance instance = Instance.builder()
                .instanceId(INSTANCE_ID)
                .instanceType(InstanceType.A1_MEDIUM)
                .metadataOptions(InstanceMetadataOptionsResponse.builder().httpTokens(HttpTokensState.OPTIONAL).build())
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE).build())
                .build();
        CloudResource cloudResource = setupImdsTest(instance);
        Image image = mock(Image.class);
        when(image.getImageId()).thenReturn("image");
        when(image.getPackageVersions()).thenReturn(Map.of());
        when(cloudStack.getImage()).thenReturn(image);

        assertThrows(CloudbreakServiceException.class, () -> underTest.update(awsContext, cloudResource, cloudInstance, ac, cloudStack,
                Optional.empty(), UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED));

        verify(amazonEc2Client, times(0)).modifyInstanceMetadataOptions(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testDeleteWhenTheInstanceIdIsNullOrEmptyString(String instanceId) throws Exception {
        CloudResource cloudResource = createInstanceResource(instanceId);

        CloudResource actual = underTest.delete(awsContext, ac, cloudResource);

        assertEquals(cloudResource, actual);
        verifyNoInteractions(awsMethodExecutor);
    }

    @Test
    void testDeleteWhenTheInstanceIsNotReturnedByDescribeInstances() throws Exception {
        CloudResource cloudResource = createInstanceResource();
        DescribeInstancesResponse emptyDescribeResp = DescribeInstancesResponse.builder().build();
        when(awsMethodExecutor.execute(any(), eq(emptyDescribeResp))).thenReturn(emptyDescribeResp);

        CloudResource actual = underTest.delete(awsContext, ac, cloudResource);

        assertNull(actual);
        verify(awsMethodExecutor, times(1)).execute(any(), any());
    }

    @Test
    void testDeleteWhenTheDescribeInstancesCallFails() {
        CloudResource cloudResource = createInstanceResource();
        DescribeInstancesResponse emptyDescribeResp = DescribeInstancesResponse.builder().build();
        when(awsMethodExecutor.execute(any(), eq(emptyDescribeResp))).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> underTest.delete(awsContext, ac, cloudResource));

        verify(awsMethodExecutor, times(1)).execute(any(), any());
    }

    @Test
    void testDeleteWhenTheTerminateSdkCallIsNeeded() throws Exception {
        CloudResource cloudResource = createInstanceResource(INSTANCE_ID);
        DescribeInstancesResponse emptyDescribeResp = DescribeInstancesResponse.builder().build();
        DescribeInstancesResponse describeResp = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(Instance.builder().instanceId(INSTANCE_ID).build())
                        .build())
                .build();
        when(awsMethodExecutor.execute(any(), eq(emptyDescribeResp))).thenReturn(describeResp);
        when(awsMethodExecutor.execute(ArgumentMatchers.<Supplier<TerminateInstancesResponse>>any(), eq(null)))
                .thenReturn(TerminateInstancesResponse.builder().build());

        CloudResource actual = underTest.delete(awsContext, ac, cloudResource);

        assertEquals(cloudResource, actual);
        verify(awsMethodExecutor, times(2)).execute(any(), any());
    }

    @Test
    void testDeleteWhenTheTerminateSdkCallIsNeededAndReturnsWithNull() throws Exception {
        CloudResource cloudResource = createInstanceResource(INSTANCE_ID);
        DescribeInstancesResponse emptyDescribeResp = DescribeInstancesResponse.builder().build();
        DescribeInstancesResponse describeResp = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(Instance.builder().instanceId(INSTANCE_ID).build())
                        .build())
                .build();
        when(awsMethodExecutor.execute(any(), eq(emptyDescribeResp))).thenReturn(describeResp);
        when(awsMethodExecutor.execute(ArgumentMatchers.<Supplier<TerminateInstancesResponse>>any(), eq(null))).thenReturn(null);

        CloudResource actual = underTest.delete(awsContext, ac, cloudResource);

        assertNull(actual);
        verify(awsMethodExecutor, times(2)).execute(any(), any());
    }

    @Test
    void testDeleteWhenTheTerminateSdkCallIsNeededButFails() {
        CloudResource cloudResource = createInstanceResource(INSTANCE_ID);
        DescribeInstancesResponse emptyDescribeResp = DescribeInstancesResponse.builder().build();
        DescribeInstancesResponse describeResp = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(Instance.builder().instanceId(INSTANCE_ID).build())
                        .build())
                .build();
        when(awsMethodExecutor.execute(any(), eq(emptyDescribeResp))).thenReturn(describeResp);
        when(awsMethodExecutor.execute(ArgumentMatchers.<Supplier<TerminateInstancesResponse>>any(), eq(null))).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> underTest.delete(awsContext, ac, cloudResource));

        verify(awsMethodExecutor, times(2)).execute(any(), any());
    }

    private CloudResource setupImdsTest(Instance instance) {
        CloudResource cloudResource = createInstanceResource(INSTANCE_ID);
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenReturn(Optional.of(instance));
        when(cloudInstance.getTemplate()).thenReturn(new InstanceTemplate(InstanceType.A1_MEDIUM.toString(), null, null, List.of(),
                null, null, null, null, null, null));
        lenient().when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        lenient().when(amazonEc2Client.modifyInstanceMetadataOptions(any())).thenReturn(ModifyInstanceMetadataOptionsResponse.builder().build());
        return cloudResource;
    }

    private CloudResource createInstanceResource() {
        return createInstanceResource(INSTANCE_ID);
    }

    private CloudResource createInstanceResource(String instanceId) {
        return CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withParameters(emptyMap())
                .withInstanceId(instanceId)
                .build();
    }
}
