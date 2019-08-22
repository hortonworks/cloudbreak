package com.sequenceiq.redbeams.controller.v4.databaseserver;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.CreateDatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.CreateDatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
// import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTerminationOutcomeV4Response;
// import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Responses;
import com.sequenceiq.redbeams.converter.stack.AllocateDatabaseServerV4RequestToDBStackConverter;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.stack.RedbeamsCreationService;
import com.sequenceiq.redbeams.service.stack.RedbeamsTerminationService;

public class DatabaseServerV4ControllerTest {

    private static final Crn CRN = Crn.builder()
            .setService(Crn.Service.IAM)
            .setAccountId("account")
            .setResourceType(Crn.ResourceType.DATABASE_SERVER)
            .setResource("resource")
            .build();

    private static final String SERVER_CRN = CRN.toString();

    private static final String SERVER_NAME = "myserver";

    private static final String ENVIRONMENT_CRN = "myenv";

    private static final String USER_CRN = "userCrn";

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
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Mock
    private ConverterUtil converterUtil;

    private DatabaseServerConfig server;

    private DatabaseServerConfig server2;

    private DatabaseServerV4Request request;

    private DatabaseServerV4Response response;

    private DatabaseServerV4Response response2;

    private AllocateDatabaseServerV4Request allocateRequest;

    private DatabaseServerStatusV4Response allocateResponse;

    private DatabaseServerTerminationOutcomeV4Response terminateResponse;

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

        response = new DatabaseServerV4Response();
        response.setId(1L);
        response.setName(SERVER_NAME);

        response2 = new DatabaseServerV4Response();
        response2.setId(2L);
        response2.setName("myotherserver");

        allocateRequest = new AllocateDatabaseServerV4Request();

        allocateResponse = new DatabaseServerStatusV4Response();

        terminateResponse = new DatabaseServerTerminationOutcomeV4Response();

        dbStack = new DBStack();
    }

    @Test
    public void testList() {
        Set<DatabaseServerConfig> serverSet = Collections.singleton(server);
        when(service.findAll(DatabaseServerV4Controller.DEFAULT_WORKSPACE, ENVIRONMENT_CRN)).thenReturn(serverSet);
        Set<DatabaseServerV4Response> responseSet = Collections.singleton(response);
        when(converterUtil.convertAllAsSet(serverSet, DatabaseServerV4Response.class)).thenReturn(responseSet);

        DatabaseServerV4Responses responses = underTest.list(ENVIRONMENT_CRN);

        assertEquals(1, responses.getResponses().size());
        assertEquals(response.getId(), responses.getResponses().iterator().next().getId());
    }

    @Test
    public void testGetByName() {
        when(service.getByName(DatabaseServerV4Controller.DEFAULT_WORKSPACE, ENVIRONMENT_CRN, SERVER_NAME)).thenReturn(server);
        when(converterUtil.convert(server, DatabaseServerV4Response.class)).thenReturn(response);

        DatabaseServerV4Response response = underTest.getByName(ENVIRONMENT_CRN, SERVER_NAME);

        assertEquals(1L, response.getId().longValue());
    }

    @Test
    public void testGetByCrn() {
        when(service.getByCrn(SERVER_CRN)).thenReturn(server);
        when(converterUtil.convert(server, DatabaseServerV4Response.class)).thenReturn(response);

        DatabaseServerV4Response response = underTest.getByCrn(SERVER_CRN);

        assertEquals(1L, response.getId().longValue());
    }

    @Test
    public void testCreate() {
        String userCrn = USER_CRN;
        when(threadBasedUserCrnProvider.getUserCrn()).thenReturn(userCrn);
        when(dbStackConverter.convert(allocateRequest, userCrn)).thenReturn(dbStack);
        DBStack savedDBStack = new DBStack();
        when(creationService.launchDatabaseServer(dbStack)).thenReturn(savedDBStack);
        when(converterUtil.convert(savedDBStack, DatabaseServerStatusV4Response.class))
            .thenReturn(allocateResponse);

        DatabaseServerStatusV4Response response = underTest.create(allocateRequest);

        assertEquals(allocateResponse, response);
        verify(creationService).launchDatabaseServer(dbStack);
    }

    @Test
    public void testTerminate() {
        when(terminationService.terminateDatabaseServer(server.getResourceCrn().toString(), true)).thenReturn(dbStack);
        when(converterUtil.convert(dbStack, DatabaseServerTerminationOutcomeV4Response.class))
            .thenReturn(terminateResponse);

        DatabaseServerTerminationOutcomeV4Response response = underTest.terminate(server.getResourceCrn().toString(), true);

        assertEquals(terminateResponse, response);
        verify(terminationService).terminateDatabaseServer(server.getResourceCrn().toString(), true);
    }

    @Test
    public void testRegister() {
        when(converterUtil.convert(request, DatabaseServerConfig.class)).thenReturn(server);
        when(service.create(server, DatabaseServerV4Controller.DEFAULT_WORKSPACE, false)).thenReturn(server);
        when(converterUtil.convert(server, DatabaseServerV4Response.class)).thenReturn(response);

        DatabaseServerV4Response response = underTest.register(request);

        assertEquals(1L, response.getId().longValue());
    }

    @Test
    public void testDelete() {
        when(service.deleteByCrn(SERVER_CRN)).thenReturn(server);
        when(converterUtil.convert(server, DatabaseServerV4Response.class)).thenReturn(response);

        DatabaseServerV4Response response = underTest.deleteByCrn(SERVER_CRN);

        assertEquals(1L, response.getId().longValue());
    }

    @Test
    public void testDeleteMultiple() {
        Set<String> nameSet = new HashSet<>();
        nameSet.add(server.getName());
        nameSet.add(server2.getName());
        Set<DatabaseServerConfig> serverSet = new HashSet<>();
        serverSet.add(server);
        serverSet.add(server2);
        when(service.deleteMultipleByCrn(nameSet)).thenReturn(serverSet);
        Set<DatabaseServerV4Response> responseSet = new HashSet<>();
        responseSet.add(response);
        responseSet.add(response2);
        when(converterUtil.convertAllAsSet(serverSet, DatabaseServerV4Response.class)).thenReturn(responseSet);

        DatabaseServerV4Responses responses = underTest.deleteMultiple(nameSet);

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
}
