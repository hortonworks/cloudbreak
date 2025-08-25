package com.sequenceiq.redbeams.controller.v4.databaseserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.CreateDatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.CreateDatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Responses;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.UpgradeDatabaseServerV4Response;
import com.sequenceiq.redbeams.converter.stack.AllocateDatabaseServerV4RequestToDBStackConverter;
import com.sequenceiq.redbeams.converter.upgrade.UpgradeDatabaseResponseToUpgradeDatabaseServerV4ResponseConverter;
import com.sequenceiq.redbeams.converter.upgrade.UpgradeDatabaseServerV4RequestToUpgradeDatabaseServerRequestConverter;
import com.sequenceiq.redbeams.converter.v4.databaseserver.DBStackToDatabaseServerStatusV4ResponseConverter;
import com.sequenceiq.redbeams.converter.v4.databaseserver.DatabaseServerConfigToDatabaseServerV4ResponseConverter;
import com.sequenceiq.redbeams.converter.v4.databaseserver.DatabaseServerV4RequestToDatabaseServerConfigConverter;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseRequest;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseResponse;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.stack.RedbeamsCreationService;
import com.sequenceiq.redbeams.service.stack.RedbeamsRotateSslService;
import com.sequenceiq.redbeams.service.stack.RedbeamsStartService;
import com.sequenceiq.redbeams.service.stack.RedbeamsStopService;
import com.sequenceiq.redbeams.service.stack.RedbeamsTerminationService;
import com.sequenceiq.redbeams.service.stack.RedbeamsUpgradeService;
import com.sequenceiq.redbeams.service.validation.RedBeamsTagValidator;

@ExtendWith(MockitoExtension.class)
public class DatabaseServerV4ControllerTest {

    private static final Crn CRN = CrnTestUtil.getDatabaseServerCrnBuilder()
            .setAccountId("account")
            .setResource("resource")
            .build();

    private static final String SERVER_CRN = CRN.toString();

    private static final Crn CRN_2 = CrnTestUtil.getDatabaseServerCrnBuilder()
            .setAccountId("account")
            .setResource("resource2")
            .build();

    private static final Crn USERCRN = CrnTestUtil.getDatabaseServerCrnBuilder()
            .setAccountId("account")
            .setResource("user")
            .build();

    private static final String USER_CRN = USERCRN.toString();

    private static final String SERVER_CRN_2 = CRN_2.toString();

    private static final String SERVER_NAME = "myserver";

    private static final String ENVIRONMENT_CRN = "myenv";

    private static final String CLUSTER_CRN = "clusterCrn";

    private static final String CLUSTER_CRN1 = "clusterCrn1";

    @InjectMocks
    private DatabaseServerV4Controller underTest;

    @Mock
    private RedbeamsCreationService creationService;

    @Mock
    private RedbeamsTerminationService terminationService;

    @Mock
    private DatabaseServerConfigService service;

    @Mock
    private AllocateDatabaseServerV4RequestToDBStackConverter dbStackConverter;

    @Mock
    private RedbeamsStartService redbeamsStartService;

    @Mock
    private RedbeamsStopService redbeamsStopService;

    @Mock
    private RedbeamsUpgradeService redbeamsUpgradeService;

    @Mock
    private DatabaseServerConfigToDatabaseServerV4ResponseConverter databaseServerConfigToDatabaseServerV4ResponseConverter;

    @Mock
    private DBStackToDatabaseServerStatusV4ResponseConverter dbStackToDatabaseServerStatusV4ResponseConverter;

    @Mock
    private DatabaseServerV4RequestToDatabaseServerConfigConverter databaseServerV4RequestToDatabaseServerConfigConverter;

    @Mock
    private UpgradeDatabaseServerV4RequestToUpgradeDatabaseServerRequestConverter upgradeDatabaseServerV4RequestConverter;

    @Mock
    private RedBeamsTagValidator redBeamsTagValidator;

    @Mock
    private ValidationResult validationResult;

    @Mock
    private UpgradeDatabaseResponseToUpgradeDatabaseServerV4ResponseConverter upgradeDatabaseServerV4ResponseConverter;

    @Mock
    private RedbeamsRotateSslService redbeamsRotateSslService;

    private DatabaseServerConfig server;

    private DatabaseServerConfig server2;

    private DatabaseServerV4Request request;

    private DatabaseServerV4Response serverResponse;

    private DatabaseServerV4Response serverResponse2;

    private AllocateDatabaseServerV4Request allocateRequest;

    private DatabaseServerStatusV4Response allocateResponse;

    private DBStack dbStack;

    @BeforeEach
    public void setUp() {
        server = new DatabaseServerConfig();
        server.setId(1L);
        server.setName(SERVER_NAME);
        server.setEnvironmentId(ENVIRONMENT_CRN);
        server.setResourceCrn(CRN);

        server2 = new DatabaseServerConfig();
        server2.setId(2L);
        server2.setName("myotherserver");
        server2.setEnvironmentId(ENVIRONMENT_CRN);

        request = new DatabaseServerV4Request();
        request.setName(SERVER_NAME);

        serverResponse = new DatabaseServerV4Response();
        serverResponse.setId(1L);
        serverResponse.setName(SERVER_NAME);

        serverResponse2 = new DatabaseServerV4Response();
        serverResponse2.setId(2L);
        serverResponse2.setName("myotherserver");

        allocateRequest = new AllocateDatabaseServerV4Request();
        allocateRequest.setClusterCrn(CLUSTER_CRN);

        allocateResponse = new DatabaseServerStatusV4Response();

        dbStack = new DBStack();
    }

    @Test
    public void testList() {
        Set<DatabaseServerConfig> serverSet = Collections.singleton(server);
        when(service.findAll(DatabaseServerV4Controller.DEFAULT_WORKSPACE, ENVIRONMENT_CRN)).thenReturn(serverSet);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convert(any())).thenReturn(serverResponse);

        DatabaseServerV4Responses responses = underTest.list(ENVIRONMENT_CRN);

        assertEquals(1, responses.getResponses().size());
        assertEquals(serverResponse.getId().longValue(), responses.getResponses().iterator().next().getId().longValue());
    }

    @Test
    public void testGetByName() {
        when(service.getByName(DatabaseServerV4Controller.DEFAULT_WORKSPACE, ENVIRONMENT_CRN, SERVER_NAME)).thenReturn(server);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convert(any())).thenReturn(serverResponse);

        DatabaseServerV4Response response = underTest.getByName(ENVIRONMENT_CRN, SERVER_NAME);

        assertEquals(serverResponse.getId().longValue(), response.getId().longValue());
    }

    @Test
    public void testUpdateClusterCrn() {
        when(service.listByClusterCrn(ENVIRONMENT_CRN, CLUSTER_CRN)).thenReturn(List.of(server));
        underTest.updateClusterCrn(ENVIRONMENT_CRN, CLUSTER_CRN, CLUSTER_CRN1, USER_CRN);

        when(service.listByClusterCrn(ENVIRONMENT_CRN, CLUSTER_CRN)).thenThrow(new NotFoundException("Not found"));
        assertThrows(NotFoundException.class, () -> underTest.updateClusterCrn(ENVIRONMENT_CRN, CLUSTER_CRN, CLUSTER_CRN1, USER_CRN));
    }

    @Test
    public void testGetByCrn() {
        when(service.getByCrn(SERVER_CRN)).thenReturn(server);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convert(any())).thenReturn(serverResponse);

        DatabaseServerV4Response response = underTest.getByCrn(SERVER_CRN);

        assertEquals(serverResponse.getId().longValue(), response.getId().longValue());
    }

    @Test
    public void testCreate() {
        when(dbStackConverter.convert(allocateRequest, USER_CRN)).thenReturn(dbStack);
        DBStack savedDBStack = new DBStack();
        when(creationService.launchDatabaseServer(dbStack, CLUSTER_CRN, null)).thenReturn(savedDBStack);
        when(dbStackToDatabaseServerStatusV4ResponseConverter.convert(savedDBStack))
            .thenReturn(allocateResponse);
        when(redBeamsTagValidator.validateTags(any(), any())).thenReturn(validationResult);

        DatabaseServerStatusV4Response response = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->  underTest.create(allocateRequest));

        assertEquals(allocateResponse, response);
        verify(creationService).launchDatabaseServer(dbStack, CLUSTER_CRN, null);
    }

    @Test
    public void testCreateNonUnique() {
        when(dbStackConverter.convert(allocateRequest, USER_CRN)).thenReturn(dbStack);
        DBStack savedDBStack = new DBStack();
        when(creationService.launchMultiDatabaseServer(dbStack, CLUSTER_CRN, null)).thenReturn(savedDBStack);
        when(dbStackToDatabaseServerStatusV4ResponseConverter.convert(savedDBStack))
                .thenReturn(allocateResponse);
        when(redBeamsTagValidator.validateTags(any(), any())).thenReturn(validationResult);

        DatabaseServerStatusV4Response response = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->  underTest.createNonUniqueInternal(allocateRequest, USER_CRN));

        assertEquals(allocateResponse, response);
        verify(creationService).launchMultiDatabaseServer(dbStack, CLUSTER_CRN, null);
    }

    @Test
    public void testRelease() {
        when(service.release(SERVER_CRN)).thenReturn(server);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convert(server)).thenReturn(serverResponse);

        DatabaseServerV4Response response = underTest.release(SERVER_CRN);

        assertEquals(serverResponse.getId().longValue(), response.getId().longValue());
    }

    public void testRegister() {
        when(databaseServerV4RequestToDatabaseServerConfigConverter.convert(request)).thenReturn(server);
        when(service.create(server, DatabaseServerV4Controller.DEFAULT_WORKSPACE)).thenReturn(server);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convert(server)).thenReturn(serverResponse);

        DatabaseServerV4Response response = underTest.register(request);

        assertEquals(serverResponse.getId().longValue(), response.getId().longValue());
    }

    @Test
    public void testDeleteByCrn() {
        when(terminationService.terminateByCrn(SERVER_CRN, true)).thenReturn(server);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convert(server)).thenReturn(serverResponse);

        DatabaseServerV4Response response = underTest.deleteByCrn(SERVER_CRN, true);

        assertEquals(serverResponse.getId().longValue(), response.getId().longValue());
    }

    @Test
    public void testDeleteByName() {
        when(terminationService.terminateByName(ENVIRONMENT_CRN, SERVER_NAME, true)).thenReturn(server);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convert(server)).thenReturn(serverResponse);

        DatabaseServerV4Response response = underTest.deleteByName(ENVIRONMENT_CRN, SERVER_NAME, true);

        assertEquals(serverResponse.getId().longValue(), response.getId().longValue());
    }

    @Test
    public void testDeleteMultiple() {
        Set<String> crnSet = new HashSet<>();
        crnSet.add(SERVER_CRN);
        crnSet.add(SERVER_CRN_2);
        Set<DatabaseServerConfig> serverSet = new HashSet<>();
        serverSet.add(server);
        serverSet.add(server2);
        when(terminationService.terminateMultipleByCrn(crnSet, true)).thenReturn(serverSet);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convert(server)).thenReturn(serverResponse);
        when(databaseServerConfigToDatabaseServerV4ResponseConverter.convert(server2)).thenReturn(serverResponse2);

        DatabaseServerV4Responses responses = underTest.deleteMultiple(crnSet, true);

        assertEquals(2, responses.getResponses().size());
    }

    // @Test
    // public void testTestWithIdentifiers() {
    //     when(service.testConnection(SERVER_CRN)).thenReturn("yeahhh");
    //     DatabaseServerTestV4Request testRequest = new DatabaseServerTestV4Request();
    //     testRequest.setExistingDatabaseServerCrn(SERVER_CRN);

    //     DatabaseServerTestV4Response response = underTest.test(testRequest);

    //     assertEquals("yeahhh", response.getResult());
    // }

    // @Test
    // public void testTestWithServer() {
    //     when(converterUtil.convert(request, DatabaseServerConfig.class)).thenReturn(server);
    //     when(service.testConnection(server)).thenReturn("okayyy");
    //     DatabaseServerTestV4Request testRequest = new DatabaseServerTestV4Request();
    //     testRequest.setDatabaseServer(request);

    //     DatabaseServerTestV4Response response = underTest.test(testRequest);

    //     assertEquals("okayyy", response.getResult());
    // }

    @Test
    public void testCreateDatabase() {
        CreateDatabaseV4Request createRequest = new CreateDatabaseV4Request();
        createRequest.setExistingDatabaseServerCrn(SERVER_CRN);
        createRequest.setDatabaseName("mydb");
        createRequest.setType("hive");
        createRequest.setDatabaseDescription("mine not yours");
        when(service.createDatabaseOnServer(SERVER_CRN, "mydb", "hive", Optional.of("mine not yours"))).thenReturn("ok");

        CreateDatabaseV4Response createResponse = underTest.createDatabase(createRequest);

        assertEquals("ok", createResponse.getResult());
    }

    @Test
    public void testStart() {
        underTest.start(SERVER_CRN);

        verify(redbeamsStartService).startDatabaseServer(SERVER_CRN);
    }

    @Test
    public void testStop() {
        underTest.stop(SERVER_CRN);

        verify(redbeamsStopService).stopDatabaseServer(SERVER_CRN);
    }

    @Test
    public void testUpdateToLatestSslCert() {
        underTest.updateToLatestSslCert(SERVER_CRN);

        verify(redbeamsRotateSslService).updateToLatestDatabaseServerSslCert(SERVER_CRN);
    }

    @Test
    public void testUpgrade() {
        UpgradeDatabaseServerV4Request request = new UpgradeDatabaseServerV4Request();
        request.setUpgradeTargetMajorVersion(UpgradeTargetMajorVersion.VERSION_11);
        UpgradeDatabaseServerV4Response response = new UpgradeDatabaseServerV4Response();
        response.setCurrentVersion(MajorVersion.VERSION_10);

        UpgradeDatabaseRequest upgradeDatabaseRequest = getUpgradeDatabaseRequest();
        when(upgradeDatabaseServerV4RequestConverter.convert(request)).thenReturn(upgradeDatabaseRequest);
        when(redbeamsUpgradeService.upgradeDatabaseServer(eq(SERVER_CRN), eq(upgradeDatabaseRequest))).thenReturn(getUpgradeDatabaseResponse());
        when(upgradeDatabaseServerV4ResponseConverter.convert(getUpgradeDatabaseResponse())).thenReturn(response);

        UpgradeDatabaseServerV4Response actualResponse = underTest.upgrade(SERVER_CRN, request);

        ArgumentCaptor<UpgradeDatabaseRequest> upgradeDatabaseRequestArgumentCaptor = ArgumentCaptor.forClass(UpgradeDatabaseRequest.class);
        verify(redbeamsUpgradeService).upgradeDatabaseServer(eq(SERVER_CRN), upgradeDatabaseRequestArgumentCaptor.capture());
        UpgradeDatabaseRequest actualUpgradeDatabaseRequest = upgradeDatabaseRequestArgumentCaptor.getValue();
        assertEquals(actualUpgradeDatabaseRequest.getTargetMajorVersion(), TargetMajorVersion.VERSION_11);
        assertEquals(response.getCurrentVersion(), actualResponse.getCurrentVersion());
    }

    @Test
    public void testValidateUpgrade() throws Exception {
        // GIVEN
        UpgradeDatabaseServerV4Request request = new UpgradeDatabaseServerV4Request();
        request.setUpgradeTargetMajorVersion(UpgradeTargetMajorVersion.VERSION_11);
        UpgradeDatabaseRequest upgradeDatabaseRequest = getUpgradeDatabaseRequest();
        UpgradeDatabaseResponse upgradeDatabaseResponse = new UpgradeDatabaseResponse("reason", MajorVersion.VERSION_11);
        when(upgradeDatabaseServerV4RequestConverter.convert(request)).thenReturn(upgradeDatabaseRequest);
        when(redbeamsUpgradeService.validateUpgradeDatabaseServer(SERVER_CRN, upgradeDatabaseRequest))
                .thenReturn(upgradeDatabaseResponse);
        UpgradeDatabaseServerV4Response response = new UpgradeDatabaseServerV4Response();
        when(upgradeDatabaseServerV4ResponseConverter.convert(upgradeDatabaseResponse)).thenReturn(response);
        // WHEN
        UpgradeDatabaseServerV4Response actualResult = underTest.validateUpgrade(SERVER_CRN, request);
        // THEN
        assertEquals(actualResult, response);
    }

    @Test
    public void testValidateUpgradeThrowsException() {
        // GIVEN
        UpgradeDatabaseServerV4Request request = new UpgradeDatabaseServerV4Request();
        request.setUpgradeTargetMajorVersion(UpgradeTargetMajorVersion.VERSION_11);
        UpgradeDatabaseRequest upgradeDatabaseRequest = getUpgradeDatabaseRequest();
        when(upgradeDatabaseServerV4RequestConverter.convert(request)).thenReturn(upgradeDatabaseRequest);
        RuntimeException exception = new RuntimeException("exception");
        doThrow(exception).when(redbeamsUpgradeService).validateUpgradeDatabaseServer(SERVER_CRN, upgradeDatabaseRequest);
        // WHEN
        Exception actualException = assertThrows(Exception.class, () -> underTest.validateUpgrade(SERVER_CRN, request));
        // THEN
        assertEquals(exception, actualException);
    }

    private UpgradeDatabaseRequest getUpgradeDatabaseRequest() {
        UpgradeDatabaseRequest upgradeDatabaseRequest = new UpgradeDatabaseRequest();
        upgradeDatabaseRequest.setTargetMajorVersion(TargetMajorVersion.VERSION_11);
        return upgradeDatabaseRequest;
    }

    private UpgradeDatabaseResponse getUpgradeDatabaseResponse() {
        UpgradeDatabaseResponse upgradeDatabaseResponse = new UpgradeDatabaseResponse();
        upgradeDatabaseResponse.setCurrentVersion(MajorVersion.VERSION_10);
        return upgradeDatabaseResponse;
    }

    @Test
    public void testMigrateDatabaseToSslByCrnInternal() {
        DBStack savedDBStack = new DBStack();
        when(redbeamsRotateSslService.migrateDatabaseServerSslCertFromNonSslToSsl(SERVER_CRN)).thenReturn(savedDBStack);
        when(dbStackToDatabaseServerStatusV4ResponseConverter.convert(savedDBStack)).thenReturn(allocateResponse);
        DatabaseServerStatusV4Response response = underTest.migrateDatabaseToSslByCrnInternal(SERVER_CRN, USER_CRN);

        verify(redbeamsRotateSslService).migrateDatabaseServerSslCertFromNonSslToSsl(SERVER_CRN);
        assertEquals(allocateResponse, response);
    }

    @Test
    public void testTurnOnSslByCrnInternal() {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "1");
        when(redbeamsRotateSslService.turnOnSsl(SERVER_CRN)).thenReturn(flowIdentifier);
        FlowIdentifier response = underTest.turnOnSslEnforcementOnProviderByCrnInternal(SERVER_CRN);

        verify(redbeamsRotateSslService).turnOnSsl(SERVER_CRN);
        assertEquals("1", response.getPollableId());
    }

}