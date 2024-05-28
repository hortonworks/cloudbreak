package com.sequenceiq.cloudbreak.cloud.aws.common.kms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.template.compute.PreserveResourceException;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AwsKmsKeyResourceBuilderTest {
    private static final Integer PENDING_WINDOWS_IN_DAYS = 7;

    private static final String KEY_ID = "keyId";

    @Mock
    private CommonAwsClient commonAwsClient;

    @Mock
    private AmazonKmsClient amazonKmsClient;

    @Mock
    private AwsContext context;

    @Mock
    private AuthenticatedContext auth;

    @Mock
    private CloudResource resource;

    @Mock
    private CloudInstance cloudInstance;

    @InjectMocks
    private AwsKmsKeyResourceBuilder underTest;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(underTest, "pendingWindowInDays", PENDING_WINDOWS_IN_DAYS);
        when(commonAwsClient.createAWSKMS(auth)).thenReturn(amazonKmsClient);
    }

    @Test
    void testDelete() throws PreserveResourceException {
        when(resource.getReference()).thenReturn(KEY_ID);
        CloudResource cloudResource = underTest.delete(context, auth, resource);
        verify(commonAwsClient).createAWSKMS(auth);
        assertEquals(cloudResource, resource);
        ArgumentCaptor<ScheduleKeyDeletionRequest> contextCaptor = ArgumentCaptor.forClass(ScheduleKeyDeletionRequest.class);
        verify(amazonKmsClient).scheduleKeyDeletion(contextCaptor.capture());
        ScheduleKeyDeletionRequest scheduleKeyDeletionRequest = contextCaptor.getValue();
        assertEquals(PENDING_WINDOWS_IN_DAYS, scheduleKeyDeletionRequest.pendingWindowInDays());
        assertEquals(KEY_ID, scheduleKeyDeletionRequest.keyId());
    }

    @Test
    void testResourceType() {
        assertEquals(ResourceType.AWS_KMS_KEY, underTest.resourceType());
    }

    @Test
    void testOrder() {
        assertEquals(underTest.LOWEST_PRECEDENCE, underTest.order());
    }

    @Test
    void testPlatform() {
        assertEquals(AwsConstants.AWS_PLATFORM, underTest.platform());
    }

    @Test
    void testVariant() {
        assertEquals(AwsConstants.AWS_DEFAULT_VARIANT, underTest.variant());
    }

    @Test
    void testCheckResources() {
        List<CloudResource> cloudResourceList = List.of(resource);
        List<CloudResourceStatus> cloudResourceStatuses = underTest.checkResources(context, auth, cloudResourceList);
        assertEquals(List.of(), cloudResourceStatuses);
    }

    @Test
    void testCheckInstances() {
        List<CloudInstance> cloudInstances = List.of(cloudInstance);
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = underTest.checkInstances(context, auth, cloudInstances);
        assertNull(cloudVmInstanceStatuses);
    }

    @Test
    void testStop() {
        CloudVmInstanceStatus cloudVmInstanceStatus = underTest.stop(context, auth, cloudInstance);
        assertNull(cloudVmInstanceStatus);
    }

    @Test
    void testStart() {
        CloudVmInstanceStatus cloudVmInstanceStatus = underTest.start(context, auth, cloudInstance);
        assertNull(cloudVmInstanceStatus);
    }

    @Test
    void testCreate() {
        List<CloudResource> cloudResources = underTest.create(context, cloudInstance, 0, auth, mock(Group.class), mock(Image.class));
        assertEquals(List.of(), cloudResources);
    }

    @Test
    void testBuild() throws Exception {
        List<CloudResource> cloudResources = underTest.build(context, cloudInstance, 0, auth, mock(Group.class), List.of(), mock(CloudStack.class));
        assertEquals(List.of(), cloudResources);
    }
}
