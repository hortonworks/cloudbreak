package com.sequenceiq.redbeams.controller.v4.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Responses;
import com.sequenceiq.redbeams.converter.database.DatabaseConfigToDatabaseV4ResponseConverter;
import com.sequenceiq.redbeams.converter.database.DatabaseV4RequestToDatabaseConfigConverter;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.service.dbconfig.DatabaseConfigService;

@ExtendWith(MockitoExtension.class)
class DatabaseV4ControllerTest {

    private static final Crn CRN = CrnTestUtil.getDatabaseCrnBuilder()
            .setAccountId("account")
            .setResource("resource")
            .build();

    private static final String DB_CRN = CRN.toString();

    private static final String DB_NAME = "mydb";

    private static final String ENVIRONMENT_CRN = "myenv";

    @InjectMocks
    private DatabaseV4Controller underTest;

    @Mock
    private DatabaseConfigService service;

    @Mock
    private DatabaseV4RequestToDatabaseConfigConverter databaseV4RequestToDatabaseConfigConverter;

    @Mock
    private DatabaseConfigToDatabaseV4ResponseConverter databaseConfigToDatabaseV4ResponseConverter;

    private DatabaseConfig db;

    private DatabaseConfig db2;

    private DatabaseV4Request request;

    private DatabaseV4Response response;

    private DatabaseV4Response response2;

    @BeforeEach
    public void setUp() {
        db = new DatabaseConfig();
        db.setId(1L);
        db.setName(DB_NAME);
        db.setResourceCrn(CRN);

        db2 = new DatabaseConfig();
        db2.setId(2L);
        db2.setName("myotherdb");

        request = new DatabaseV4Request();
        request.setName(DB_NAME);

        response = new DatabaseV4Response();
        response.setName(DB_NAME);

        response2 = new DatabaseV4Response();
        response2.setName("myotherdb");
    }

    @Test
    void testList() {
        Set<DatabaseConfig> dbSet = Collections.singleton(db);
        when(service.findAll(ENVIRONMENT_CRN)).thenReturn(dbSet);

        DatabaseV4Responses responses = underTest.list(ENVIRONMENT_CRN);

        assertEquals(1, responses.getResponses().size());
    }

    @Test
    void testGetByCrn() {
        when(service.getByCrn(DB_CRN)).thenReturn(db);
        when(databaseConfigToDatabaseV4ResponseConverter.convert(any())).thenReturn(new DatabaseV4Response());

        DatabaseV4Response response = underTest.getByCrn(DB_CRN);

        assertNotNull(response);
    }

    @Test
    void testGetByName() {
        when(service.getByName(DB_NAME, ENVIRONMENT_CRN)).thenReturn(db);
        when(databaseConfigToDatabaseV4ResponseConverter.convert(any())).thenReturn(new DatabaseV4Response());

        DatabaseV4Response response = underTest.getByName(ENVIRONMENT_CRN, DB_NAME);

        assertNotNull(response);
    }

    @Test
    void testRegister() {
        when(databaseV4RequestToDatabaseConfigConverter.convert(any())).thenReturn(db);
        when(service.register(db, false)).thenReturn(db);
        when(databaseConfigToDatabaseV4ResponseConverter.convert(db)).thenReturn(response);

        DatabaseV4Response response = underTest.register(request);

        assertEquals(db.getName(), response.getName());
    }

    @Test
    void testDeleteByCrn() {
        when(service.deleteByCrn(DB_CRN)).thenReturn(db);
        when(databaseConfigToDatabaseV4ResponseConverter.convert(db)).thenReturn(response);

        DatabaseV4Response response = underTest.deleteByCrn(DB_CRN);

        assertEquals(db.getName(), response.getName());
    }

    @Test
    void testDeleteByName() {
        when(service.deleteByName(DB_NAME, ENVIRONMENT_CRN)).thenReturn(db);
        when(databaseConfigToDatabaseV4ResponseConverter.convert(db)).thenReturn(response);

        DatabaseV4Response response = underTest.deleteByName(ENVIRONMENT_CRN, DB_NAME);

        assertEquals(db.getName(), response.getName());
    }

    @Test
    void testDeleteMultiple() {
        Set<String> crnSet = new HashSet<>();
        crnSet.add(DB_CRN);
        crnSet.add(DB_CRN + "2");
        Set<DatabaseConfig> dbSet = new HashSet<>();
        dbSet.add(db);
        dbSet.add(db2);
        when(service.deleteMultipleByCrn(crnSet)).thenReturn(dbSet);
        when(databaseConfigToDatabaseV4ResponseConverter.convert(db)).thenReturn(response);
        when(databaseConfigToDatabaseV4ResponseConverter.convert(db2)).thenReturn(response2);

        DatabaseV4Responses responses = underTest.deleteMultiple(crnSet);

        assertEquals(2, responses.getResponses().size());
    }
}
