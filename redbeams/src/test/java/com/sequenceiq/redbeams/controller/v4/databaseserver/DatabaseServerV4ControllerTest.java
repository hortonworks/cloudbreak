package com.sequenceiq.redbeams.controller.v4.databaseserver;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.base.DatabaseServerV4Identifiers;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Responses;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DatabaseServerV4ControllerTest {

    @InjectMocks
    private DatabaseServerV4Controller underTest;

    @Mock
    private DatabaseServerConfigService service;

    @Mock
    private ConverterUtil converterUtil;

    private DatabaseServerConfig server;

    private DatabaseServerConfig server2;

    private DatabaseServerV4Request request;

    private DatabaseServerV4Response response;

    private DatabaseServerV4Response response2;

    @Before
    public void setUp() {
        initMocks(this);

        server = new DatabaseServerConfig();
        server.setId(1L);
        server.setName("myserver");
        server.setEnvironmentId("myenv");

        server2 = new DatabaseServerConfig();
        server2.setId(2L);
        server2.setName("myotherserver");
        server2.setEnvironmentId("myenv");

        request = new DatabaseServerV4Request();
        request.setName("myserver");

        response = new DatabaseServerV4Response();
        response.setId(1L);
        response.setName("myserver");

        response2 = new DatabaseServerV4Response();
        response2.setId(2L);
        response2.setName("myotherserver");
    }

    @Test
    public void testList() {
        Set<DatabaseServerConfig> serverSet = Collections.singleton(server);
        when(service.findAll(DatabaseServerV4Controller.DEFAULT_WORKSPACE, "myenv", Boolean.TRUE)).thenReturn(serverSet);
        Set<DatabaseServerV4Response> responseSet = Collections.singleton(response);
        when(converterUtil.convertAllAsSet(serverSet, DatabaseServerV4Response.class)).thenReturn(responseSet);

        DatabaseServerV4Responses responses = underTest.list("myenv", Boolean.TRUE);

        assertEquals(1, responses.getResponses().size());
        assertEquals(response.getId(), responses.getResponses().iterator().next().getId());
    }

    @Test
    public void testGet() {
        when(service.getByName(DatabaseServerV4Controller.DEFAULT_WORKSPACE, "myenv", "myserver")).thenReturn(server);
        when(converterUtil.convert(server, DatabaseServerV4Response.class)).thenReturn(response);

        DatabaseServerV4Response response = underTest.get("myenv", "myserver");

        assertEquals(1L, response.getId().longValue());
    }

    @Test
    public void testRegister() {
        when(converterUtil.convert(request, DatabaseServerConfig.class)).thenReturn(server);
        when(service.create(server, DatabaseServerV4Controller.DEFAULT_WORKSPACE)).thenReturn(server);
        when(converterUtil.convert(server, DatabaseServerV4Response.class)).thenReturn(response);

        DatabaseServerV4Response response = underTest.register(request);

        assertEquals(1L, response.getId().longValue());
    }

    @Test
    public void testDelete() {
        when(service.deleteByName(DatabaseServerV4Controller.DEFAULT_WORKSPACE, "myenv", "myserver")).thenReturn(server);
        when(converterUtil.convert(server, DatabaseServerV4Response.class)).thenReturn(response);

        DatabaseServerV4Response response = underTest.delete("myenv", "myserver");

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
        when(service.deleteMultipleByName(DatabaseServerV4Controller.DEFAULT_WORKSPACE, "myenv", nameSet)).thenReturn(serverSet);
        Set<DatabaseServerV4Response> responseSet = new HashSet<>();
        responseSet.add(response);
        responseSet.add(response2);
        when(converterUtil.convertAllAsSet(serverSet, DatabaseServerV4Response.class)).thenReturn(responseSet);

        DatabaseServerV4Responses responses = underTest.deleteMultiple("myenv", nameSet);

        assertEquals(2, responses.getResponses().size());
    }

    @Test
    public void testTestWithIdentifiers() {
        when(service.testConnection(DatabaseServerV4Controller.DEFAULT_WORKSPACE, "myenv", "myserver")).thenReturn("yeahhh");
        DatabaseServerV4Identifiers testIdentifiers = new DatabaseServerV4Identifiers();
        testIdentifiers.setName("myserver");
        testIdentifiers.setEnvironmentId("myenv");
        DatabaseServerTestV4Request testRequest = new DatabaseServerTestV4Request();
        testRequest.setExistingDatabaseServer(testIdentifiers);

        DatabaseServerTestV4Response response = underTest.test(testRequest);

        assertEquals("yeahhh", response.getResult());
    }

    @Test
    public void testTestWithServer() {
        when(converterUtil.convert(request, DatabaseServerConfig.class)).thenReturn(server);
        when(service.testConnection(server)).thenReturn("okayyy");
        DatabaseServerTestV4Request testRequest = new DatabaseServerTestV4Request();
        testRequest.setDatabaseServer(request);

        DatabaseServerTestV4Response response = underTest.test(testRequest);

        assertEquals("okayyy", response.getResult());
    }
}
