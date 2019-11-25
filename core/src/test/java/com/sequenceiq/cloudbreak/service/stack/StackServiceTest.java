package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationState;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.workspace.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;

@RunWith(MockitoJUnitRunner.class)
public class StackServiceTest {

    private static final Long STACK_ID = 1L;

    private static final Long DATALAKE_RESOURCE_ID = 2L;

    private static final Long WORKSPACE_ID = 1L;

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String OWNER = "1234567";

    private static final String VARIANT_VALUE = "VARIANT_VALUE";

    private static final String STACK_NAME = "name";

    private static final String STACK_DELETE_ACCESS_DENIED = "You cannot modify this Stack";

    private static final String STACK_NOT_FOUND_BY_ID_MESSAGE = "Stack '%d' not found";

    private static final String STACK_NOT_FOUND_BY_NAME_MESSAGE = "Stack '%s' not found";

    private static final String HAS_ATTACHED_CLUSTERS_MESSAGE = "Data Lake has attached Data Hub clusters! "
            + "Please delete Data Hub cluster %s before deleting this Data Lake";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Captor
    public final ArgumentCaptor<String> crnCaptor = ArgumentCaptor.forClass(String.class);

    @InjectMocks
    private StackService underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private Stack stack;

    @Mock
    private User user;

    @Mock
    private Workspace workspace;

    @Mock
    private ServiceProviderConnectorAdapter connector;

    @Mock
    private Variant variant;

    @Mock
    private StackAuthentication stackAuthentication;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private SecurityConfig securityConfig;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private SaltSecurityConfigService saltSecurityConfigService;

    @Mock
    private ImageService imageService;

    @Mock
    private PlatformParameters parameters;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private TransactionService transactionService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private UserService userService;

    @Mock
    private PermissionCheckingUtils permissionCheckingUtils;

    @Mock
    private DatalakeResourcesService datalakeResourcesService;

    @Mock
    private ConverterUtil converterUtil;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Before
    public void setup() {
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getName()).thenReturn(STACK_NAME);
        when(stack.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(user.getUserCrn()).thenReturn(USER_CRN);
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setDatalakeStackId(STACK_ID);
        datalakeResources.setId(DATALAKE_RESOURCE_ID);
        when(threadBasedUserCrnProvider.getAccountId()).thenReturn("something");
        when(datalakeResourcesService.findByDatalakeStackId(anyLong())).thenReturn(Optional.of(datalakeResources));
    }

    @Test
    public void testWhenStackCouldNotFindByItsIdThenExceptionWouldThrown() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.empty());
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(STACK_NOT_FOUND_BY_ID_MESSAGE, STACK_ID));
        underTest.getById(STACK_ID);
    }

    @Test
    public void testDeleteWhenStackCouldNotFindByItsNameForWorkspaceAndNotForcedThenExceptionShouldCome() {
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(STACK_NOT_FOUND_BY_NAME_MESSAGE, STACK_NAME));

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, false, user);

        verify(stackRepository, times(0)).findById(anyLong());
        verify(stackRepository, times(0)).findById(STACK_ID);
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void testDeleteWhenStackCouldNotFindByItsNameForWorkspaceAndForcedThenExceptionShouldCome() {
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(STACK_NOT_FOUND_BY_NAME_MESSAGE, STACK_NAME));

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, true, user);

        verify(stackRepository, times(0)).findById(anyLong());
        verify(stackRepository, times(0)).findById(STACK_ID);
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void testDeleteWhenStackCouldNotFindByItsIdForWorkspaceAndForcedThenExceptionShouldCome() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(STACK_NOT_FOUND_BY_NAME_MESSAGE, STACK_ID));

        underTest.deleteByName(STACK_ID, true, user);
        verify(stackRepository, times(1)).findById(anyLong());
        verify(stackRepository, times(1)).findById(STACK_ID);
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void testDeleteWhenStackCouldNotFindByItsIdForWorkspaceAndNotForcedThenExceptionShouldCome() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(STACK_NOT_FOUND_BY_NAME_MESSAGE, STACK_ID));

        underTest.deleteByName(STACK_ID, true, user);
        verify(stackRepository, times(1)).findById(anyLong());
        verify(stackRepository, times(1)).findById(STACK_ID);
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void testDeleteByIdWhenStackIsAlreadyDeletedThenDeletionWillNotTrigger() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        doNothing().when(permissionCheckingUtils).checkPermissionForUser(any(), any(), anyString());
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stack.isDeleteCompleted()).thenReturn(true);

        underTest.deleteByName(STACK_ID, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteByNameAndWorkspaceIdWhenStackIsAlreadyDeletedThenDeletionWillNotTrigger() {
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        doNothing().when(permissionCheckingUtils).checkPermissionForUser(any(), any(), anyString());
        when(stack.isDeleteCompleted()).thenReturn(true);

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteByIdWhenUserHasNoWriteRightOverStackThenExceptionShouldComeAndTerminationShouldNotBeCalled() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        doThrow(new AccessDeniedException(STACK_DELETE_ACCESS_DENIED)).when(permissionCheckingUtils).checkPermissionForUser(any(), any(), anyString());

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(STACK_DELETE_ACCESS_DENIED);

        underTest.deleteByName(STACK_ID, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteByNameAndWorkspaceIdWhenUserHasNoWriteRightOverStackThenExceptionShouldComeAndTerminationShouldNotBeCalled() {
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        doThrow(new AccessDeniedException(STACK_DELETE_ACCESS_DENIED)).when(permissionCheckingUtils).checkPermissionForUser(any(), any(), anyString());

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(STACK_DELETE_ACCESS_DENIED);

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteByIdWhenUserHasWriteRightOverStackAndStackIsNotDeletedThenTerminationShouldBeCalled() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        doNothing().when(permissionCheckingUtils).checkPermissionForUser(any(), any(), anyString());
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));

        underTest.deleteByName(STACK_ID, true, user);

        verify(flowManager, times(1)).triggerTermination(anyLong(), anyBoolean());
        verify(flowManager, times(1)).triggerTermination(STACK_ID, true);
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteByNameAndWorkspaceIdWhenUserHasWriteRightOverStackAndStackIsNotDeletedThenTerminationShouldBeCalled() {
        doNothing().when(permissionCheckingUtils).checkPermissionForUser(any(), any(), anyString());
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, true, user);

        verify(flowManager, times(1)).triggerTermination(anyLong(), anyBoolean());
        verify(flowManager, times(1)).triggerTermination(STACK_ID, true);
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteWithForceByNameWhenStachHasMultipleAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        StackIdView stack1 = mock(StackIdView.class);
        StackIdView stack2 = mock(StackIdView.class);
        when(stack1.getName()).thenReturn("stack1");
        when(stack2.getName()).thenReturn("stack2");
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack1, stack2));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, String.format("%s, %s", "stack1", "stack2")));
        expectedException.expect(BadRequestException.class);

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteWithoutForceByNameWhenStachHasMultipleAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        StackIdView stack1 = mock(StackIdView.class);
        StackIdView stack2 = mock(StackIdView.class);
        when(stack1.getName()).thenReturn("stack1");
        when(stack2.getName()).thenReturn("stack2");
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack1, stack2));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, String.format("%s, %s", "stack1", "stack2")));
        expectedException.expect(BadRequestException.class);

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, false, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteWithForceByNameWhenStachHasOneAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        StackIdView stack = mock(StackIdView.class);
        when(stack.getName()).thenReturn("stack");
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(this.stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, "stack"));
        expectedException.expect(BadRequestException.class);

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteWithoutForceByNameWhenStachHasOneAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        StackIdView stack = mock(StackIdView.class);
        when(stack.getName()).thenReturn("stack");
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(this.stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, "stack"));
        expectedException.expect(BadRequestException.class);

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, false, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteWithForceByIdWhenStachHasMultipleAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        StackIdView stack1 = mock(StackIdView.class);
        StackIdView stack2 = mock(StackIdView.class);
        when(stack1.getName()).thenReturn("stack1");
        when(stack2.getName()).thenReturn("stack2");
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack1, stack2));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, String.format("%s, %s", "stack1", "stack2")));
        expectedException.expect(BadRequestException.class);

        underTest.deleteByName(STACK_ID, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteWithoutForceByIdWhenStachHasMultipleAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        StackIdView stack1 = mock(StackIdView.class);
        StackIdView stack2 = mock(StackIdView.class);
        when(stack1.getName()).thenReturn("stack1");
        when(stack2.getName()).thenReturn("stack2");
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack1, stack2));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, String.format("%s, %s", "stack1", "stack2")));
        expectedException.expect(BadRequestException.class);

        underTest.deleteByName(STACK_ID, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteWithForceByIdWhenStachHasOneAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        StackIdView stack = mock(StackIdView.class);
        when(stack.getName()).thenReturn("stack");
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(this.stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(this.stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, "stack"));
        expectedException.expect(BadRequestException.class);

        underTest.deleteByName(STACK_ID, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteWithoutForceByIdWhenStachHasOneAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        StackIdView stack = mock(StackIdView.class);
        when(stack.getName()).thenReturn("stack");
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(this.stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(this.stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, "stack"));
        expectedException.expect(BadRequestException.class);

        underTest.deleteByName(STACK_ID, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(any(AuthorizationResource.class), any(ResourceAction.class), anyString());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
    }

    @Test
    public void testDeleteWithoutForceWithDeleteInProgress() {
        Stack stack = mock(Stack.class);
        when(datalakeResourcesService.findByDatalakeStackId(anyLong())).thenReturn(Optional.empty());
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stack.isDeleteInProgress()).thenReturn(Boolean.TRUE);
        when(stack.isDeleteCompleted()).thenReturn(Boolean.FALSE);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getWorkspace()).thenReturn(workspace);

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, false, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
    }

    @Test
    public void testDeleteWithForceWithDeleteInProgressNotForced() {
        Stack stack = mock(Stack.class);
        when(datalakeResourcesService.findByDatalakeStackId(anyLong())).thenReturn(Optional.empty());
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stack.getId()).thenReturn(STACK_ID);
        FlowLog flowLog = new FlowLog();
        flowLog.setVariables("{\"FORCEDTERMINATION\":false}");
        flowLog.setCurrentState(StackTerminationState.PRE_TERMINATION_STATE.name());
        when(flowLogService.findAllByResourceIdOrderByCreatedDesc(STACK_ID)).thenReturn(Collections.singletonList(flowLog));

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, true, user);

        verify(flowManager, times(1)).triggerTermination(anyLong(), eq(true));
    }

    @Test
    public void testDeleteWithForceWithDeleteInProgressForced() {
        Stack stack = mock(Stack.class);
        when(datalakeResourcesService.findByDatalakeStackId(anyLong())).thenReturn(Optional.empty());
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stack.isDeleteInProgress()).thenReturn(Boolean.TRUE);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getWorkspace()).thenReturn(workspace);
        FlowLog flowLog = new FlowLog();
        flowLog.setVariables("{\"FORCEDTERMINATION\":true}");
        flowLog.setCurrentState(StackTerminationState.PRE_TERMINATION_STATE.name());
        when(flowLogService.findAllByResourceIdOrderByCreatedDesc(STACK_ID)).thenReturn(Collections.singletonList(flowLog));

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
    }

    @Test
    public void testDeleteWithForceWithDeleteCompleted() {
        Stack stack = mock(Stack.class);
        when(datalakeResourcesService.findByDatalakeStackId(anyLong())).thenReturn(Optional.empty());
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stack.isDeleteInProgress()).thenReturn(Boolean.FALSE);
        when(stack.isDeleteCompleted()).thenReturn(Boolean.TRUE);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getWorkspace()).thenReturn(workspace);

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(flowLogService, times(0)).findAllByResourceIdOrderByCreatedDesc(STACK_ID);
    }

    @Test
    public void testDeleteWithoutForceWithDeleteCompleted() {
        Stack stack = mock(Stack.class);
        when(datalakeResourcesService.findByDatalakeStackId(anyLong())).thenReturn(Optional.empty());
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stack.isDeleteInProgress()).thenReturn(Boolean.FALSE);
        when(stack.isDeleteCompleted()).thenReturn(Boolean.TRUE);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getWorkspace()).thenReturn(workspace);

        underTest.deleteByName(STACK_NAME, WORKSPACE_ID, false, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean());
        verify(flowLogService, times(0)).findAllByResourceIdOrderByCreatedDesc(STACK_ID);
    }

    @Test
    public void testCreateFailsWithInvalidImageId() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(connector.checkAndGetPlatformVariant(stack)).thenReturn(variant);
        when(variant.value()).thenReturn(VARIANT_VALUE);

        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        when(stackAuthentication.passwordAuthenticationRequired()).thenReturn(false);

        when(stackRepository.save(stack)).thenReturn(stack);

        when(tlsSecurityService.generateSecurityKeys(any(Workspace.class))).thenReturn(securityConfig);
        when(connector.getPlatformParameters(stack)).thenReturn(parameters);

        expectedException.expectCause(org.hamcrest.Matchers.any(CloudbreakImageNotFoundException.class));

        String platformString = "AWS";
        doThrow(new CloudbreakImageNotFoundException("Image not found"))
                .when(imageService)
                .create(eq(stack), eq(platformString), nullable(StatedImage.class));

        try {
            stack = underTest.create(stack, platformString, mock(StatedImage.class), user, workspace);
        } finally {
            verify(stack, times(1)).setPlatformVariant(eq(VARIANT_VALUE));
            verify(securityConfig, times(1)).setStack(stack);
            verify(securityConfigService, times(1)).save(securityConfig);
        }
    }

    @Test
    public void testCreateImageFoundNoStackStatusUpdate() {
        when(connector.checkAndGetPlatformVariant(stack)).thenReturn(variant);
        when(variant.value()).thenReturn(VARIANT_VALUE);

        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        when(stackAuthentication.passwordAuthenticationRequired()).thenReturn(false);

        when(stackRepository.save(stack)).thenReturn(stack);

        when(tlsSecurityService.generateSecurityKeys(any(Workspace.class))).thenReturn(securityConfig);
        when(connector.getPlatformParameters(stack)).thenReturn(parameters);

        try {
            stack = underTest.create(stack, "AWS", mock(StatedImage.class), user, workspace);
        } finally {
            verify(stack, times(1)).setPlatformVariant(eq(VARIANT_VALUE));
            verify(stack).setResourceCrn(crnCaptor.capture());
            String resourceCrn = crnCaptor.getValue();
            assertTrue(resourceCrn.matches("crn:cdp:datahub:us-west-1:something:cluster:.*"));
            verify(securityConfig, times(1)).setStack(stack);
            verify(securityConfigService, times(1)).save(securityConfig);

            verify(stackUpdater, times(0)).updateStackStatus(eq(Long.MAX_VALUE), eq(DetailedStackStatus.PROVISION_FAILED), anyString());
        }
    }

    @Test
    public void testGetAllForAutoscaleWithNullSetFromDb() throws TransactionExecutionException {
        when(transactionService.required(any())).thenAnswer(invocation -> {
            Supplier<AutoscaleStackV4Response> callback = invocation.getArgument(0);
            return callback.get();
        });
        when(stackRepository.findAliveOnesWithAmbari()).thenReturn(null);
        ArgumentCaptor<Set> aliveStackCaptor = ArgumentCaptor.forClass(Set.class);
        when(converterUtil.convertAllAsSet(aliveStackCaptor.capture(), eq(AutoscaleStackV4Response.class))).thenReturn(Set.of());

        Set<AutoscaleStackV4Response> allForAutoscale = underTest.getAllForAutoscale();
        assertNotNull(allForAutoscale);
        assertTrue(allForAutoscale.isEmpty());

        assertNotNull(aliveStackCaptor.getValue());
        assertTrue(aliveStackCaptor.getValue().isEmpty());
    }

    @Test
    public void testGetAllForAutoscaleWithAvailableStack() throws TransactionExecutionException {
        when(transactionService.required(any())).thenAnswer(invocation -> {
            Supplier<AutoscaleStackV4Response> callback = invocation.getArgument(0);
            return callback.get();
        });

        AutoscaleStack stack = mock(AutoscaleStack.class);
        when(stack.getStackStatus()).thenReturn(Status.AVAILABLE);
        when(stackRepository.findAliveOnesWithAmbari()).thenReturn(Set.of(stack));

        ArgumentCaptor<Set> aliveStackCaptor = ArgumentCaptor.forClass(Set.class);
        AutoscaleStackV4Response autoscaleStackResponse = new AutoscaleStackV4Response();
        when(converterUtil.convertAllAsSet(aliveStackCaptor.capture(), eq(AutoscaleStackV4Response.class))).thenReturn(Set.of(autoscaleStackResponse));

        Set<AutoscaleStackV4Response> allForAutoscale = underTest.getAllForAutoscale();
        assertNotNull(allForAutoscale);
        assertEquals(autoscaleStackResponse, allForAutoscale.iterator().next());

        Set<AutoscaleStack> stackSet = aliveStackCaptor.getValue();
        assertNotNull(stackSet);
        assertEquals(stack.getStackStatus(), stackSet.iterator().next().getStackStatus());
    }

    @Test
    public void testGetAllForAutoscaleWithDeleteInProgressStack() throws TransactionExecutionException {
        when(transactionService.required(any())).thenAnswer(invocation -> {
            Supplier<AutoscaleStackV4Response> callback = invocation.getArgument(0);
            return callback.get();
        });

        AutoscaleStack availableStack = mock(AutoscaleStack.class);
        when(availableStack.getStackStatus()).thenReturn(Status.AVAILABLE);

        AutoscaleStack deleteInProgressStack = mock(AutoscaleStack.class);
        when(deleteInProgressStack.getStackStatus()).thenReturn(Status.AVAILABLE);
        when(stackRepository.findAliveOnesWithAmbari()).thenReturn(Set.of(availableStack, deleteInProgressStack));

        ArgumentCaptor<Set> aliveStackCaptor = ArgumentCaptor.forClass(Set.class);
        AutoscaleStackV4Response autoscaleStackResponse = new AutoscaleStackV4Response();
        when(converterUtil.convertAllAsSet(aliveStackCaptor.capture(), eq(AutoscaleStackV4Response.class))).thenReturn(Set.of(autoscaleStackResponse));

        Set<AutoscaleStackV4Response> allForAutoscale = underTest.getAllForAutoscale();
        assertNotNull(allForAutoscale);
        assertEquals(autoscaleStackResponse, allForAutoscale.iterator().next());

        Set<AutoscaleStack> stackSet = aliveStackCaptor.getValue();
        assertNotNull(stackSet);
        assertEquals(availableStack.getStackStatus(), stackSet.iterator().next().getStackStatus());
    }
}
