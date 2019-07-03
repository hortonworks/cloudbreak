package com.sequenceiq.redbeams.service.dbconfig;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
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

    private static final String CRN = "crn";

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
    private TransactionService transactionService;

    @Mock
    private DatabaseConnectionValidator connectionValidator;

    @InjectMocks
    private DatabaseConfigService underTest;

    private DatabaseConfig db;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        db = new DatabaseConfig();
        db.setId(1L);
        db.setName("mydb");
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
        configToRegister.setResourceCrn(null);
        configToRegister.setCreationDate(null);
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME_MILLIS);
        Crn dbCrn = TestData.getTestCrn("database", "name");
        when(crnService.createCrn(configToRegister)).thenReturn(dbCrn);
        when(repository.save(configToRegister)).thenReturn(configToRegister);

        DatabaseConfig createdConfig = underTest.register(configToRegister);

        assertEquals(configToRegister, createdConfig);
        verify(repository).save(configToRegister);
        assertEquals(CURRENT_TIME_MILLIS, createdConfig.getCreationDate().longValue());
        assertEquals(dbCrn, createdConfig.getResourceCrn());
        assertEquals(dbCrn.getAccountId(), createdConfig.getAccountId());
    }

    @Test
    public void testRegisterConnectionFailure() {
        thrown.expect(IllegalArgumentException.class);
        DatabaseConfig configToRegister = new DatabaseConfig();
        doAnswer((Answer) invocation -> {
            MapBindingResult errors = invocation.getArgument(1, MapBindingResult.class);
            errors.addError(new ObjectError("failed", ERROR_MESSAGE));
            return null;
        }).when(connectionValidator).validate(any(), any());

        underTest.register(configToRegister);

    }

    @Test
    public void testDeleteRegisteredDatabase() throws TransactionService.TransactionExecutionException {
        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.USER_MANAGED, DATABASE_NAME);
        when(repository.findByEnvironmentIdAndName(ENVIRONMENT_CRN, DATABASE_NAME)).thenReturn(Optional.of(databaseConfig));

        underTest.delete(DATABASE_NAME, ENVIRONMENT_CRN);

        assertTrue(databaseConfig.isArchived());
        verify(repository).save(databaseConfig);
    }

    @Test
    public void testDeleteNotFound() {
        thrown.expect(NotFoundException.class);
        when(repository.findByEnvironmentIdAndName(ENVIRONMENT_CRN, DATABASE_NAME)).thenReturn(Optional.empty());

        underTest.delete(DATABASE_NAME, ENVIRONMENT_CRN);
    }

    @Test
    public void testDeleteCreatedDatabase() throws TransactionService.TransactionExecutionException {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setId(1L);
        server.setName("myserver");

        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.SERVICE_MANAGED, DATABASE_NAME);
        databaseConfig.setServer(server);
        when(repository.findByEnvironmentIdAndName(ENVIRONMENT_CRN, DATABASE_NAME)).thenReturn(Optional.of(databaseConfig));
        setupTransactionServiceRequired(Boolean.class);

        underTest.delete(DATABASE_NAME, ENVIRONMENT_CRN);

        assertTrue(databaseConfig.isArchived());
        verify(repository).save(databaseConfig);

        verify(driverFunctions).execWithDatabaseDriver(eq(server), any());
    }

    @Test
    public void testDeleteMultiple() throws TransactionService.TransactionExecutionException {
        Set<DatabaseConfig> databaseConfigs = new HashSet<>();
        databaseConfigs.add(getDatabaseConfig(ResourceStatus.USER_MANAGED, DATABASE_NAME));
        databaseConfigs.add(getDatabaseConfig(ResourceStatus.USER_MANAGED, DATABASE_NAME2));
        Set<String> databasesToDelete = Set.of(DATABASE_NAME, DATABASE_NAME2);
        when(repository.findAllByEnvironmentIdAndNameIn(ENVIRONMENT_CRN, databasesToDelete)).thenReturn(databaseConfigs);
        setupTransactionServiceRequired(DatabaseConfig.class);

        underTest.delete(databasesToDelete, ENVIRONMENT_CRN);

        assertThat(databaseConfigs, Every.everyItem(hasProperty("archived", is(true))));
    }

    @Test
    public void testDeleteMultipleWhenNotFound() throws TransactionService.TransactionExecutionException {
        thrown.expect(NotFoundException.class);
        thrown.expectMessage(String.format("Database(s) for %s not found", DATABASE_NAME2));
        DatabaseConfig databaseConfig = getDatabaseConfig(ResourceStatus.SERVICE_MANAGED, DATABASE_NAME);
        Set<String> databasesToDelete = Set.of(DATABASE_NAME, DATABASE_NAME2);
        when(repository.findAllByEnvironmentIdAndNameIn(ENVIRONMENT_CRN, databasesToDelete)).thenReturn(Set.of(databaseConfig));
        setupTransactionServiceRequired(DatabaseConfig.class);

        underTest.delete(databasesToDelete, ENVIRONMENT_CRN);

        assertFalse(databaseConfig.isArchived());
    }

    @Test
    public void testRegisterEntityWithNameExists() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("database config already exists with name");
        DatabaseConfig configToRegister = new DatabaseConfig();
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME_MILLIS);
        when(crnService.createCrn(configToRegister)).thenReturn(TestData.getTestCrn("database", "name"));
        when(repository.save(configToRegister)).thenThrow(getDataIntegrityException());

        underTest.register(configToRegister);
    }

    private DatabaseConfig getDatabaseConfig(ResourceStatus resourceStatus, String name) {
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setStatus(resourceStatus);
        databaseConfig.setName(DATABASE_NAME);
        databaseConfig.setConnectionUserName(DATABASE_USER_NAME);
        return databaseConfig;
    }

    private DataIntegrityViolationException getDataIntegrityException() {
        return new DataIntegrityViolationException("", new ConstraintViolationException("", new SQLException(), ""));
    }

    @Test
    public void testRegisterHasNoAccess() {
        thrown.expect(AccessDeniedException.class);
        DatabaseConfig configToRegister = new DatabaseConfig();
        when(clock.getCurrentTimeMillis()).thenReturn(CURRENT_TIME_MILLIS);
        when(crnService.createCrn(configToRegister)).thenReturn(TestData.getTestCrn("database", "name"));
        when(repository.save(configToRegister)).thenThrow(new AccessDeniedException("User has no right to access resource"));

        underTest.register(configToRegister);
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

    private <T> void setupTransactionServiceRequired(T supplierReturnType) throws TransactionService.TransactionExecutionException {
        when(transactionService.required(any())).thenAnswer((Answer<T>) invocation -> {
            Supplier<T> supplier = invocation.getArgument(0, Supplier.class);
            return supplier.get();
        });
    }

    @Test
    public void testGetFound() {
        when(repository.findByEnvironmentIdAndName("id", db.getName())).thenReturn(Optional.of(db));

        DatabaseConfig foundDb = underTest.get(db.getName(), "id");

        assertEquals(db, foundDb);
    }

    @Test
    public void testGetNotFound() {
        thrown.expect(NotFoundException.class);

        when(repository.findByEnvironmentIdAndName("id", db.getName())).thenReturn(Optional.empty());

        underTest.get(db.getName(), "id");
    }

}
