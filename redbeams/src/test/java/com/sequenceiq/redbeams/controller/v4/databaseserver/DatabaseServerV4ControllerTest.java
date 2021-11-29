package com.sequenceiq.redbeams.controller.v4.databaseserver;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.CreateDatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.CreateDatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Responses;
import com.sequenceiq.redbeams.converter.stack.AllocateDatabaseServerV4RequestToDBStackConverter;
import com.sequenceiq.redbeams.converter.v4.databaseserver.DBStackToDatabaseServerStatusV4ResponseConverter;
import com.sequenceiq.redbeams.converter.v4.databaseserver.DatabaseServerConfigToDatabaseServerV4ResponseConverter;
import com.sequenceiq.redbeams.converter.v4.databaseserver.DatabaseServerV4RequestToDatabaseServerConfigConverter;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.exception.NotFoundException;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.stack.RedbeamsCreationService;
import com.sequenceiq.redbeams.service.stack.RedbeamsStartService;
import com.sequenceiq.redbeams.service.stack.RedbeamsStopService;
import com.sequenceiq.redbeams.service.stack.RedbeamsTerminationService;

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
    private DatabaseServerConfigToDatabaseServerV4ResponseConverter databaseServerConfigToDatabaseServerV4ResponseConverter;

    @Mock
    private DBStackToDatabaseServerStatusV4ResponseConverter dbStackToDatabaseServerStatusV4ResponseConverter;

    @Mock
    private DatabaseServerV4RequestToDatabaseServerConfigConverter databaseServerV4RequestToDatabaseServerConfigConverter;

    private DatabaseServerConfig server;

    private DatabaseServerConfig server2;

    private DatabaseServerV4Request request;

    private DatabaseServerV4Response serverResponse;

    private DatabaseServerV4Response serverResponse2;

    private AllocateDatabaseServerV4Request allocateRequest;

    private DatabaseServerStatusV4Response allocateResponse;

    private DBStack dbStack;

    @Before
    public void setUp() {
        initMocks(this);

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
        when(service.findByEnvironmentCrnAndClusterCrn(ENVIRONMENT_CRN, CLUSTER_CRN)).thenReturn(Optional.of(server));
        underTest.updateClusterCrn(ENVIRONMENT_CRN, CLUSTER_CRN, CLUSTER_CRN1, USER_CRN);

        when(service.findByEnvironmentCrnAndClusterCrn(ENVIRONMENT_CRN, CLUSTER_CRN)).thenThrow(new NotFoundException("Not found"));
        try {
            underTest.updateClusterCrn(ENVIRONMENT_CRN, CLUSTER_CRN, CLUSTER_CRN1, USER_CRN);
            Assert.fail("NotFoundException should have been thrown");
        } catch (NotFoundException notFoundException) {

        }
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
        when(creationService.launchDatabaseServer(dbStack, CLUSTER_CRN)).thenReturn(savedDBStack);
        when(dbStackToDatabaseServerStatusV4ResponseConverter.convert(savedDBStack))
            .thenReturn(allocateResponse);

        DatabaseServerStatusV4Response response = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->  underTest.create(allocateRequest));

        assertEquals(allocateResponse, response);
        verify(creationService).launchDatabaseServer(dbStack, CLUSTER_CRN);
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
        when(service.create(server, DatabaseServerV4Controller.DEFAULT_WORKSPACE, false)).thenReturn(server);
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
}
