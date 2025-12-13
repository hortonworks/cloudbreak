package com.sequenceiq.redbeams.service.dbconfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class DatabaseConfigServiceTest {

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

    @BeforeEach
    public void setup() throws TransactionService.TransactionExecutionException {
        db = new DatabaseConfig();
        db.setId(1L);
        db.setName("mydb");

        lenient().doNothing().when(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(anyString(), anyString());
        lenient().doNothing().when(ownerAssignmentService).notifyResourceDeleted(anyString());
        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any(Supplier.class));
    }

    @Test
    void testFindAll() {
        when(repository.findByEnvironmentId("myenv")).thenReturn(Collections.singleton(db));

        Set<DatabaseConfig> dbs = underTest.findAll("myenv");

        assertEquals(1, dbs.size());
        assertEquals(1L, dbs.iterator().next().getId().longValue());
    }

    @Test
    void testRegister() {
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
    void testRegisterWithoutConnectionDriver() {
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
    void testRegisterConnectionFailure() {
        DatabaseConfig configToRegister = new DatabaseConfig();
        configToRegister.setConnectionDriver("org.postgresql.MyCustomDriver");
        doAnswer((Answer) invocation -> {
            MapBindingResult errors = invocation.getArgument(1, MapBindingResult.class);
            errors.addError(new ObjectError("failed", ERROR_MESSAGE));
            return null;
        }).when(connectionValidator).validate(any(), any());

        assertThrows(IllegalArgumentException.class, () -> underTest.register(configToRegister, true));
    }

    @Test
    void testDeleteByNameRegisteredDatabase() {
        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.USER_MANAGED, DATABASE_NAME);
        when(repository.findByEnvironmentIdAndName(ENVIRONMENT_CRN, DATABASE_NAME)).thenReturn(Optional.of(databaseConfig));

        underTest.deleteByName(DATABASE_NAME, ENVIRONMENT_CRN);

        assertTrue(databaseConfig.isArchived());
        verify(repository).save(databaseConfig);
    }

    @Test
    void testDeleteByNameNotFound() {
        when(repository.findByEnvironmentIdAndName(ENVIRONMENT_CRN, DATABASE_NAME)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.deleteByName(DATABASE_NAME, ENVIRONMENT_CRN));
    }

    @Test
    void testDeleteByNameCreatedDatabase() {
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
    void testDeleteByCrnRegisteredDatabase() {
        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.USER_MANAGED, DATABASE_NAME);
        when(repository.findByResourceCrn(DB_CRN)).thenReturn(Optional.of(databaseConfig));

        underTest.deleteByCrn(DB_CRN_STRING);

        assertTrue(databaseConfig.isArchived());
        verify(repository).save(databaseConfig);
    }

    @Test
    void testDeleteByCrnNotFound() {
        when(repository.findByResourceCrn(DB_CRN)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.deleteByCrn(DB_CRN_STRING));
    }

    @Test
    void testDeleteByCrnCreatedDatabase() {
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
    void testDeleteCreatedDatabaseForce() {
        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.SERVICE_MANAGED, DATABASE_NAME);
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setId(1L);
        server.setName("myserver");

        databaseConfig.setServer(server);

        underTest.delete(databaseConfig, true, true);

        assertTrue(databaseConfig.isArchived());
        verify(repository).save(databaseConfig);
    }

    @Test
    void testDeleteCreatedDatabaseNoForce() {
        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.SERVICE_MANAGED, DATABASE_NAME);
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setId(1L);
        server.setName("myserver");

        databaseConfig.setServer(server);

        doThrow(new RuntimeException()).when(driverFunctions).execWithDatabaseDriver(eq(server), any());

        try {
            assertThrows(RuntimeException.class, () -> underTest.delete(databaseConfig, false, false));
        } finally {
            assertFalse(databaseConfig.isArchived());
            verify(repository, never()).save(databaseConfig);
        }
    }

    @Test
    void testDeleteCreatedDatabaseSkipDeletionOnServer() {
        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.SERVICE_MANAGED, DATABASE_NAME);
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setId(1L);
        server.setName("myserver");

        databaseConfig.setServer(server);

        underTest.delete(databaseConfig, false, true);

        assertTrue(databaseConfig.isArchived());
        verify(repository).save(databaseConfig);

        verify(driverFunctions, never()).execWithDatabaseDriver(eq(server), any());
    }

    @Test
    void testDeleteMultipleByCrn() {
        Set<DatabaseConfig> databaseConfigs = new HashSet<>();
        databaseConfigs.add(getDatabaseConfig(ResourceStatus.USER_MANAGED, DATABASE_NAME));
        databaseConfigs.add(getDatabaseConfig(ResourceStatus.USER_MANAGED, DATABASE_NAME2));
        Set<Crn> databasesToDelete = Set.of(DB_CRN, DB2_CRN);
        when(repository.findByResourceCrnIn(databasesToDelete)).thenReturn(databaseConfigs);

        underTest.deleteMultipleByCrn(Set.of(DB_CRN_STRING, DB2_CRN_STRING));

        assertThat(databaseConfigs, Every.everyItem(hasProperty("archived", is(true))));
    }

    @Test
    void testDeleteMultipleByCrnWhenNotFound() {
        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.SERVICE_MANAGED, DATABASE_NAME);
        databaseConfig.setResourceCrn(DB_CRN);
        Set<Crn> databasesToDelete = Set.of(DB_CRN, DB2_CRN);
        when(repository.findByResourceCrnIn(databasesToDelete)).thenReturn(Set.of(databaseConfig));

        assertThrows(NotFoundException.class, () -> underTest.deleteMultipleByCrn(Set.of(DB_CRN_STRING, DB2_CRN_STRING)),
                String.format("Database(s) not found: %s", DB2_CRN_STRING));

        assertFalse(databaseConfig.isArchived());
    }

    @Test
    void testRegisterEntityWithNameExists() {
        DatabaseConfig configToSave = new DatabaseConfig();
        configToSave.setName("name");

        when(repository.findByName(anyString())).thenReturn(Optional.of(configToSave));

        assertThrows(BadRequestException.class, () -> underTest.register(configToSave, false), "database config already exists with name");
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
    void testRegisterHasNoAccess() {
        DatabaseConfig configToRegister = new DatabaseConfig();
        configToRegister.setConnectionDriver("org.postgresql.MyCustomDriver");
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME_MILLIS);
        when(crnService.createCrn(configToRegister)).thenReturn(TestData.getTestCrn("database", "name"));
        when(repository.save(configToRegister)).thenThrow(new ForbiddenException("User has no right to access resource"));

        assertThrows(ForbiddenException.class, () -> underTest.register(configToRegister, false));
    }

    @Test
    void testTestNewConnectionSucceed() {
        DatabaseConfig newConfig = new DatabaseConfig();

        String result = underTest.testConnection(newConfig);

        assertEquals("success", result);
        verify(connectionValidator).validate(eq(newConfig), any());
    }

    @Test
    void testTestNewConnectionFails() {
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
    void testExistingConnectionSucceed() {
        DatabaseConfig existingDatabaseConfig = new DatabaseConfig();
        when(repository.findByEnvironmentIdAndName(ENVIRONMENT_CRN, DATABASE_NAME)).thenReturn(Optional.of(existingDatabaseConfig));

        String result = underTest.testConnection(DATABASE_NAME, ENVIRONMENT_CRN);

        assertEquals("success", result);
        verify(connectionValidator).validate(eq(existingDatabaseConfig), any());
    }

    @Test
    void testExistingConnectionFails() {
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
    void testGetByCrnFound() {
        when(repository.findByResourceCrn(DB_CRN)).thenReturn(Optional.of(db));

        DatabaseConfig foundDb = underTest.getByCrn(DB_CRN_STRING);

        assertEquals(db, foundDb);
    }

    @Test
    void testGetByCrnNotFound() {
        when(repository.findByResourceCrn(DB_CRN)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.getByCrn(DB_CRN_STRING));
    }

    @Test
    void testGetByNameFound() {
        when(repository.findByEnvironmentIdAndName("id", db.getName())).thenReturn(Optional.of(db));

        DatabaseConfig foundDb = underTest.getByName(db.getName(), "id");

        assertEquals(db, foundDb);
    }

    @Test
    void testGetByNameNotFound() {
        when(repository.findByEnvironmentIdAndName("id", db.getName())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.getByName(db.getName(), "id"));
    }

}
