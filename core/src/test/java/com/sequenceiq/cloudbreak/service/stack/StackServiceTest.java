package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
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
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.projection.StackImageView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
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
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.core.FlowLogService;

@RunWith(MockitoJUnitRunner.class)
public class StackServiceTest {

    private static final Long STACK_ID = 1L;

    private static final Long DATALAKE_RESOURCE_ID = 2L;

    private static final Long WORKSPACE_ID = 1L;

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String VARIANT_VALUE = "VARIANT_VALUE";

    private static final String STACK_NAME = "name";

    private static final String STACK_NOT_FOUND_BY_ID_MESSAGE = "Stack '%d' not found";

    private static final LocalDateTime MOCK_NOW = LocalDateTime.of(1969, 4, 1, 4, 20);

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

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private TargetGroupPersistenceService targetGroupPersistenceService;

    @Before
    public void setUp() {
        underTest.nowSupplier = () -> MOCK_NOW;
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

        expectedException.expectCause(org.hamcrest.Matchers.any(CloudbreakImageNotFoundException.class));

        String platformString = "AWS";
        doThrow(new CloudbreakImageNotFoundException("Image not found"))
                .when(imageService)
                .create(eq(stack), eq(platformString), nullable(StatedImage.class));

        try {
            stack = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                    () -> underTest.create(stack, platformString, mock(StatedImage.class), user, workspace, Optional.empty()));
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

        try {
            stack = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                    () -> underTest.create(stack, "AWS", mock(StatedImage.class), user, workspace, Optional.empty()));
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
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<AutoscaleStackV4Response> callback = invocation.getArgument(0);
            return callback.get();
        });
        when(stackRepository.findAliveOnesWithClusterManager()).thenReturn(null);
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
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<AutoscaleStackV4Response> callback = invocation.getArgument(0);
            return callback.get();
        });

        AutoscaleStack stack = mock(AutoscaleStack.class);
        when(stack.getStackStatus()).thenReturn(Status.AVAILABLE);
        when(stackRepository.findAliveOnesWithClusterManager()).thenReturn(Set.of(stack));

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
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<AutoscaleStackV4Response> callback = invocation.getArgument(0);
            return callback.get();
        });

        AutoscaleStack availableStack = mock(AutoscaleStack.class);
        when(availableStack.getStackStatus()).thenReturn(Status.AVAILABLE);

        AutoscaleStack deleteInProgressStack = mock(AutoscaleStack.class);
        when(deleteInProgressStack.getStackStatus()).thenReturn(Status.AVAILABLE);
        when(stackRepository.findAliveOnesWithClusterManager()).thenReturn(Set.of(availableStack, deleteInProgressStack));

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
    public void testGetImagesOfAliveStacksWithValidImage() {
        long thresholdTimestamp = Timestamp.valueOf(MOCK_NOW).getTime();
        doReturn(List.of(createStackImageView("{\"imageName\":\"mockimage/hdc-hdp--1710161226.tar.gz\"}")))
                .when(stackRepository).findImagesOfAliveStacks(thresholdTimestamp);

        final List<Image> result = underTest.getImagesOfAliveStacks(0);

        assertEquals("mockimage/hdc-hdp--1710161226.tar.gz", result.get(0).getImageName());
    }

    @Test
    public void testGetImagesOfAliveStacksWithInvalidImage() {
        long thresholdTimestamp = Timestamp.valueOf(MOCK_NOW).getTime();
        doReturn(List.of(createStackImageView("[]")))
                .when(stackRepository).findImagesOfAliveStacks(thresholdTimestamp);

        assertThrows("Could not deserialize image from string []", IllegalStateException.class, () -> underTest.getImagesOfAliveStacks(0));
    }

    @Test
    public void testGetImagesOfAliveStacksWithNullThresholdInDays() {
        underTest.getImagesOfAliveStacks(null);

        verify(stackRepository).findImagesOfAliveStacks(Timestamp.valueOf(MOCK_NOW).getTime());
    }

    @Test
    public void testGetImagesOfAliveStacksWithThreshold() {
        final int thresholdInDays = 10;

        underTest.getImagesOfAliveStacks(thresholdInDays);

        verify(stackRepository).findImagesOfAliveStacks(Timestamp.valueOf(MOCK_NOW.minusDays(thresholdInDays)).getTime());
    }

    private StackImageView createStackImageView(String json) {
        final StackImageView stackImageView = mock(StackImageView.class);
        when(stackImageView.getImage()).thenReturn(new Json(json));
        return stackImageView;
    }
}
