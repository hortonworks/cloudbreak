package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.common.type.ComponentType.CDH_PRODUCT_DETAILS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.ws.rs.InternalServerErrorException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.stack.AutoscaleStackToAutoscaleStackResponseJsonConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToStackV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.StackToStackV4RequestConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.projection.AutoscaleStack;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.projection.StackImageView;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.database.DatabaseDefaultVersionProvider;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.environment.credential.OpenSshPublicKeyValidator;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(MockitoExtension.class)
class StackServiceTest {

    private static final Long STACK_ID = 1L;

    private static final Long WORKSPACE_ID = 1L;

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String VARIANT_VALUE = "VARIANT_VALUE";

    private static final String STACK_NAME = "name";

    private static final String STACK_CRN = "stackCrn";

    private static final String STACK_NOT_FOUND_BY_ID_MESSAGE = "Stack '%d' not found.";

    private static final LocalDateTime MOCK_NOW = LocalDateTime.of(1969, 4, 1, 4, 20);

    private static final String PUBLIC_KEY = "ssh-rsa foobar";

    @InjectMocks
    private StackService underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private OpenSshPublicKeyValidator openSshPublicKeyValidator;

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
    private FlowLogService flowLogService;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private TargetGroupPersistenceService targetGroupPersistenceService;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private DatalakeService datalakeService;

    @Mock
    private StackToStackV4ResponseConverter stackToStackV4ResponseConverter;

    @Mock
    private StackToStackV4RequestConverter stackToStackV4RequestConverter;

    @Mock
    private AutoscaleStackToAutoscaleStackResponseJsonConverter autoscaleStackToAutoscaleStackResponseJsonConverter;

    @Mock
    private DatabaseDefaultVersionProvider databaseDefaultVersionProvider;

    @Mock
    private StatedImage statedImage;

    @Mock
    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image image;

    @Mock
    private DatabaseService databaseService;

    @BeforeEach
    void setUp() throws Exception {
        underTest.nowSupplier = () -> MOCK_NOW;

        lenient().when(stack.getStackAuthentication()).thenReturn(stackAuthentication);
        lenient().when(stackAuthentication.passwordAuthenticationRequired()).thenReturn(false);
        lenient().when(stackRepository.save(stack)).thenReturn(stack);
        lenient().when(statedImage.getImage()).thenReturn(image);
        lenient().when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
    }

    @Test
    void testWhenStackCouldNotBeFoundByItsIdThenExceptionWouldBeThrown() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getById(STACK_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(String.format(STACK_NOT_FOUND_BY_ID_MESSAGE, STACK_ID));
    }

    @Test
    void testCreateFailsWithInvalidImageId() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(connector.checkAndGetPlatformVariant(stack)).thenReturn(variant);
        when(variant.value()).thenReturn(VARIANT_VALUE);

        CloudbreakImageNotFoundException imageNotFound = new CloudbreakImageNotFoundException("Image not found");
        doThrow(imageNotFound)
                .when(imageService)
                .create(eq(stack), nullable(StatedImage.class));

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.create(stack, statedImage, user, workspace)))
                .hasCause(imageNotFound);
        verify(stack, times(1)).setPlatformVariant(eq(VARIANT_VALUE));
    }

    @Test
    void testCreateWithRuntime() throws Exception {
        when(connector.checkAndGetPlatformVariant(stack)).thenReturn(variant);
        when(variant.value()).thenReturn(VARIANT_VALUE);
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.MOCK.name());
        String os = "redhat8";
        when(image.getOs()).thenReturn(os);
        ClouderaManagerProduct cdhProduct = new ClouderaManagerProduct();
        String stackVersion = "7.2.16";
        cdhProduct.setVersion(stackVersion);
        Component cdhComponent = new Component(CDH_PRODUCT_DETAILS, CDH_PRODUCT_DETAILS.name(), new Json(cdhProduct), stack);
        when(imageService.create(stack, statedImage)).thenReturn(Set.of(cdhComponent));
        String dbVersion = "10";
        when(stack.getExternalDatabaseEngineVersion()).thenReturn(dbVersion);
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NONE);
        when(stack.getDatabase()).thenReturn(database);
        String calculatedDbVersion = "11";
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeAndOsIfMissing(stackVersion, os, dbVersion, CloudPlatform.MOCK, false))
                .thenReturn(calculatedDbVersion);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.create(stack, statedImage, user, workspace));

        verify(stack).setStackVersion(stackVersion);
        verify(stackRepository, times(2)).save(stack);
        assertEquals(calculatedDbVersion, database.getExternalDatabaseEngineVersion());
    }

    @Test
    void testCreateWithoutRuntime() throws Exception {
        when(connector.checkAndGetPlatformVariant(stack)).thenReturn(variant);
        when(variant.value()).thenReturn(VARIANT_VALUE);
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.MOCK.name());
        String os = "redhat8";
        when(image.getOs()).thenReturn(os);
        when(imageService.create(stack, statedImage)).thenReturn(Set.of());
        String dbVersion = "10";
        when(stack.getExternalDatabaseEngineVersion()).thenReturn(dbVersion);
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NONE);
        when(stack.getDatabase()).thenReturn(database);
        String calculatedDbVersion = "11";
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeAndOsIfMissing(null, os, dbVersion, CloudPlatform.MOCK, false))
                .thenReturn(calculatedDbVersion);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.create(stack, statedImage, user, workspace));

        verify(stack, never()).setStackVersion(any());
        verify(stackRepository, times(2)).save(stack);
        verify(databaseService, never()).save(any());
    }

    @Test
    void testCreateImageFoundNoStackStatusUpdate() {
        when(connector.checkAndGetPlatformVariant(stack)).thenReturn(variant);
        when(variant.value()).thenReturn(VARIANT_VALUE);
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.MOCK.name());
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NONE);
        when(stack.getDatabase()).thenReturn(database);

        try {
            stack = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                    () -> underTest.create(stack, statedImage, user, workspace));
        } finally {
            verify(stack, times(1)).setPlatformVariant(eq(VARIANT_VALUE));
            verify(stackUpdater, times(0)).updateStackStatus(eq(Long.MAX_VALUE), eq(DetailedStackStatus.PROVISION_FAILED), anyString());
        }
    }

    @ParameterizedTest(name = "publicKey={0}")
    @NullSource
    @ValueSource(strings = {""})
    void testCreateNoPublicKey(String publicKey) {
        when(stack.getPlatformVariant()).thenReturn(VARIANT_VALUE);
        when(stackAuthentication.getPublicKey()).thenReturn(publicKey);
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.MOCK.name());
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NONE);
        when(stack.getDatabase()).thenReturn(database);

        stack = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.create(stack, statedImage, user, workspace));

        verify(connector, never()).checkAndGetPlatformVariant(any(Stack.class));
        verify(stack, never()).setPlatformVariant(anyString());
        verify(openSshPublicKeyValidator, never()).validate(anyString(), anyBoolean());
    }

    static Object[][] testCreatePublicKeyDataProvider() {
        return new Object[][]{
                // platformVariant, fipsEnabledExpected
                {VARIANT_VALUE, false},
                {CloudConstants.AWS_NATIVE_GOV, true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCreatePublicKeyDataProvider")
    void testCreatePublicKey(String platformVariant, boolean fipsEnabledExpected) {
        when(stack.getPlatformVariant()).thenReturn(platformVariant);
        when(stackAuthentication.getPublicKey()).thenReturn(PUBLIC_KEY);
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.MOCK.name());
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NONE);
        when(stack.getDatabase()).thenReturn(database);

        stack = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.create(stack, statedImage, user, workspace));

        verify(connector, never()).checkAndGetPlatformVariant(any(Stack.class));
        verify(stack, never()).setPlatformVariant(anyString());
        verify(openSshPublicKeyValidator).validate(PUBLIC_KEY, fipsEnabledExpected);
    }

    @Test
    void testGetAllForAutoscaleWithNullSetFromDb() throws TransactionExecutionException {
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<AutoscaleStackV4Response> callback = invocation.getArgument(0);
            return callback.get();
        });
        when(stackRepository.findAliveOnesWithClusterManager()).thenReturn(Set.of());

        Set<AutoscaleStackV4Response> allForAutoscale = underTest.getAllForAutoscale();
        assertNotNull(allForAutoscale);
        assertTrue(allForAutoscale.isEmpty());
    }

    @Test
    void testGetAllForAutoscaleWithAvailableStack() throws TransactionExecutionException {
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<AutoscaleStackV4Response> callback = invocation.getArgument(0);
            return callback.get();
        });

        AutoscaleStack stack = mock(AutoscaleStack.class);
        when(stack.getStackStatus()).thenReturn(Status.AVAILABLE);
        when(stackRepository.findAliveOnesWithClusterManager()).thenReturn(Set.of(stack));

        ArgumentCaptor<AutoscaleStack> aliveStackCaptor = ArgumentCaptor.forClass(AutoscaleStack.class);
        AutoscaleStackV4Response autoscaleStackResponse = new AutoscaleStackV4Response();
        when(autoscaleStackToAutoscaleStackResponseJsonConverter.convert(aliveStackCaptor.capture())).thenReturn(autoscaleStackResponse);

        Set<AutoscaleStackV4Response> allForAutoscale = underTest.getAllForAutoscale();
        assertNotNull(allForAutoscale);
        assertEquals(autoscaleStackResponse, allForAutoscale.iterator().next());

        AutoscaleStack stackSet = aliveStackCaptor.getValue();
        assertNotNull(stackSet);
        assertEquals(stack.getStackStatus(), stackSet.getStackStatus());
    }

    @Test
    void testGetAllForAutoscaleWithDeleteInProgressStack() throws TransactionExecutionException {
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<AutoscaleStackV4Response> callback = invocation.getArgument(0);
            return callback.get();
        });

        AutoscaleStack availableStack = mock(AutoscaleStack.class);
        when(availableStack.getStackStatus()).thenReturn(Status.AVAILABLE);

        AutoscaleStack deleteInProgressStack = mock(AutoscaleStack.class);
        when(deleteInProgressStack.getStackStatus()).thenReturn(Status.AVAILABLE);
        when(stackRepository.findAliveOnesWithClusterManager()).thenReturn(Set.of(availableStack, deleteInProgressStack));

        ArgumentCaptor<AutoscaleStack> aliveStackCaptor = ArgumentCaptor.forClass(AutoscaleStack.class);
        AutoscaleStackV4Response autoscaleStackResponse = new AutoscaleStackV4Response();
        when(autoscaleStackToAutoscaleStackResponseJsonConverter.convert(aliveStackCaptor.capture())).thenReturn(autoscaleStackResponse);

        Set<AutoscaleStackV4Response> allForAutoscale = underTest.getAllForAutoscale();
        assertNotNull(allForAutoscale);
        assertEquals(autoscaleStackResponse, allForAutoscale.iterator().next());

        AutoscaleStack stackSet = aliveStackCaptor.getValue();
        assertNotNull(stackSet);
        assertEquals(availableStack.getStackStatus(), stackSet.getStackStatus());
    }

    @Test
    void testGetImagesOfAliveStacksWithValidImage() {
        long thresholdTimestamp = Timestamp.valueOf(MOCK_NOW).getTime();
        doReturn(List.of(createStackImageView("{\"imageName\":\"mockimage/hdc-hdp--1710161226.tar.gz\"}")))
                .when(stackRepository).findImagesOfAliveStacks(thresholdTimestamp);

        final List<Image> result = underTest.getImagesOfAliveStacks(0);

        assertEquals("mockimage/hdc-hdp--1710161226.tar.gz", result.get(0).getImageName());
    }

    @Test
    void testGetImagesOfAliveStacksWithInvalidImage() {
        long thresholdTimestamp = Timestamp.valueOf(MOCK_NOW).getTime();
        doReturn(List.of(createStackImageView("[]")))
                .when(stackRepository).findImagesOfAliveStacks(thresholdTimestamp);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.getImagesOfAliveStacks(0));
        assertThat(illegalStateException).hasMessage("Could not deserialize image for stack 0 from Json{value='[]'}");
    }

    @Test
    void testGetImagesOfAliveStacksWithNullThresholdInDays() {
        underTest.getImagesOfAliveStacks(null);

        verify(stackRepository).findImagesOfAliveStacks(Timestamp.valueOf(MOCK_NOW).getTime());
    }

    @Test
    void testGetImagesOfAliveStacksWithThreshold() {
        final int thresholdInDays = 10;

        underTest.getImagesOfAliveStacks(thresholdInDays);

        verify(stackRepository).findImagesOfAliveStacks(Timestamp.valueOf(MOCK_NOW.minusDays(thresholdInDays)).getTime());
    }

    @Test
    void testFindClustersConnectedToDatalakeByDatalakeStackIdWhereNoResultFromDatalakeCrn() throws TransactionExecutionException {
        Set<StackIdView> result = findClusterConnectedToDatalake(Set.of());
        assertEquals(0, result.size());
    }

    @Test
    void testFindClustersConnectedToDatalakeByDatalakeStackIdWhereNoResultFromDatalakeResource() throws TransactionExecutionException {
        StackIdView i = new StackIdViewImpl(1L, "no", "no");
        StackIdView j = new StackIdViewImpl(2L, "nope", "no");
        StackIdView k = new StackIdViewImpl(3L, "none", "no");


        Set<StackIdView> result = findClusterConnectedToDatalake(Set.of(i, j, k));
        assertEquals(3, result.size());
    }

    @Test
    void testGetStatusByCrnsInternalShouldReturnWithStatuses() {
        when(stackRepository.getStatusByCrnsInternal(any(), any())).thenReturn(List.of(new StackClusterStatusView() {
            @Override
            public Long getId() {
                return 1L;
            }

            @Override
            public Status getStatus() {
                return Status.AVAILABLE;
            }

            @Override
            public String getStatusReason() {
                return "ok";
            }

            @Override
            public Status getClusterStatus() {
                return Status.AVAILABLE;
            }

            @Override
            public String getClusterStatusReason() {
                return "ok";
            }

            @Override
            public String getCrn() {
                return "crn1";
            }

            @Override
            public CertExpirationState getCertExpirationState() {
                return CertExpirationState.HOST_CERT_EXPIRING;
            }
        }));

        List<StackClusterStatusView> statuses = underTest.getStatusesByCrnsInternal(List.of("crn1"), StackType.WORKLOAD);
        assertEquals(1, statuses.size());
    }

    @Test
    void testGetComputeMonitoringFlagWhenEnabled() {
        when(componentConfigProviderService.getTelemetry(any())).thenReturn(getTelemetry(true, true));
        Optional<Boolean> computeMonitoringEnabled = underTest.computeMonitoringEnabled(mock(StackDto.class));
        assertTrue(computeMonitoringEnabled.isPresent());
        assertTrue(computeMonitoringEnabled.get());
    }

    @Test
    void testGetComputeMonitoringFlagWhenDisabled() {
        when(componentConfigProviderService.getTelemetry(any())).thenReturn(getTelemetry(true, false));
        Optional<Boolean> computeMonitoringEnabled = underTest.computeMonitoringEnabled(mock(StackDto.class));
        assertTrue(computeMonitoringEnabled.isPresent());
        assertFalse(computeMonitoringEnabled.get());
    }

    @Test
    void testGetComputeMonitoringFlagWhenTelemetryNull() {
        when(componentConfigProviderService.getTelemetry(any())).thenReturn(getTelemetry(false, false));
        Optional<Boolean> computeMonitoringEnabled = underTest.computeMonitoringEnabled(mock(StackDto.class));
        assertFalse(computeMonitoringEnabled.isPresent());
    }

    @Test
    void testGetComputeMonitoringFlagWhenExceptionThrown() {
        when(componentConfigProviderService.getTelemetry(any())).thenThrow(new InternalServerErrorException("something"));
        Optional<Boolean> computeMonitoringEnabled = underTest.computeMonitoringEnabled(mock(StackDto.class));
        assertFalse(computeMonitoringEnabled.isPresent());
    }

    private Telemetry getTelemetry(boolean telemetryPresent, boolean monitoringEnabled) {
        Telemetry telemetry = null;
        if (telemetryPresent) {
            telemetry = new Telemetry();
            if (monitoringEnabled) {
                Monitoring monitoring = new Monitoring();
                monitoring.setRemoteWriteUrl("something");
                telemetry.setMonitoring(monitoring);
            }
        }
        return telemetry;
    }

    private Set<StackIdView> findClusterConnectedToDatalake(
            Set<StackIdView> setOfStackThroughNewDatalakeCrn) throws TransactionExecutionException {
        when(stackRepository.findByDatalakeCrn(anyString())).thenReturn(new HashSet<>(setOfStackThroughNewDatalakeCrn));
        Stack datalakeStack = new Stack();
        datalakeStack.setDatalakeCrn("crnofdl");
        datalakeStack.setResourceCrn("crnofme");
        when(stackRepository.findById(1L)).thenReturn(Optional.of(datalakeStack));
        when(transactionService.required(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<Stack> callback = invocation.getArgument(0);
            return callback.get();
        });
        return underTest.findClustersConnectedToDatalakeByDatalakeStackId(1L);
    }

    @Test
    void testWhenGetByNameOrCrnInWorkspaceIsCalledWithNameThenGetByNameInWorkspaceIsCalled() {
        when(stackRepository.findByNameAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.of(stack));

        underTest.getByNameOrCrnInWorkspace(NameOrCrn.ofName(STACK_NAME), WORKSPACE_ID);

        verify(stackRepository).findByNameAndWorkspaceId(eq(STACK_NAME), eq(WORKSPACE_ID));
        verify(stackRepository, never()).findNotTerminatedByCrnAndWorkspaceId(anyString(), eq(WORKSPACE_ID));
    }

    @Test
    void testWhenGetByNameOrCrnInWorkspaceIsCalledWithCrnThenGetNotTerminatedByCrnInWorkspaceIsCalled() {
        when(stackRepository.findNotTerminatedByCrnAndWorkspaceId(STACK_CRN, WORKSPACE_ID)).thenReturn(Optional.of(stack));

        underTest.getByNameOrCrnInWorkspace(NameOrCrn.ofCrn(STACK_CRN), WORKSPACE_ID);

        verify(stackRepository, never()).findByNameAndWorkspaceId(anyString(), eq(WORKSPACE_ID));
        verify(stackRepository).findNotTerminatedByCrnAndWorkspaceId(eq(STACK_CRN), eq(WORKSPACE_ID));
    }

    private StackImageView createStackImageView(String json) {
        final StackImageView stackImageView = mock(StackImageView.class);
        when(stackImageView.getImage()).thenReturn(new Json(json));
        return stackImageView;
    }

    @Test
    void testFindEnvironmentCrnByStackId() {
        when(stackRepository.findEnvironmentCrnByStackId(WORKSPACE_ID)).thenReturn(Optional.of("ENVIRONMENT_CRN"));
        String environmentCrn = underTest.findEnvironmentCrnByStackId(WORKSPACE_ID);
        assertEquals("ENVIRONMENT_CRN", environmentCrn);
    }

    @Test
    void testFindEnvironmentCrnByStackIdException() {
        when(stackRepository.findEnvironmentCrnByStackId(WORKSPACE_ID)).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(NotFoundException.class, () -> underTest.findEnvironmentCrnByStackId(WORKSPACE_ID));
        assertEquals(exception.getMessage(), "Stack '1' not found.");
    }

    @Test
    void updateExternalDatabaseEngineVersionWhenNoDBEntity() {
        when(stackRepository.findDatabaseIdByStackId(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> underTest.updateExternalDatabaseEngineVersion(1L, "11"));
        verify(databaseService, never()).updateExternalDatabaseEngineVersion(anyLong(), anyString());
    }

    @Test
    void updateExternalDatabaseEngineVersion() {
        when(stackRepository.findDatabaseIdByStackId(1L)).thenReturn(Optional.of(2L));
        underTest.updateExternalDatabaseEngineVersion(1L, "11");
        verify(databaseService, times(1)).updateExternalDatabaseEngineVersion(2L, "11");
    }
}
