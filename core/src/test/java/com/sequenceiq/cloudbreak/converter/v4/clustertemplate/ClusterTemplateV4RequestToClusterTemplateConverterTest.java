package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import static com.sequenceiq.cloudbreak.TestUtil.awsCredential;
import static com.sequenceiq.cloudbreak.TestUtil.blueprint;
import static com.sequenceiq.cloudbreak.TestUtil.cluster;
import static com.sequenceiq.cloudbreak.TestUtil.stack;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.clustertemplate.ClusterTemplateTestUtil;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackV4RequestToStackConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;

class ClusterTemplateV4RequestToClusterTemplateConverterTest {

    private static final Long TEST_WORKSPACE_ID = 1L;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private UserService userService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private DistroXV1RequestToStackV4RequestConverter stackV4RequestConverter;

    private ClusterTemplateV4RequestToClusterTemplateConverter underTest;

    @Mock
    private CloudbreakUser testCloudbreakUser;

    @Mock
    private User testUser;

    @Mock
    private Workspace testWorkspace;

    @Mock
    private StackV4Request testStackV4Request;

    @Mock
    private StackV4RequestToStackConverter stackV4RequestToStackConverter;

    private Stack testStack;

    @BeforeEach
    void setUp() {
        testStack = createStack();

        MockitoAnnotations.openMocks(this);
        underTest = new ClusterTemplateV4RequestToClusterTemplateConverter(
                stackV4RequestToStackConverter,
                workspaceService,
                userService,
                restRequestThreadLocalService,
                credentialClientService,
                stackV4RequestConverter);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(testCloudbreakUser);
        when(userService.getOrCreate(testCloudbreakUser)).thenReturn(testUser);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(TEST_WORKSPACE_ID);
        when(workspaceService.get(TEST_WORKSPACE_ID, testUser)).thenReturn(testWorkspace);
        when(stackV4RequestConverter.convert(any(DistroXV1Request.class))).thenReturn(testStackV4Request);
        when(stackV4RequestToStackConverter.convert(testStackV4Request)).thenReturn(testStack);
    }

    @Test
    void testWhenDistroXTemplateIsNullThenBadRequestExceptionComes() {
        ClusterTemplateV4Request r = ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws();
        r.setDistroXTemplate(null);

        assertThrows(BadRequestException.class, () -> underTest.convert(r));
    }

    @Test
    void testWhenDistroXTemplateEnvironmentNameIsNullThenBadRequestExceptionComes() {
        ClusterTemplateV4Request r = ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws();
        r.getDistroXTemplate().setEnvironmentName(null);

        assertThrows(BadRequestException.class, () -> underTest.convert(r));
    }

    @Test
    void testWhenDistroXTemplateEnvironmentNameIsEmptyThenBadRequestExceptionComes() {
        ClusterTemplateV4Request r = ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws();
        r.getDistroXTemplate().setEnvironmentName("");

        assertThrows(BadRequestException.class, () -> underTest.convert(r));
    }

    @Test
    void testFetchedWorkspaceShouldBeSet() {
        ClusterTemplate result = underTest.convert(ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws());

        assertNotNull(result);
        assertEquals(testWorkspace, result.getWorkspace());

        verify(restRequestThreadLocalService, times(1)).getCloudbreakUser();
        verify(userService, times(1)).getOrCreate(any());
        verify(userService, times(1)).getOrCreate(testCloudbreakUser);
        verify(workspaceService, times(1)).get(any(), any());
        verify(workspaceService, times(1)).get(TEST_WORKSPACE_ID, testUser);
    }

    @Test
    void testGivenStackHasBeenSet() {
        ClusterTemplate result = underTest.convert(ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws());

        assertNotNull(result);
        assertEquals(testStack, result.getStackTemplate());
    }

    @Test
    void testWhenCloudPlatformIsNotNullThenItShouldBeSetFromTheGivenRequest() {
        ClusterTemplateV4Request r = ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws();
        ClusterTemplate result = underTest.convert(r);

        assertNotNull(r);
        assertEquals(r.getCloudPlatform(), result.getCloudPlatform());
    }

    @Test
    void testWhenCloudPlatformIsNullThenItShouldBeSetFromTheCredentialClientService() {
        ClusterTemplateV4Request r = ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws();
        r.setCloudPlatform(null);

        Credential testCredential = Credential.builder().cloudPlatform("AWS").build();
        when(credentialClientService.getByEnvironmentCrn(testStack.getEnvironmentCrn())).thenReturn(testCredential);

        ClusterTemplate result = underTest.convert(r);

        assertNotNull(r);
        assertEquals(testCredential.cloudPlatform(), result.getCloudPlatform());
    }

    @Test
    void testDatalakeRequiredShouldBeSetToOptional() {
        ClusterTemplate result = underTest.convert(ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws());

        assertNotNull(result);
        assertEquals(DatalakeRequired.OPTIONAL, result.getDatalakeRequired());
    }

    @Test
    void testFeatureStateShouldBeSetToReleased() {
        ClusterTemplate result = underTest.convert(ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws());

        assertNotNull(result);
        assertEquals(FeatureState.RELEASED, result.getFeatureState());
    }

    @Test
    void testStatusShouldBeSetToUserManaged() {
        ClusterTemplate result = underTest.convert(ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws());

        assertNotNull(result);
        assertEquals(ResourceStatus.USER_MANAGED, result.getStatus());
    }

    @Test
    void testWhenClusterInStackIsNullThenIllegalStateExceptionComes() {
        Stack invalidStack = createStack();
        invalidStack.setCluster(null);
        when(stackV4RequestToStackConverter.convert(testStackV4Request)).thenReturn(invalidStack);

        assertThrows(IllegalStateException.class, () -> underTest.convert(ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws()));
    }

    @Test
    void testWhenBlueprintInClusterIsNullThenIllegalStateExceptionComes() {
        Stack invalidStack = createStack();
        invalidStack.getCluster().setBlueprint(null);
        when(stackV4RequestToStackConverter.convert(testStackV4Request)).thenReturn(invalidStack);

        assertThrows(IllegalStateException.class, () -> underTest.convert(ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws()));
    }

    @Test
    void testWhenCldrRuntimeVersionIsObtainableThenItShouldBeSet() {
        ClusterTemplate result = underTest.convert(ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws());

        assertNotNull(result);
        assertEquals(testStack.getCluster().getBlueprint().getStackVersion(), result.getClouderaRuntimeVersion());
    }

    @Test
    void testWhenSourceContainsTypeThenThatShouldBeSet() {
        ClusterTemplateV4Request r = ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws();

        ClusterTemplate result = underTest.convert(r);

        assertNotNull(r);
        assertEquals(r.getType(), result.getType());
    }

    @Test
    void testWhenSourceDoesNotContainsTypeThenOtherShouldBeSet() {
        ClusterTemplateV4Request r = ClusterTemplateTestUtil.createClusterTemplateV4RequestForAws();
        r.setType(null);

        ClusterTemplate result = underTest.convert(r);

        assertNotNull(r);
        assertEquals(ClusterTemplateV4Type.OTHER, result.getType());
    }

    private Stack createStack() {
        Stack stack = stack(AVAILABLE, awsCredential());
        Cluster cluster = cluster(blueprint(), stack, 1L);
        stack.setCluster(cluster);
        return stack;
    }

}