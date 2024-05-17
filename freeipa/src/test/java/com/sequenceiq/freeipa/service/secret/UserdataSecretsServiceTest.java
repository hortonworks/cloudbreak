package com.sequenceiq.freeipa.service.secret;

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
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import com.sequenceiq.cloudbreak.cloud.util.UserdataSecretsUtil;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackEncryption;
import com.sequenceiq.freeipa.service.StackEncryptionService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
class UserdataSecretsServiceTest {

    private static final int INSTANCE_GROUP_COUNT = 2;

    private static final int INSTANCE_COUNT_PER_GROUP = 2;

    private static final int NODE_COUNT = INSTANCE_GROUP_COUNT * INSTANCE_COUNT_PER_GROUP;

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "stackName";

    private static final String STACK_CRN = "stackCrn";

    private static final String ENV_CRN = "env_crn";

    private static final String INSTANCE_PROFILE_ARN = "arn:aws-us-gov:iam::111111111111:instance-profile/example-instance-profile";

    private static final String CROSS_ACCOUNT_ROLE_ARN = "arn:aws-us-gov:iam::111111111111:role/example-cred-role";

    private static final String ENCRYPTION_KEY = "encryptionKey";

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

    private static final String USERDATA = "userdata_with_secrets";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private ResourceService resourceService;

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

    @InjectMocks
    private UserdataSecretsService underTest;

    @Captor
    private ArgumentCaptor<CreateCloudSecretRequest> createCloudSecretRequestCaptor;

    @Captor
    private ArgumentCaptor<List<String>> resourceReferencesCaptor;

    @Captor
    private ArgumentCaptor<UpdateCloudSecretRequest> updateCloudSecretRequestCaptor;

    @Captor
    private ArgumentCaptor<UpdateCloudSecretResourceAccessRequest> updateCloudSecretResourceAccessRequestCaptor;

    @Captor
    private ArgumentCaptor<Iterable<InstanceMetaData>> instanceMetaDataCaptor;

    @Captor
    private ArgumentCaptor<DeleteCloudSecretRequest> deleteCloudSecretRequestCaptor;

    @BeforeEach
    void setup() {
        lenient().when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        lenient().when(cloudConnector.secretConnector()).thenReturn(secretConnector);
        lenient().when(secretConnector.getDefaultEncryptionKeySource()).thenReturn(DEFAULT_ENCRYPTION_KEY_SOURCE);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void testCreateUserdataSecrets(boolean hasEncryptionKey) {
        StackEncryption stackEncryption = getStackEncryption(hasEncryptionKey);
        Stack stack = createStack();
        List<Long> privateIds = LongStream.range(0, NODE_COUNT).boxed().toList();
        stubCreateCloudSecret();
        stubFindByResourceReferencesAndStatusAndTypeAndStack();
        when(stackEncryptionService.getStackEncryption(STACK_ID)).thenReturn(stackEncryption);

        underTest.createUserdataSecrets(stack, privateIds, CLOUD_CONTEXT, CLOUD_CREDENTIAL);

        verify(secretConnector, times(NODE_COUNT)).createCloudSecret(createCloudSecretRequestCaptor.capture());
        List<CreateCloudSecretRequest> capturedRequests = createCloudSecretRequestCaptor.getAllValues();
        verify(resourceService).findByResourceReferencesAndStatusAndTypeAndStack(resourceReferencesCaptor.capture(),
                eq(CommonStatus.CREATED), eq(ResourceType.AWS_SECRETSMANAGER_SECRET), eq(STACK_ID));
        for (int i = 0; i < NODE_COUNT; i++) {
            CreateCloudSecretRequest request = capturedRequests.get(i);
            assertEquals(STACK_NAME + '-' + STACK_CRN + "-userdata-secret-" + i, request.secretName());
            assertEquals("PLACEHOLDER", request.secretValue());
            assertEquals(CLOUD_CONTEXT, request.cloudContext());
            assertEquals(CLOUD_CREDENTIAL, request.cloudCredential());
            if (hasEncryptionKey) {
                assertEquals(ENCRYPTION_KEY_SOURCE, request.encryptionKeySource().get());
            } else {
                assertEquals(DEFAULT_ENCRYPTION_KEY_SOURCE, request.encryptionKeySource().get());
            }
            assertEquals("PLACEHOLDER", request.secretValue());
            assertEquals("Created by CDP. This secret stores the sensitive values needed on the instance during first boot.", request.description());
        }
        assertEquals(NODE_COUNT, resourceReferencesCaptor.getValue().size());
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

    private void stubFindByResourceReferencesAndStatusAndTypeAndStack() {
        List<Resource> resourceRefLists = getResources();
        OngoingStubbing<List<Resource>> ongoingStubbing = when(resourceService.findByResourceReferencesAndStatusAndTypeAndStack(anyList(),
                eq(CommonStatus.CREATED), eq(ResourceType.AWS_SECRETSMANAGER_SECRET), eq(STACK_ID)));
        for (Resource resource : resourceRefLists) {
            ongoingStubbing = ongoingStubbing.thenReturn(List.of(resource));
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

        assertEquals(1L, imd1.getUserdataSecretResourceId());
        assertEquals(2L, imd2.getUserdataSecretResourceId());
    }

    @Test
    void testUpdateUserdataSecrets() {
        StackEncryption stackEncryption = getStackEncryption(true);
        Stack stack = createStack();
        List<InstanceMetaData> instanceMetaDatas = getInstanceMetaDatas();
        CredentialResponse credentialResponse = getCredentialResponse();
        stubConvert();
        when(userdataSecretsUtil.getSecretsSection(USERDATA)).thenReturn("only_secrets");
        when(resourceService.findAllByResourceId(anyList())).thenReturn(getResources());
        when(stackEncryptionService.getStackEncryption(STACK_ID)).thenReturn(stackEncryption);

        underTest.updateUserdataSecrets(stack, instanceMetaDatas, credentialResponse, CLOUD_CONTEXT, CLOUD_CREDENTIAL);

        verify(secretConnector, times(NODE_COUNT)).updateCloudSecretResourceAccess(updateCloudSecretResourceAccessRequestCaptor.capture());
        List<UpdateCloudSecretResourceAccessRequest> updateCloudSecretResourceAccessRequests = updateCloudSecretResourceAccessRequestCaptor.getAllValues();
        verify(secretConnector, times(NODE_COUNT)).updateCloudSecret(updateCloudSecretRequestCaptor.capture());
        List<UpdateCloudSecretRequest> updateCloudSecretRequests = updateCloudSecretRequestCaptor.getAllValues();
        List<String> actualAuthorizedClients = new ArrayList<>();
        for (int i = 0; i < NODE_COUNT; i++) {
            UpdateCloudSecretResourceAccessRequest updateCloudSecretResourceAccessRequest = updateCloudSecretResourceAccessRequests.get(i);
            assertEquals(CLOUD_CONTEXT, updateCloudSecretResourceAccessRequest.cloudContext());
            assertEquals(CLOUD_CREDENTIAL, updateCloudSecretResourceAccessRequest.cloudCredential());
            assertEquals("secret-" + i, updateCloudSecretResourceAccessRequest.cloudResource().getReference());
            assertThat(updateCloudSecretResourceAccessRequest.cryptographicPrincipals()).hasSameElementsAs(
                    List.of(INSTANCE_PROFILE_ARN, CROSS_ACCOUNT_ROLE_ARN));
            actualAuthorizedClients.add(updateCloudSecretResourceAccessRequest.cryptographicAuthorizedClients().getFirst());
            UpdateCloudSecretRequest updateCloudSecretRequest = updateCloudSecretRequests.get(i);
            assertEquals(CLOUD_CONTEXT, updateCloudSecretRequest.cloudContext());
            assertEquals(CLOUD_CREDENTIAL, updateCloudSecretRequest.cloudCredential());
            assertEquals("secret-" + i, updateCloudSecretRequest.cloudResource().getReference());
            assertEquals(ENCRYPTION_KEY_SOURCE, updateCloudSecretRequest.newEncryptionKeySource().get());
            assertEquals("only_secrets", updateCloudSecretRequest.newSecretValue().get());
        }
        assertThat(actualAuthorizedClients).hasSameElementsAs(IntStream.range(0, NODE_COUNT)
                .boxed()
                .map(i -> "arn:aws-us-gov:ec2:region:111111111111:instance/instance-" + i)
                .toList());
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

    @Test
    void testDeleteUserdataSecretsForInstances() {
        List<InstanceMetaData> instanceMetaDatas = getInstanceMetaDatas();
        InstanceMetaData instanceWithNoSecret = new InstanceMetaData();
        instanceWithNoSecret.setInstanceId("instance-with-no-secret");
        instanceMetaDatas.add(instanceWithNoSecret);
        stubConvert();
        when(resourceService.findAllByResourceId(anyList())).thenReturn(getResources());

        underTest.deleteUserdataSecretsForInstances(instanceMetaDatas, CLOUD_CONTEXT, CLOUD_CREDENTIAL);

        verify(secretConnector, times(NODE_COUNT)).deleteCloudSecret(deleteCloudSecretRequestCaptor.capture());
        List<DeleteCloudSecretRequest> deleteCloudSecretRequests = deleteCloudSecretRequestCaptor.getAllValues();
        for (int i = 0; i < NODE_COUNT; i++) {
            DeleteCloudSecretRequest request = deleteCloudSecretRequests.get(i);
            assertEquals(CLOUD_CONTEXT, request.cloudContext());
            assertEquals(CLOUD_CREDENTIAL, request.cloudCredential());
            assertEquals("secret-" + i, request.cloudResources().getFirst().getName());
        }
        instanceMetaDatas.forEach(imd -> assertNull(imd.getUserdataSecretResourceId()));
        verify(instanceMetaDataService, times(1)).saveAll(instanceMetaDataCaptor.capture());
        assertThat(instanceMetaDataCaptor.getValue()).hasSameElementsAs(instanceMetaDatas);
    }

    private static List<Resource> getResources() {
        return IntStream.range(0, NODE_COUNT)
                .boxed()
                .map(i -> {
                    Resource r = new Resource();
                    r.setId((long) i);
                    r.setResourceReference("secret-" + i);
                    r.setResourceName("secret-" + i);
                    return r;
                })
                .toList();
    }

    private void stubConvert() {
        OngoingStubbing<CloudResource> ongoingStubbing = when(resourceToCloudResourceConverter.convert(any()));
        List<CloudResource> cloudResources = IntStream.range(0, NODE_COUNT)
                .boxed()
                .map(i -> CloudResource.builder()
                        .withType(ResourceType.AWS_SECRETSMANAGER_SECRET)
                        .withReference("secret-" + i)
                        .withName("secret-" + i)
                        .build())
                .toList();
        for (CloudResource cloudResource : cloudResources) {
            ongoingStubbing = ongoingStubbing.thenReturn(cloudResource);
        }
    }

    private static Stack createStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
        stack.setResourceCrn(STACK_CRN);
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setCloudPlatform("AWS");
        stack.setRegion("region");
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setUserdata(USERDATA);
        imageEntity.setGatewayUserdata(USERDATA);
        stack.setImage(imageEntity);
        Telemetry telemetry = new Telemetry();
        Logging logging = new Logging();
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        s3CloudStorageV1Parameters.setInstanceProfile(INSTANCE_PROFILE_ARN);
        logging.setS3(s3CloudStorageV1Parameters);
        telemetry.setLogging(logging);
        stack.setTelemetry(telemetry);
        return stack;
    }

    private static StackEncryption getStackEncryption(boolean hasEncryptionKey) {
        StackEncryption stackEncryption = new StackEncryption();
        if (hasEncryptionKey) {
            stackEncryption.setEncryptionKeyCloudSecretManager(ENCRYPTION_KEY);
        }
        return stackEncryption;
    }

    private static List<InstanceMetaData> getInstanceMetaDatas() {
        List<InstanceMetaData> instanceMetaDatas = new ArrayList<>();
        for (int i = 0; i < NODE_COUNT; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId("instance-" + i);
            instanceMetaData.setUserdataSecretResourceId((long) i);
            instanceMetaDatas.add(instanceMetaData);
        }
        return instanceMetaDatas;
    }
}
