package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_START_IGNORED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
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
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
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
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.core.FlowLogService;

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
    private DatalakeResourcesService datalakeResourcesService;

    @Mock
    private ConverterUtil converterUtil;

    @Mock
    private FlowLogService flowLogService;

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
    public void testCreateFailsWithInvalidImageId() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(connector.checkAndGetPlatformVariant(stack)).thenReturn(variant);
        when(variant.value()).thenReturn(VARIANT_VALUE);

        when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        when(stackAuthentication.passwordAuthenticationRequired()).thenReturn(false);

        when(stackRepository.save(stack)).thenReturn(stack);

        when(tlsSecurityService.generateSecurityKeys(any(Workspace.class))).thenReturn(securityConfig);

        expectedException.expectCause(org.hamcrest.Matchers.any(CloudbreakImageNotFoundException.class));

        String platformString = "AWS";
        doThrow(new CloudbreakImageNotFoundException("Image not found"))
                .when(imageService)
                .create(eq(stack), eq(platformString), nullable(StatedImage.class));

        try {
            stack = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                    () -> underTest.create(stack, platformString, mock(StatedImage.class), user, workspace));
        } finally {
            verify(stack, times(1)).setPlatformVariant(eq(VARIANT_VALUE));
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

        try {
            stack = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                    () -> underTest.create(stack, "AWS", mock(StatedImage.class), user, workspace));
        } finally {
            verify(stack, times(1)).setPlatformVariant(eq(VARIANT_VALUE));
            verify(stack).setResourceCrn(crnCaptor.capture());
            String resourceCrn = crnCaptor.getValue();
            assertTrue(resourceCrn.matches("crn:cdp:datahub:us-west-1:1234:cluster:.*"));

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

    @Test
    public void testStartWhenStackAvailable() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));

        underTest.start(stack, null, false, new User());

        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), STACK_START_IGNORED);
    }

    @Test
    public void testStartWhenStackStopped() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.STOPPED));

        underTest.start(stack, null, false, new User());

        verify(flowManager, times(1)).triggerStackStart(stack.getId());
    }

    @Test
    public void testStartWhenStackStartFailed() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.START_FAILED));

        underTest.start(stack, null, false, new User());

        verify(flowManager, times(1)).triggerStackStart(stack.getId());
    }

    @Test
    public void testStartWhenStackStopFailed() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.STOP_FAILED));

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("");
        underTest.start(stack, null, false, new User());
    }

    @Test
    public void testStartWhenClusterStopFailed() {
        Stack stack = new Stack();
        stack.setId(9876L);
        stack.setStackStatus(new StackStatus(stack, AVAILABLE));
        Cluster cluster = new Cluster();
        cluster.setStatus(Status.STOPPED);
        stack.setCluster(cluster);
        underTest.start(stack, cluster, false, new User());
        verify(flowManager, times(1)).triggerStackStart(stack.getId());
    }
}
