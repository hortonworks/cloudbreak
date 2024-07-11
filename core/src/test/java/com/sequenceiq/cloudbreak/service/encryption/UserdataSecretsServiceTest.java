package com.sequenceiq.cloudbreak.service.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.SecretConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.UserdataSecretsException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.cloudbreak.cloud.model.secret.CloudSecret;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.CreateCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.DeleteCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.UpdateCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.UpdateCloudSecretResourceAccessRequest;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.util.UserdataSecretsUtil;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackEncryptionService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class UserdataSecretsServiceTest {

    private static final List<String> INSTANCE_GROUPS = List.of("master", "idbroker");

    private static final int INSTANCE_COUNT_PER_GROUP = 2;

    private static final int NODE_COUNT = INSTANCE_GROUPS.size() * INSTANCE_COUNT_PER_GROUP;

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "stackName";

    private static final String STACK_CRN = "stackCrn";

    private static final String ENCRYPTION_KEY = "encryptionKey";

    private static final String LOGGER_INSTANCE_PROFILE_ARN = "arn:aws-us-gov:iam::111111111111:instance-profile/example-logger-instance-profile";

    private static final String IDBROKER_ASSUMER_ARN = "arn:aws-us-gov:iam::111111111111:instance-profile/example-idbroker-assumer";

    private static final String CROSS_ACCOUNT_ROLE_ARN = "arn:aws-us-gov:iam::111111111111:role/example-cred-role";

    private static final CloudContext CLOUD_CONTEXT = CloudContext.Builder.builder()
            .withLocation(Location.location(Region.region("region")))
            .withPlatform("AWS")
            .withVariant("AWS")
            .build();

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential();

    private static final EncryptionKeySource DEFAULT_ENCRYPTION_KEY_SOURCE = EncryptionKeySource.builder()
            .withKeyType(EncryptionKeyType.AWS_MANAGED_KEY)
            .build();

    private static final EncryptionKeySource ENCRYPTION_KEY_SOURCE = EncryptionKeySource.builder()
            .withKeyType(EncryptionKeyType.AWS_KMS_KEY_ARN)
            .withKeyValue(ENCRYPTION_KEY)
            .build();

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private UserDataService userDataService;

    @Mock
    private StackEncryptionService stackEncryptionService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Mock
    private UserdataSecretsUtil userdataSecretsUtil;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private SecretConnector secretConnector;

    @Mock
    private CloudInformationDecoratorProvider cloudInformationDecoratorProvider;

    @Mock
    private CloudInformationDecorator cloudInformationDecorator;

    @InjectMocks
    private UserdataSecretsService underTest;

    @Captor
    private ArgumentCaptor<CreateCloudSecretRequest> createRequestCaptor;

    @Captor
    private ArgumentCaptor<List<String>> resourceReferencesCaptor;

    @Captor
    private ArgumentCaptor<UpdateCloudSecretResourceAccessRequest> policyUpdateRequestCaptor;

    @Captor
    private ArgumentCaptor<UpdateCloudSecretRequest> secretUpdateRequestCaptor;

    @Captor
    private ArgumentCaptor<DeleteCloudSecretRequest> deleteRequestCaptor;

    @Captor
    private ArgumentCaptor<Iterable<InstanceMetaData>> instanceMetaDataCaptor;

    @BeforeEach
    void setup() {
        lenient().when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        lenient().when(cloudConnector.secretConnector()).thenReturn(secretConnector);
        lenient().when(secretConnector.getDefaultEncryptionKeySource()).thenReturn(DEFAULT_ENCRYPTION_KEY_SOURCE);
        lenient().when(cloudInformationDecoratorProvider.getForStack(any())).thenReturn(cloudInformationDecorator);
        lenient().when(cloudInformationDecorator.getUserdataSecretEncryptionKeyType()).thenReturn(EncryptionKeyType.AWS_KMS_KEY_ARN);
    }

    @Test
    void testCreateUserdataSecrets() {
        Stack stack = createStack();
        stubCreateCloudSecret();
        List<Long> privateIds = LongStream.range(0, NODE_COUNT).boxed().toList();

        underTest.createUserdataSecrets(stack, privateIds, CLOUD_CONTEXT, CLOUD_CREDENTIAL);

        verify(secretConnector, times(NODE_COUNT)).createCloudSecret(createRequestCaptor.capture());
        List<CreateCloudSecretRequest> capturedRequests = createRequestCaptor.getAllValues();
        for (int i = 0; i < NODE_COUNT; i++) {
            CreateCloudSecretRequest request = capturedRequests.get(i);
            assertEquals(STACK_NAME + '-' + STACK_CRN + "-userdata-secret-" + i, request.secretName());
            assertEquals("PLACEHOLDER", request.secretValue());
            assertEquals(CLOUD_CONTEXT, request.cloudContext());
            assertEquals(CLOUD_CREDENTIAL, request.cloudCredential());
            assertEquals(DEFAULT_ENCRYPTION_KEY_SOURCE, request.encryptionKeySource().get());
            assertEquals("PLACEHOLDER", request.secretValue());
            assertEquals("Created by CDP. This secret stores the sensitive values needed on the instance during first boot.", request.description());
        }
        verify(resourceService).findByResourceReferencesAndStatusAndTypeAndStack(resourceReferencesCaptor.capture(),
                eq(CommonStatus.CREATED), eq(ResourceType.AWS_SECRETSMANAGER_SECRET), eq(STACK_ID));
        assertEquals(NODE_COUNT, resourceReferencesCaptor.getValue().size());
    }

    private static StackEncryption getStackEncryption() {
        StackEncryption stackEncryption = new StackEncryption();
        stackEncryption.setEncryptionKeyCloudSecretManager(ENCRYPTION_KEY);
        return stackEncryption;
    }

    private static Stack createStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
        stack.setResourceCrn(STACK_CRN);
        stack.setEnvironmentCrn("env_crn");
        stack.setCloudPlatform("AWS");
        stack.setRegion("region");
        return stack;
    }

    private void stubCreateCloudSecret() {
        List<CloudSecret> cloudSecrets = IntStream.range(0, NODE_COUNT)
                .boxed()
                .map(i -> CloudSecret.builder()
                        .withSecretId("secret-" + i)
                        .withSecretName("secret-" + i)
                        .build())
                .toList();
        OngoingStubbing<CloudSecret> ongoingStubbing = when(secretConnector.createCloudSecret(any()));
        for (CloudSecret cloudSecret : cloudSecrets) {
            ongoingStubbing = ongoingStubbing.thenReturn(cloudSecret);
        }
    }

    @Test
    void testAssignSecretsToInstancesWhenListsHaveDifferentSizes() {
        assertThrows(UserdataSecretsException.class,
                () -> underTest.assignSecretsToInstances(new Stack(), Collections.emptyList(), List.of(new InstanceMetaData())),
                "The number of secrets and number of instances do not match.");
    }

    @Test
    void testAssignSecretsToInstances() {
        Stack stack = createStack();
        InstanceMetaData imd1 = new InstanceMetaData();
        imd1.setPrivateId(1L);
        InstanceMetaData imd2 = new InstanceMetaData();
        imd2.setPrivateId(2L);
        Resource r1 = new Resource();
        r1.setResourceName(STACK_NAME + '-' + STACK_CRN + "-userdata-secret-1");
        r1.setId(1L);
        Resource r2 = new Resource();
        r2.setResourceName(STACK_NAME + '-' + STACK_CRN + "-userdata-secret-2");
        r2.setId(2L);

        underTest.assignSecretsToInstances(stack, List.of(r1, r2), List.of(imd1, imd2));

        verify(instanceMetaDataService).saveAll(List.of(imd1, imd2));
        assertEquals(1L, imd1.getUserdataSecretResourceId());
        assertEquals(2L, imd2.getUserdataSecretResourceId());
    }

    @Test
    void testUpdateUserdataSecrets() {
        StackEncryption stackEncryption = getStackEncryption();
        Stack stack = createStack();
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        List<InstanceMetaData> instanceMetaDatas = getInstanceMetaDatas();
        CredentialResponse credentialResponse = getCredentialResponse();
        List<Resource> resources = getResources();
        stubConvert(resources);
        when(userDataService.getUserData(STACK_ID)).thenReturn(getUserdata());
        when(userdataSecretsUtil.getSecretsSection("core-userdata-with-secrets")).thenReturn("core-userdata-only-secrets");
        when(userdataSecretsUtil.getSecretsSection("gateway-userdata-with-secrets")).thenReturn("gateway-userdata-only-secrets");
        when(resourceService.findAllByResourceId(anyList())).thenReturn(resources);
        when(stackEncryptionService.getStackEncryption(STACK_ID)).thenReturn(stackEncryption);
        when(cloudInformationDecorator.getUserdataSecretCryptographicPrincipalsForInstanceGroups(environment, stack))
                .thenReturn(getUserdataSecretCryptographicPrincipalsForInstanceGroups());
        for (int i = 0; i < NODE_COUNT; i++) {
            when(cloudInformationDecorator.getUserdataSecretCryptographicAuthorizedClients(stack, "instance-" + i))
                    .thenReturn(List.of("arn:aws-us-gov:ec2:region:111111111111:instance/instance-" + i));
        }

        underTest.updateUserdataSecrets(stack, instanceMetaDatas, environment, CLOUD_CONTEXT, CLOUD_CREDENTIAL);

        verify(secretConnector, times(NODE_COUNT)).updateCloudSecretResourceAccess(policyUpdateRequestCaptor.capture());
        List<UpdateCloudSecretResourceAccessRequest> updateCloudSecretResourceAccessRequests = policyUpdateRequestCaptor.getAllValues();
        verify(secretConnector, times(NODE_COUNT)).updateCloudSecret(secretUpdateRequestCaptor.capture());
        List<UpdateCloudSecretRequest> updateCloudSecretRequests = secretUpdateRequestCaptor.getAllValues();
        List<String> actualAuthorizedClients = new ArrayList<>();
        for (InstanceMetaData instance : instanceMetaDatas) {
            UpdateCloudSecretResourceAccessRequest updateCloudSecretResourceAccessRequest = updateCloudSecretResourceAccessRequests.stream()
                    .filter(request -> request.cryptographicAuthorizedClients().getFirst().endsWith(instance.getInstanceId()))
                    .findFirst().get();
            assertEquals(CLOUD_CONTEXT, updateCloudSecretResourceAccessRequest.cloudContext());
            assertEquals(CLOUD_CREDENTIAL, updateCloudSecretResourceAccessRequest.cloudCredential());
            assertEquals("secret-" + instance.getUserdataSecretResourceId(), updateCloudSecretResourceAccessRequest.cloudResource().getReference());
            if ("idbroker".equals(instance.getInstanceGroupName())) {
                assertThat(updateCloudSecretResourceAccessRequest.cryptographicPrincipals()).hasSameElementsAs(
                        List.of(IDBROKER_ASSUMER_ARN, CROSS_ACCOUNT_ROLE_ARN));
            } else {
                assertThat(updateCloudSecretResourceAccessRequest.cryptographicPrincipals()).hasSameElementsAs(
                        List.of(LOGGER_INSTANCE_PROFILE_ARN, CROSS_ACCOUNT_ROLE_ARN));
            }
            actualAuthorizedClients.add(updateCloudSecretResourceAccessRequest.cryptographicAuthorizedClients().getFirst());
            UpdateCloudSecretRequest updateCloudSecretRequest = updateCloudSecretRequests.stream()
                    .filter(request -> {
                        Character num = request.cloudResource().getReference().charAt(request.cloudResource().getReference().length() - 1);
                        return num.equals(instance.getInstanceId().charAt(instance.getInstanceId().length() - 1));
                    })
                    .findFirst().get();
            assertEquals(CLOUD_CONTEXT, updateCloudSecretRequest.cloudContext());
            assertEquals(CLOUD_CREDENTIAL, updateCloudSecretRequest.cloudCredential());
            assertEquals("secret-" + instance.getUserdataSecretResourceId(), updateCloudSecretRequest.cloudResource().getReference());
            assertEquals(ENCRYPTION_KEY_SOURCE, updateCloudSecretRequest.newEncryptionKeySource().get());
            if (instance.getInstanceGroupType() == InstanceGroupType.GATEWAY) {
                assertEquals("gateway-userdata-only-secrets", updateCloudSecretRequest.newSecretValue().get());
            } else {
                assertEquals("core-userdata-only-secrets", updateCloudSecretRequest.newSecretValue().get());
            }
        }
        assertThat(actualAuthorizedClients).hasSameElementsAs(IntStream.range(0, NODE_COUNT)
                .boxed()
                .map(i -> "arn:aws-us-gov:ec2:region:111111111111:instance/instance-" + i)
                .toList());
    }

    private static List<InstanceMetaData> getInstanceMetaDatas() {
        List<InstanceMetaData> instanceMetaDatas = new ArrayList<>();
        int index = 0;
        for (String instanceGroupName : INSTANCE_GROUPS) {
            InstanceGroup instanceGroup = new InstanceGroup();
            instanceGroup.setGroupName(instanceGroupName);
            if ("master".equals(instanceGroupName)) {
                instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
            } else {
                instanceGroup.setInstanceGroupType(InstanceGroupType.CORE);
            }
            for (int i = 0; i < INSTANCE_COUNT_PER_GROUP; i++) {
                InstanceMetaData instanceMetaData = new InstanceMetaData();
                instanceMetaData.setInstanceGroup(instanceGroup);
                instanceMetaData.setInstanceId("instance-" + index);
                instanceMetaData.setUserdataSecretResourceId((long) index);
                instanceMetaDatas.add(instanceMetaData);
                index++;
            }
        }
        return instanceMetaDatas;
    }

    private static CredentialResponse getCredentialResponse() {
        AwsCredentialParameters awsCredentialParameters = new AwsCredentialParameters();
        RoleBasedParameters roleBasedParameters = new RoleBasedParameters();
        roleBasedParameters.setRoleArn(CROSS_ACCOUNT_ROLE_ARN);
        awsCredentialParameters.setRoleBased(roleBasedParameters);
        return CredentialResponse.builder()
                .withAws(awsCredentialParameters)
                .withGovCloud(true)
                .build();
    }

    private void stubConvert(List<Resource> resources) {
        for (Resource resource : resources) {
            when(resourceToCloudResourceConverter.convert(resource)).thenReturn(CloudResource.builder()
                    .withType(ResourceType.AWS_SECRETSMANAGER_SECRET)
                    .withReference(resource.getResourceReference())
                    .withName(resource.getResourceName())
                    .build());
        }
    }

    private static Map<InstanceGroupType, String> getUserdata() {
        return Map.of(
                InstanceGroupType.CORE, "core-userdata-with-secrets",
                InstanceGroupType.GATEWAY, "gateway-userdata-with-secrets"
        );
    }

    private static List<Resource> getResources() {
        return LongStream.range(0, NODE_COUNT)
                .boxed()
                .map(i -> {
                    Resource r = new Resource();
                    r.setId(i);
                    r.setResourceReference("secret-" + i);
                    r.setResourceName("secret-" + i);
                    return r;
                })
                .toList();
    }

    private static Map<String, List<String>> getUserdataSecretCryptographicPrincipalsForInstanceGroups() {
        return Map.of(
                "master", List.of(LOGGER_INSTANCE_PROFILE_ARN, CROSS_ACCOUNT_ROLE_ARN),
                "idbroker", List.of(IDBROKER_ASSUMER_ARN, CROSS_ACCOUNT_ROLE_ARN)
        );
    }

    @Test
    void testDeleteUserdataSecretsForStack() {
        List<InstanceMetaData> instanceMetaDatas = getInstanceMetaDatas();
        Stack stack = createStack();
        stack.setInstanceGroups(getInstanceGroups(instanceMetaDatas));
        when(resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_SECRETSMANAGER_SECRET, STACK_ID))
                .thenReturn(getCloudResources());

        underTest.deleteUserdataSecretsForStack(stack, CLOUD_CONTEXT, CLOUD_CREDENTIAL);

        verify(secretConnector, times(NODE_COUNT)).deleteCloudSecret(deleteRequestCaptor.capture());
        List<DeleteCloudSecretRequest> deleteCloudSecretRequests = deleteRequestCaptor.getAllValues();
        for (int i = 0; i < NODE_COUNT; i++) {
            DeleteCloudSecretRequest request = deleteCloudSecretRequests.get(i);
            assertEquals(CLOUD_CONTEXT, request.cloudContext());
            assertEquals(CLOUD_CREDENTIAL, request.cloudCredential());
            assertEquals("secret-" + i, request.cloudResources().getFirst().getName());
        }
        verify(instanceMetaDataService, times(1)).saveAll(instanceMetaDataCaptor.capture());
        assertThat(instanceMetaDataCaptor.getValue()).hasSameElementsAs(instanceMetaDatas);
    }

    private static Set<InstanceGroup> getInstanceGroups(List<InstanceMetaData> instanceMetaDatas) {
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        for (String instanceGroupName : INSTANCE_GROUPS) {
            InstanceGroup instanceGroup = new InstanceGroup();
            instanceGroup.setGroupName(instanceGroupName);
            instanceGroup.setInstanceMetaData(instanceMetaDatas.stream().
                    filter(imd -> instanceGroupName.equals(imd.getInstanceGroupName()))
                    .collect(Collectors.toSet()));
            instanceGroups.add(instanceGroup);
        }
        return instanceGroups;
    }

    private static List<CloudResource> getCloudResources() {
        return IntStream.range(0, NODE_COUNT)
                .boxed()
                .map(i -> CloudResource.builder()
                        .withName("secret-" + i)
                        .withStatus(CommonStatus.CREATED)
                        .withType(ResourceType.AWS_SECRETSMANAGER_SECRET)
                        .build())
                .toList();
    }

    @Test
    void testDeleteUserdataSecretsForInstances() {
        List<InstanceMetaData> goodInstanceMetaDatas = getInstanceMetaDatas();
        List<InstanceMetaData> instanceMetaDatas = new ArrayList<>(goodInstanceMetaDatas);
        InstanceMetaData instanceWithNoSecret = new InstanceMetaData();
        instanceWithNoSecret.setInstanceId("instance-with-no-secret");
        instanceMetaDatas.add(instanceWithNoSecret);
        List<Resource> resources = getResources();
        stubConvert(resources);
        when(resourceService.findAllByResourceId(LongStream.range(0, NODE_COUNT).boxed().toList())).thenReturn(resources);

        underTest.deleteUserdataSecretsForInstances(instanceMetaDatas, CLOUD_CONTEXT, CLOUD_CREDENTIAL);

        verify(secretConnector, times(NODE_COUNT)).deleteCloudSecret(deleteRequestCaptor.capture());
        List<DeleteCloudSecretRequest> deleteCloudSecretRequests = deleteRequestCaptor.getAllValues();
        List<String> secretNamesCaptured = new ArrayList<>();
        List<String> secretNamesExpected = new ArrayList<>();
        for (int i = 0; i < NODE_COUNT; i++) {
            DeleteCloudSecretRequest request = deleteCloudSecretRequests.get(i);
            assertEquals(CLOUD_CONTEXT, request.cloudContext());
            assertEquals(CLOUD_CREDENTIAL, request.cloudCredential());
            secretNamesExpected.add("secret-" + i);
            secretNamesCaptured.add(request.cloudResources().getFirst().getName());
        }
        assertThat(secretNamesCaptured).hasSameElementsAs(secretNamesExpected);
        instanceMetaDatas.forEach(imd -> assertNull(imd.getUserdataSecretResourceId()));
        verify(instanceMetaDataService, times(1)).saveAll(instanceMetaDataCaptor.capture());
        assertThat(instanceMetaDataCaptor.getValue()).hasSameElementsAs(goodInstanceMetaDatas);
    }
}
