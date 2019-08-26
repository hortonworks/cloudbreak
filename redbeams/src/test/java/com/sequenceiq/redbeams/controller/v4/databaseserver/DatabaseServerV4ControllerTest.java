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

    private static final Crn CRN_2 = Crn.builder()
            .setService(Crn.Service.IAM)
            .setAccountId("account")
            .setResourceType(Crn.ResourceType.DATABASE_SERVER)
            .setResource("resource2")
            .build();

    private static final String SERVER_CRN_2 = CRN_2.toString();

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

        allocateResponse = new DatabaseServerStatusV4Response();

        dbStack = new DBStack();
    }

    @Test
    public void testList() {
        Set<DatabaseServerConfig> serverSet = Collections.singleton(server);
        when(service.findAll(DatabaseServerV4Controller.DEFAULT_WORKSPACE, ENVIRONMENT_CRN)).thenReturn(serverSet);
        Set<DatabaseServerV4Response> responseSet = Collections.singleton(serverResponse);
        when(converterUtil.convertAllAsSet(serverSet, DatabaseServerV4Response.class)).thenReturn(responseSet);

        DatabaseServerV4Responses responses = underTest.list(ENVIRONMENT_CRN);

        assertEquals(1, responses.getResponses().size());
        assertEquals(serverResponse.getId().longValue(), responses.getResponses().iterator().next().getId().longValue());
    }

    @Test
    public void testGetByName() {
        when(service.getByName(DatabaseServerV4Controller.DEFAULT_WORKSPACE, ENVIRONMENT_CRN, SERVER_NAME)).thenReturn(server);
        when(converterUtil.convert(server, DatabaseServerV4Response.class)).thenReturn(serverResponse);

        DatabaseServerV4Response response = underTest.getByName(ENVIRONMENT_CRN, SERVER_NAME);

        assertEquals(serverResponse.getId().longValue(), response.getId().longValue());
    }

    @Test
    public void testGetByCrn() {
        when(service.getByCrn(SERVER_CRN)).thenReturn(server);
        when(converterUtil.convert(server, DatabaseServerV4Response.class)).thenReturn(serverResponse);

        DatabaseServerV4Response response = underTest.getByCrn(SERVER_CRN);

        assertEquals(serverResponse.getId().longValue(), response.getId().longValue());
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
    public void testRelease() {
        when(service.release(SERVER_CRN)).thenReturn(server);
        when(converterUtil.convert(server, DatabaseServerV4Response.class)).thenReturn(serverResponse);

        DatabaseServerV4Response response = underTest.release(SERVER_CRN);

        assertEquals(serverResponse.getId().longValue(), response.getId().longValue());
    }

    public void testRegister() {
        when(converterUtil.convert(request, DatabaseServerConfig.class)).thenReturn(server);
        when(service.create(server, DatabaseServerV4Controller.DEFAULT_WORKSPACE, false)).thenReturn(server);
        when(converterUtil.convert(server, DatabaseServerV4Response.class)).thenReturn(serverResponse);

        DatabaseServerV4Response response = underTest.register(request);

        assertEquals(serverResponse.getId().longValue(), response.getId().longValue());
    }

    @Test
    public void testDeleteByCrn() {
        when(terminationService.terminateByCrn(SERVER_CRN, true)).thenReturn(server);
        when(converterUtil.convert(server, DatabaseServerV4Response.class)).thenReturn(serverResponse);

        DatabaseServerV4Response response = underTest.deleteByCrn(SERVER_CRN, true);

        assertEquals(serverResponse.getId().longValue(), response.getId().longValue());
    }

    @Test
    public void testDeleteByName() {
        when(terminationService.terminateByName(ENVIRONMENT_CRN, SERVER_NAME, true)).thenReturn(server);
        when(converterUtil.convert(server, DatabaseServerV4Response.class)).thenReturn(serverResponse);

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
        Set<DatabaseServerV4Response> responseSet = new HashSet<>();
        responseSet.add(serverResponse);
        responseSet.add(serverResponse2);
        when(converterUtil.convertAllAsSet(serverSet, DatabaseServerV4Response.class)).thenReturn(responseSet);

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
}
