package com.sequenceiq.redbeams.service.dbconfig;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.ws.rs.ForbiddenException;

import org.hamcrest.core.Every;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.redbeams.TestData;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.repository.DatabaseConfigRepository;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.drivers.DriverFunctions;
import com.sequenceiq.redbeams.service.validation.DatabaseConnectionValidator;

public class DatabaseConfigServiceTest {

    private static final long CURRENT_TIME_MILLIS = 1000L;

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final Crn DB_CRN = CrnTestUtil.getDatabaseCrnBuilder()
            .setAccountId(ACCOUNT_ID)
            .setResource("resource")
            .build();

    private static final String DB_CRN_STRING = DB_CRN.toString();

    private static final Crn DB2_CRN = CrnTestUtil.getDatabaseCrnBuilder()
            .setAccountId(ACCOUNT_ID)
            .setResource("resource2")
            .build();

    private static final String DB2_CRN_STRING = DB2_CRN.toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String DATABASE_NAME = "databaseName";

    private static final String DATABASE_NAME2 = "databaseName2";

    private static final String DATABASE_USER_NAME = "databaseUserName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String ERROR_MESSAGE = "error message";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private DatabaseConfigRepository repository;

    @Mock
    private DriverFunctions driverFunctions;

    @Mock
    private DatabaseCommon databaseCommon;

    @Mock
    private Clock clock;

    @Mock
    private CrnService crnService;

    @Mock
    private DatabaseConnectionValidator connectionValidator;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private DatabaseConfigService underTest;

    private DatabaseConfig db;

    @Before
    public void setup() throws TransactionService.TransactionExecutionException {
        MockitoAnnotations.initMocks(this);

        db = new DatabaseConfig();
        db.setId(1L);
        db.setName("mydb");

        doNothing().when(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(anyString(), anyString());
        doNothing().when(ownerAssignmentService).notifyResourceDeleted(anyString());
        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any(Supplier.class));
    }

    @Test
    public void testFindAll() {
        when(repository.findByEnvironmentId("myenv")).thenReturn(Collections.singleton(db));

        Set<DatabaseConfig> dbs = underTest.findAll("myenv");

        assertEquals(1, dbs.size());
        assertEquals(1L, dbs.iterator().next().getId().longValue());
    }

    @Test
    public void testRegister() {
        DatabaseConfig configToRegister = new DatabaseConfig();
        configToRegister.setConnectionDriver("org.postgresql.MyCustomDriver");
        configToRegister.setResourceCrn(null);
        configToRegister.setCreationDate(null);
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME_MILLIS);
        Crn dbCrn = TestData.getTestCrn("database", "name");
        when(crnService.createCrn(configToRegister)).thenReturn(dbCrn);
        when(repository.save(configToRegister)).thenReturn(configToRegister);

        DatabaseConfig createdConfig = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.register(configToRegister, false));

        assertEquals(configToRegister, createdConfig);
        verify(repository).save(configToRegister);
        assertEquals(CURRENT_TIME_MILLIS, createdConfig.getCreationDate().longValue());
        assertEquals(dbCrn, createdConfig.getResourceCrn());
        assertEquals(dbCrn.getAccountId(), createdConfig.getAccountId());
        assertEquals("org.postgresql.MyCustomDriver", createdConfig.getConnectionDriver());
    }

    @Test
    public void testRegisterWithoutConnectionDriver() {
        DatabaseConfig configToRegister = new DatabaseConfig();
        configToRegister.setConnectionDriver(null);
        configToRegister.setDatabaseVendor(DatabaseVendor.POSTGRES);
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME_MILLIS);
        Crn dbCrn = TestData.getTestCrn("database", "name");
        when(crnService.createCrn(configToRegister)).thenReturn(dbCrn);
        when(repository.save(configToRegister)).thenReturn(configToRegister);

        DatabaseConfig createdConfig = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.register(configToRegister, false));

        assertEquals(DatabaseVendor.POSTGRES.connectionDriver(), createdConfig.getConnectionDriver());
    }

    @Test
    public void testRegisterConnectionFailure() {
        thrown.expect(IllegalArgumentException.class);
        DatabaseConfig configToRegister = new DatabaseConfig();
        configToRegister.setConnectionDriver("org.postgresql.MyCustomDriver");
        doAnswer((Answer) invocation -> {
            MapBindingResult errors = invocation.getArgument(1, MapBindingResult.class);
            errors.addError(new ObjectError("failed", ERROR_MESSAGE));
            return null;
        }).when(connectionValidator).validate(any(), any());

        underTest.register(configToRegister, true);

    }

    @Test
    public void testDeleteByNameRegisteredDatabase() {
        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.USER_MANAGED, DATABASE_NAME);
        when(repository.findByEnvironmentIdAndName(ENVIRONMENT_CRN, DATABASE_NAME)).thenReturn(Optional.of(databaseConfig));

        underTest.deleteByName(DATABASE_NAME, ENVIRONMENT_CRN);

        assertTrue(databaseConfig.isArchived());
        verify(repository).save(databaseConfig);
    }

    @Test
    public void testDeleteByNameNotFound() {
        thrown.expect(NotFoundException.class);
        when(repository.findByEnvironmentIdAndName(ENVIRONMENT_CRN, DATABASE_NAME)).thenReturn(Optional.empty());

        underTest.deleteByName(DATABASE_NAME, ENVIRONMENT_CRN);
    }

    @Test
    public void testDeleteByNameCreatedDatabase() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setId(1L);
        server.setName("myserver");

        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.SERVICE_MANAGED, DATABASE_NAME);
        databaseConfig.setServer(server);
        when(repository.findByEnvironmentIdAndName(ENVIRONMENT_CRN, DATABASE_NAME)).thenReturn(Optional.of(databaseConfig));

        underTest.deleteByName(DATABASE_NAME, ENVIRONMENT_CRN);

        assertTrue(databaseConfig.isArchived());
        verify(repository).save(databaseConfig);

        verify(driverFunctions).execWithDatabaseDriver(eq(server), any());
    }

    @Test
    public void testDeleteByCrnRegisteredDatabase() {
        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.USER_MANAGED, DATABASE_NAME);
        when(repository.findByResourceCrn(DB_CRN)).thenReturn(Optional.of(databaseConfig));

        underTest.deleteByCrn(DB_CRN_STRING);

        assertTrue(databaseConfig.isArchived());
        verify(repository).save(databaseConfig);
    }

    @Test
    public void testDeleteByCrnNotFound() {
        thrown.expect(NotFoundException.class);
        when(repository.findByResourceCrn(DB_CRN)).thenReturn(Optional.empty());

        underTest.deleteByCrn(DB_CRN_STRING);
    }

    @Test
    public void testDeleteByCrnCreatedDatabase() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setId(1L);
        server.setName("myserver");

        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.SERVICE_MANAGED, DATABASE_NAME);
        databaseConfig.setServer(server);
        when(repository.findByResourceCrn(DB_CRN)).thenReturn(Optional.of(databaseConfig));

        underTest.deleteByCrn(DB_CRN_STRING);

        assertTrue(databaseConfig.isArchived());
        verify(repository).save(databaseConfig);

        verify(driverFunctions).execWithDatabaseDriver(eq(server), any());
    }

    @Test
    public void testDeleteCreatedDatabaseForce() {
        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.SERVICE_MANAGED, DATABASE_NAME);
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setId(1L);
        server.setName("myserver");

        databaseConfig.setServer(server);
        when(repository.findByResourceCrn(DB_CRN)).thenReturn(Optional.of(databaseConfig));

        doThrow(new RuntimeException()).when(driverFunctions).execWithDatabaseDriver(eq(server), any());

        underTest.delete(databaseConfig, true, true);

        assertTrue(databaseConfig.isArchived());
        verify(repository).save(databaseConfig);
    }

    @Test
    public void testDeleteCreatedDatabaseNoForce() {
        thrown.expect(RuntimeException.class);

        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.SERVICE_MANAGED, DATABASE_NAME);
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setId(1L);
        server.setName("myserver");

        databaseConfig.setServer(server);
        when(repository.findByResourceCrn(DB_CRN)).thenReturn(Optional.of(databaseConfig));

        doThrow(new RuntimeException()).when(driverFunctions).execWithDatabaseDriver(eq(server), any());

        try {
            underTest.delete(databaseConfig, false, false);
        } finally {
            assertFalse(databaseConfig.isArchived());
            verify(repository, never()).save(databaseConfig);
        }
    }

    @Test
    public void testDeleteCreatedDatabaseSkipDeletionOnServer() {
        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.SERVICE_MANAGED, DATABASE_NAME);
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setId(1L);
        server.setName("myserver");

        databaseConfig.setServer(server);
        when(repository.findByResourceCrn(DB_CRN)).thenReturn(Optional.of(databaseConfig));

        underTest.delete(databaseConfig, false, true);

        assertTrue(databaseConfig.isArchived());
        verify(repository).save(databaseConfig);

        verify(driverFunctions, never()).execWithDatabaseDriver(eq(server), any());
    }

    @Test
    public void testDeleteMultipleByCrn() {
        Set<DatabaseConfig> databaseConfigs = new HashSet<>();
        databaseConfigs.add(getDatabaseConfig(ResourceStatus.USER_MANAGED, DATABASE_NAME));
        databaseConfigs.add(getDatabaseConfig(ResourceStatus.USER_MANAGED, DATABASE_NAME2));
        Set<Crn> databasesToDelete = Set.of(DB_CRN, DB2_CRN);
        when(repository.findByResourceCrnIn(databasesToDelete)).thenReturn(databaseConfigs);

        underTest.deleteMultipleByCrn(Set.of(DB_CRN_STRING, DB2_CRN_STRING));

        assertThat(databaseConfigs, Every.everyItem(hasProperty("archived", is(true))));
    }

    @Test
    public void testDeleteMultipleByCrnWhenNotFound() {
        thrown.expect(NotFoundException.class);
        thrown.expectMessage(String.format("Database(s) not found: %s", DB2_CRN_STRING));
        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.SERVICE_MANAGED, DATABASE_NAME);
        databaseConfig.setResourceCrn(DB_CRN);
        Set<Crn> databasesToDelete = Set.of(DB_CRN, DB2_CRN);
        when(repository.findByResourceCrnIn(databasesToDelete)).thenReturn(Set.of(databaseConfig));

        underTest.deleteMultipleByCrn(Set.of(DB_CRN_STRING, DB2_CRN_STRING));

        assertFalse(databaseConfig.isArchived());
    }

    @Test
    public void testRegisterEntityWithNameExists() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("database config already exists with name");

        DatabaseConfig configToSave = new DatabaseConfig();
        configToSave.setName("name");

        when(repository.findByName(anyString())).thenReturn(Optional.of(configToSave));

        underTest.register(configToSave, false);
    }

    private DatabaseConfig getDatabaseConfig(ResourceStatus resourceStatus, String name) {
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setStatus(resourceStatus);
        databaseConfig.setName(DATABASE_NAME);
        databaseConfig.setConnectionUserName(DATABASE_USER_NAME);
        databaseConfig.setResourceCrn(DB_CRN);
        return databaseConfig;
    }

    private DataIntegrityViolationException getDataIntegrityException() {
        return new DataIntegrityViolationException("", new ConstraintViolationException("", new SQLException(), ""));
    }

    @Test
    public void testRegisterHasNoAccess() {
        thrown.expect(ForbiddenException.class);
        DatabaseConfig configToRegister = new DatabaseConfig();
        configToRegister.setConnectionDriver("org.postgresql.MyCustomDriver");
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME_MILLIS);
        when(crnService.createCrn(configToRegister)).thenReturn(TestData.getTestCrn("database", "name"));
        when(repository.save(configToRegister)).thenThrow(new ForbiddenException("User has no right to access resource"));

        underTest.register(configToRegister, false);
    }

    @Test
    public void testTestNewConnectionSucceed() {
        DatabaseConfig newConfig = new DatabaseConfig();

        String result = underTest.testConnection(newConfig);

        assertEquals("success", result);
        verify(connectionValidator).validate(eq(newConfig), any());
    }

    @Test
    public void testTestNewConnectionFails() {
        DatabaseConfig newConfig = new DatabaseConfig();
        doAnswer((Answer) invocation -> {
            MapBindingResult errors = invocation.getArgument(1, MapBindingResult.class);
            errors.addError(new ObjectError("failed", ERROR_MESSAGE));
            return null;
        }).when(connectionValidator).validate(any(), any());

        String result = underTest.testConnection(newConfig);

        assertEquals(ERROR_MESSAGE, result);
        verify(connectionValidator).validate(eq(newConfig), any());
    }

    @Test
    public void testExistingConnectionSucceed() {
        DatabaseConfig existingDatabaseConfig = new DatabaseConfig();
        when(repository.findByEnvironmentIdAndName(ENVIRONMENT_CRN, DATABASE_NAME)).thenReturn(Optional.of(existingDatabaseConfig));

        String result = underTest.testConnection(DATABASE_NAME, ENVIRONMENT_CRN);

        assertEquals("success", result);
        verify(connectionValidator).validate(eq(existingDatabaseConfig), any());
    }

    @Test
    public void testExistingConnectionFails() {
        DatabaseConfig existingDatabaseConfig = new DatabaseConfig();
        when(repository.findByEnvironmentIdAndName(ENVIRONMENT_CRN, DATABASE_NAME)).thenReturn(Optional.of(existingDatabaseConfig));
        doAnswer((Answer) invocation -> {
            MapBindingResult errors = invocation.getArgument(1, MapBindingResult.class);
            errors.addError(new ObjectError("failed", ERROR_MESSAGE));
            return null;
        }).when(connectionValidator).validate(any(), any());

        String result = underTest.testConnection(DATABASE_NAME, ENVIRONMENT_CRN);

        assertEquals(ERROR_MESSAGE, result);
        verify(connectionValidator).validate(eq(existingDatabaseConfig), any());
    }

    @Test
    public void testGetByCrnFound() {
        when(repository.findByResourceCrn(DB_CRN)).thenReturn(Optional.of(db));

        DatabaseConfig foundDb = underTest.getByCrn(DB_CRN_STRING);

        assertEquals(db, foundDb);
    }

    @Test
    public void testGetByCrnNotFound() {
        thrown.expect(NotFoundException.class);

        when(repository.findByResourceCrn(DB_CRN)).thenReturn(Optional.empty());

        underTest.getByCrn(DB_CRN_STRING);
    }

    @Test
    public void testGetByNameFound() {
        when(repository.findByEnvironmentIdAndName("id", db.getName())).thenReturn(Optional.of(db));

        DatabaseConfig foundDb = underTest.getByName(db.getName(), "id");

        assertEquals(db, foundDb);
    }

    @Test
    public void testGetByNameNotFound() {
        thrown.expect(NotFoundException.class);

        when(repository.findByEnvironmentIdAndName("id", db.getName())).thenReturn(Optional.empty());

        underTest.getByName(db.getName(), "id");
    }

}
