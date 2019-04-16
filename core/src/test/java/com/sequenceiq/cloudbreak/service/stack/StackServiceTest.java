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

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.authorization.ResourceAction;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@RunWith(MockitoJUnitRunner.class)
public class StackServiceTest {

    private static final Long STACK_ID = 1L;

    private static final Long DATALAKE_RESOURCE_ID = 2L;

    private static final Long WORKSPACE_ID = 1L;

    private static final String OWNER = "1234567";

    private static final String VARIANT_VALUE = "VARIANT_VALUE";

    private static final String STACK_NAME = "name";

    private static final String STACK_DELETE_ACCESS_DENIED = "You cannot modify this Stack";

    private static final String STACK_NOT_FOUND_BY_ID_MESSAGE = "Stack '%d' not found";

    private static final String STACK_NOT_FOUND_BY_NAME_MESSAGE = "Stack '%s' not found";

    private static final String HAS_ATTACHED_CLUSTERS_MESSAGE = "Stack has attached clusters! Please remove them before try to delete this one. %nThe following"
            + " clusters has to be deleted before terminating the datalake cluster: %s";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

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

    @Before
    public void setup() {
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getName()).thenReturn(STACK_NAME);
        when(stack.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setDatalakeStackId(STACK_ID);
        datalakeResources.setId(DATALAKE_RESOURCE_ID);
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
    public void testDeleteWhenStackCouldNotFindByItsNameForWorkspaceAndForcedAndDeleteDepsThenExceptionShouldCome() {
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(STACK_NOT_FOUND_BY_NAME_MESSAGE, STACK_NAME));

        underTest.delete(STACK_NAME, WORKSPACE_ID, true, true, user);

        verify(stackRepository, times(0)).findById(anyLong());
        verify(stackRepository, times(0)).findById(STACK_ID);
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void testDeleteWhenStackCouldNotFindByItsNameForWorkspaceAndDeleteDepsButNotForcedThenExceptionShouldCome() {
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(STACK_NOT_FOUND_BY_NAME_MESSAGE, STACK_NAME));

        underTest.delete(STACK_NAME, WORKSPACE_ID, false, true, user);

        verify(stackRepository, times(0)).findById(anyLong());
        verify(stackRepository, times(0)).findById(STACK_ID);
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void testDeleteWhenStackCouldNotFindByItsNameForWorkspaceAndForcedButNotDeleteDepsThenExceptionShouldCome() {
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(STACK_NOT_FOUND_BY_NAME_MESSAGE, STACK_NAME));

        underTest.delete(STACK_NAME, WORKSPACE_ID, true, false, user);

        verify(stackRepository, times(0)).findById(anyLong());
        verify(stackRepository, times(0)).findById(STACK_ID);
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void testDeleteWhenStackCouldNotFindByItsNameForWorkspaceAndNotForcedAndNotDeleteDepsThenExceptionShouldCome() {
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(STACK_NOT_FOUND_BY_NAME_MESSAGE, STACK_NAME));

        underTest.delete(STACK_NAME, WORKSPACE_ID, false, false, user);

        verify(stackRepository, times(0)).findById(anyLong());
        verify(stackRepository, times(0)).findById(STACK_ID);
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void testDeleteWhenStackCouldNotFindByItsIdForWorkspaceAndForcedAndDeleteDepsThenExceptionShouldCome() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(STACK_NOT_FOUND_BY_NAME_MESSAGE, STACK_ID));

        underTest.delete(STACK_ID, true, true, user);

        verify(stackRepository, times(1)).findById(anyLong());
        verify(stackRepository, times(1)).findById(STACK_ID);
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void testDeleteWhenStackCouldNotFindByItsIdForWorkspaceAndDeleteDepsButNotForcedThenExceptionShouldCome() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(STACK_NOT_FOUND_BY_NAME_MESSAGE, STACK_ID));

        underTest.delete(STACK_ID, true, true, user);
        verify(stackRepository, times(1)).findById(anyLong());
        verify(stackRepository, times(1)).findById(STACK_ID);
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void testDeleteWhenStackCouldNotFindByItsIdForWorkspaceAndForcedButNotDeleteDepsThenExceptionShouldCome() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(STACK_NOT_FOUND_BY_NAME_MESSAGE, STACK_ID));

        underTest.delete(STACK_ID, true, true, user);
        verify(stackRepository, times(1)).findById(anyLong());
        verify(stackRepository, times(1)).findById(STACK_ID);
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void testDeleteWhenStackCouldNotFindByItsIdForWorkspaceAndNotForcedAndNotDeleteDepsThenExceptionShouldCome() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format(STACK_NOT_FOUND_BY_NAME_MESSAGE, STACK_ID));

        underTest.delete(STACK_ID, true, true, user);
        verify(stackRepository, times(1)).findById(anyLong());
        verify(stackRepository, times(1)).findById(STACK_ID);
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(stackRepository, times(1)).findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID);
    }

    @Test
    public void testDeleteByIdWhenStackIsAlreadyDeletedThenDeletionWillNotTrigger() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        doNothing().when(permissionCheckingUtils).checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stack.isDeleteCompleted()).thenReturn(true);

        underTest.delete(STACK_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteByNameAndWorkspaceIdWhenStackIsAlreadyDeletedThenDeletionWillNotTrigger() {
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        doNothing().when(permissionCheckingUtils).checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        when(stack.isDeleteCompleted()).thenReturn(true);

        underTest.delete(STACK_NAME, WORKSPACE_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteByIdWhenUserHasNoWriteRightOverStackThenExceptionShouldComeAndTerminationShouldNotBeCalled() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        doThrow(new AccessDeniedException(STACK_DELETE_ACCESS_DENIED)).when(permissionCheckingUtils).checkPermissionByWorkspaceIdForUser(WORKSPACE_ID,
                WorkspaceResource.STACK, ResourceAction.WRITE, user);

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(STACK_DELETE_ACCESS_DENIED);

        underTest.delete(STACK_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteByNameAndWorkspaceIdWhenUserHasNoWriteRightOverStackThenExceptionShouldComeAndTerminationShouldNotBeCalled() {
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        doThrow(new AccessDeniedException(STACK_DELETE_ACCESS_DENIED)).when(permissionCheckingUtils)
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(STACK_DELETE_ACCESS_DENIED);

        underTest.delete(STACK_NAME, WORKSPACE_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteByIdWhenUserHasWriteRightOverStackAndStackIsNotDeletedThenTerminationShouldBeCalled() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        doNothing().when(permissionCheckingUtils).checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));

        underTest.delete(STACK_ID, true, true, user);

        verify(flowManager, times(1)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(flowManager, times(1)).triggerTermination(STACK_ID, true, true);
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteByNameAndWorkspaceIdWhenUserHasWriteRightOverStackAndStackIsNotDeletedThenTerminationShouldBeCalled() {
        doNothing().when(permissionCheckingUtils).checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));

        underTest.delete(STACK_NAME, WORKSPACE_ID, true, true, user);

        verify(flowManager, times(1)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(flowManager, times(1)).triggerTermination(STACK_ID, true, true);
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithForceAndDeleteDepsByNameWhenStachHasMultipleAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack1 = mock(Stack.class);
        Stack stack2 = mock(Stack.class);
        when(stack1.getName()).thenReturn("stack1");
        when(stack2.getName()).thenReturn("stack2");
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack1, stack2));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, String.format("%s, %s", "stack1", "stack2")));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_NAME, WORKSPACE_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithForceButWithoutDeleteDepsByNameWhenStachHasMultipleAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack1 = mock(Stack.class);
        Stack stack2 = mock(Stack.class);
        when(stack1.getName()).thenReturn("stack1");
        when(stack2.getName()).thenReturn("stack2");
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack1, stack2));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, String.format("%s, %s", "stack1", "stack2")));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_NAME, WORKSPACE_ID, true, false, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithOutForceButWithDeleteDepsByNameWhenStachHasMultipleAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack1 = mock(Stack.class);
        Stack stack2 = mock(Stack.class);
        when(stack1.getName()).thenReturn("stack1");
        when(stack2.getName()).thenReturn("stack2");
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack1, stack2));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, String.format("%s, %s", "stack1", "stack2")));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_NAME, WORKSPACE_ID, false, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithOutForceAndDeleteDepsByNameWhenStachHasMultipleAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack1 = mock(Stack.class);
        Stack stack2 = mock(Stack.class);
        when(stack1.getName()).thenReturn("stack1");
        when(stack2.getName()).thenReturn("stack2");
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack1, stack2));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, String.format("%s, %s", "stack1", "stack2")));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_NAME, WORKSPACE_ID, false, false, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithForceAndDeleteDepsByNameWhenStachHasOneAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn("stack");
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(this.stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, "stack"));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_NAME, WORKSPACE_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithForceButWithoutDeleteDepsByNameWhenStachHasOneAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn("stack");
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(this.stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, "stack"));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_NAME, WORKSPACE_ID, true, false, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithOutForceButWithDeleteDepsByNameWhenStachHasOneAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn("stack");
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(this.stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, "stack"));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_NAME, WORKSPACE_ID, false, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithOutForceAndDeleteDepsByNameWhenStachHasOneAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn("stack");
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(this.stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, "stack"));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_NAME, WORKSPACE_ID, false, false, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithForceAndDeleteDepsByIdWhenStachHasMultipleAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack1 = mock(Stack.class);
        Stack stack2 = mock(Stack.class);
        when(stack1.getName()).thenReturn("stack1");
        when(stack2.getName()).thenReturn("stack2");
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack1, stack2));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, String.format("%s, %s", "stack1", "stack2")));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithForceButWithoutDeleteDepsByIdWhenStachHasMultipleAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack1 = mock(Stack.class);
        Stack stack2 = mock(Stack.class);
        when(stack1.getName()).thenReturn("stack1");
        when(stack2.getName()).thenReturn("stack2");
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack1, stack2));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, String.format("%s, %s", "stack1", "stack2")));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithOutForceButWithDeleteDepsByIdWhenStachHasMultipleAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack1 = mock(Stack.class);
        Stack stack2 = mock(Stack.class);
        when(stack1.getName()).thenReturn("stack1");
        when(stack2.getName()).thenReturn("stack2");
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack1, stack2));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, String.format("%s, %s", "stack1", "stack2")));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithOutForceAndDeleteDepsByIdWhenStachHasMultipleAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack1 = mock(Stack.class);
        Stack stack2 = mock(Stack.class);
        when(stack1.getName()).thenReturn("stack1");
        when(stack2.getName()).thenReturn("stack2");
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack1, stack2));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, String.format("%s, %s", "stack1", "stack2")));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithForceAndDeleteDepsByIdWhenStachHasOneAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn("stack");
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(this.stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(this.stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, "stack"));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithForceButWithoutDeleteDepsByIdWhenStachHasOneAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn("stack");
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(this.stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(this.stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, "stack"));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithOutForceButWithDeleteDepsByIdWhenStachHasOneAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn("stack");
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(this.stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(this.stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, "stack"));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
    }

    @Test
    public void testDeleteWithOutForceAndDeleteDepsByIdWhenStachHasOneAttachedClustersThenExceptionShouldComeAndNoTerminationProcessShouldStart() {
        Stack stack = mock(Stack.class);
        when(stack.getName()).thenReturn("stack");
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(this.stack));
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.ofNullable(this.stack));
        when(stackRepository.findEphemeralClusters(DATALAKE_RESOURCE_ID)).thenReturn(Set.of(stack));


        expectedException.expectMessage(String.format(HAS_ATTACHED_CLUSTERS_MESSAGE, "stack"));
        expectedException.expect(BadRequestException.class);

        underTest.delete(STACK_ID, true, true, user);

        verify(flowManager, times(0)).triggerTermination(anyLong(), anyBoolean(), anyBoolean());
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(anyLong(), any(WorkspaceResource.class), any(ResourceAction.class), any(User.class));
        verify(permissionCheckingUtils, times(1))
                .checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, WorkspaceResource.STACK, ResourceAction.WRITE, user);
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
                .create(eq(stack), eq(platformString), eq(parameters), nullable(StatedImage.class));

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
