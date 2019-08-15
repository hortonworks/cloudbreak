package com.sequenceiq.redbeams.controller.v4.database;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Responses;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.service.dbconfig.DatabaseConfigService;

// import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseTestV4Request;
// import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseTestV4Response;
// import java.util.HashSet;

public class DatabaseV4ControllerTest {

    @InjectMocks
    private DatabaseV4Controller underTest;

    @Mock
    private DatabaseConfigService service;

    @Mock
    private ConverterUtil converterUtil;

    private DatabaseConfig db;

    private DatabaseConfig db2;

    private DatabaseV4Request request;

    private DatabaseV4Response response;

    private DatabaseV4Response response2;

    @Before
    public void setUp() {
        initMocks(this);

        db = new DatabaseConfig();
        db.setId(1L);
        db.setName("mydb");

        db2 = new DatabaseConfig();
        db2.setId(2L);
        db2.setName("myotherdb");

        request = new DatabaseV4Request();
        request.setName("mydb");

        response = new DatabaseV4Response();
        response.setName("mydb");

        response2 = new DatabaseV4Response();
        response2.setName("myotherdb");
    }

    @Test
    public void testList() {
        Set<DatabaseConfig> dbSet = Collections.singleton(db);
        when(service.findAll("myenvironment")).thenReturn(dbSet);
        Set<DatabaseV4Response> responseSet = Collections.singleton(response);
        when(converterUtil.convertAllAsSet(dbSet, DatabaseV4Response.class)).thenReturn(responseSet);

        DatabaseV4Responses responses = underTest.list("myenvironment");

        assertEquals(1, responses.getResponses().size());
        assertEquals(response.getName(), responses.getResponses().iterator().next().getName());
    }

    @Test
    public void testGet() {
        when(service.get("mydb", "myenvironment")).thenReturn(db);
        when(converterUtil.convert(db, DatabaseV4Response.class)).thenReturn(response);

        DatabaseV4Response response = underTest.get("myenvironment", "mydb");

        assertEquals(db.getName(), response.getName());
    }

    @Test
    public void testRegister() {
        when(converterUtil.convert(request, DatabaseConfig.class)).thenReturn(db);
        when(service.register(db, false)).thenReturn(db);
        when(converterUtil.convert(db, DatabaseV4Response.class)).thenReturn(response);

        DatabaseV4Response response = underTest.register(request);

        assertEquals(db.getName(), response.getName());
    }

    // @Test
    // public void testDelete() {
    //     when(service.deleteByNameInWorkspace(DatabaseV4Controller.DEFAULT_WORKSPACE, "id",  "mydb")).thenReturn(db);
    //     when(converterUtil.convert(db, DatabaseV4Response.class)).thenReturn(response);

    //     DatabaseV4Response response = underTest.delete("id", "mydb");

    //     assertEquals(1L, response.getId().longValue());
    // }

    // @Test
    // public void testDeleteMultiple() {
    //     Set<String> nameSet = new HashSet<>();
    //     nameSet.add(db.getName());
    //     nameSet.add(db2.getName());
    //     Set<DatabaseConfig> dbSet = new HashSet<>();
    //     dbSet.add(db);
    //     dbSet.add(db2);
    //     when(service.deleteMultipleByNameInWorkspace(DatabaseV4Controller.DEFAULT_WORKSPACE, "id", nameSet)).thenReturn(dbSet);
    //     Set<DatabaseV4Response> responseSet = new HashSet<>();
    //     responseSet.add(response);
    //     responseSet.add(response2);
    //     when(converterUtil.convertAllAsSet(dbSet, DatabaseV4Response.class)).thenReturn(responseSet);

    //     DatabaseV4Responses responses = underTest.deleteMultiple("id", nameSet);

    //     assertEquals(2, responses.getResponses().size());
    // }

    // @Test
    // public void testTestWithName() {
    //     when(service.testConnection(DatabaseV4Controller.DEFAULT_WORKSPACE, "id", "mydb")).thenReturn("yeahhh");
    //     DatabaseServerTestV4Request testRequest = new DatabaseServerTestV4Request();
    //     testRequest.setEnvironmentId("id");
    //     testRequest.setExistingDatabaseServerCrn("mydb");

    //     DatabaseServerTestV4Response response = underTest.test(testRequest);

    //     assertEquals("yeahhh", response.getResult());
    // }

    // @Test
    // public void testTestWithServer() {
    //     when(converterUtil.convert(request, DatabaseConfig.class)).thenReturn(db);
    //     when(service.testConnection(db)).thenReturn("okayyy");
    //     DatabaseServerTestV4Request testRequest = new DatabaseServerTestV4Request();
    //     testRequest.setDatabaseServer(request);

    //     DatabaseServerTestV4Response response = underTest.test(testRequest);

    //     assertEquals("okayyy", response.getResult());
    // }
}
