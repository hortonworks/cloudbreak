package com.sequenceiq.redbeams.service.dbserverconfig;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.Errors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.archive.AbstractArchivistService;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.redbeams.TestData;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.exception.BadRequestException;
import com.sequenceiq.redbeams.exception.NotFoundException;
import com.sequenceiq.redbeams.repository.DatabaseServerConfigRepository;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.dbconfig.DatabaseConfigService;
import com.sequenceiq.redbeams.service.drivers.DriverFunctions;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.validation.DatabaseServerConnectionValidator;

public class DatabaseServerConfigServiceTest {

    private static final String USERNAME = "username";

    private static final String SERVER_NAME = "myserver";

    private static final long WORKSPACE_ID = 0;

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final Crn SERVER_CRN = Crn.builder()
            .setService(Crn.Service.IAM)
            .setAccountId("accountId")
            .setResourceType(Crn.ResourceType.DATABASE_SERVER)
            .setResource("resource")
            .build();

    private static final Crn SERVER_2_CRN = Crn.builder()
            .setService(Crn.Service.IAM)
            .setAccountId("accountId")
            .setResourceType(Crn.ResourceType.DATABASE_SERVER)
            .setResource("resourceother")
            .build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private DatabaseServerConfigService underTest;

    @Mock
    private DatabaseServerConfigRepository repository;

    @Mock
    private DatabaseConfigService databaseConfigService;

    @Mock
    private DBStackService dbStackService;

    @Mock
    private DriverFunctions driverFunctions;

    @Mock
    private DatabaseCommon databaseCommon;

    @Mock
    private DatabaseServerConnectionValidator connectionValidator;

    @Mock
    private Clock clock;

    @Mock
    private TransactionService transactionService;

    @Mock
    private CrnService crnService;

    @Mock
    private UserGeneratorService userGeneratorService;

    private DatabaseServerConfig server;

    private DatabaseServerConfig server2;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        server = new DatabaseServerConfig();
        server.setId(1L);
        server.setResourceCrn(SERVER_CRN);
        server.setName(SERVER_NAME);
        server.setResourceStatus(ResourceStatus.USER_MANAGED);

        server2 = new DatabaseServerConfig();
        server2.setId(2L);
        server2.setName("myotherserver");
        server2.setResourceCrn(SERVER_2_CRN);

        when(transactionService.required(any(Supplier.class))).thenAnswer((Answer) invocation -> {
            return ((Supplier) invocation.getArguments()[0]).get();
        });
    }

    @Test
    public void testFindAll() {
        when(repository.findByWorkspaceIdAndEnvironmentId(0L, ENVIRONMENT_CRN)).thenReturn(Collections.singleton(server));

        Set<DatabaseServerConfig> servers = underTest.findAll(0L, ENVIRONMENT_CRN);

        assertEquals(1, servers.size());
        assertEquals(1L, servers.iterator().next().getId().longValue());
    }

    @Test
    public void testCreateSuccess() {
        server.setWorkspaceId(-1L);
        server.setConnectionDriver("org.postgresql.MyCustomDriver");
        server.setResourceCrn(null);
        server.setCreationDate(null);
        when(clock.getCurrentTimeMillis()).thenReturn(12345L);
        Crn serverCrn = TestData.getTestCrn("databaseServer", "myserver");
        when(crnService.createCrn(server)).thenReturn(serverCrn);
        when(repository.save(server)).thenReturn(server);

        DatabaseServerConfig createdServer = underTest.create(server, 0L, false);

        assertEquals(server, createdServer);
        assertEquals(0L, createdServer.getWorkspaceId().longValue());
        assertEquals(12345L, createdServer.getCreationDate().longValue());
        assertEquals(serverCrn, createdServer.getResourceCrn());
        assertEquals(serverCrn.getAccountId(), createdServer.getAccountId());
        assertEquals("org.postgresql.MyCustomDriver", createdServer.getConnectionDriver());
    }

    @Test
    public void testCreateSuccessWithoutConnectionDriver() {
        server.setWorkspaceId(-1L);
        server.setConnectionDriver(null);
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        when(clock.getCurrentTimeMillis()).thenReturn(12345L);
        Crn serverCrn = TestData.getTestCrn("databaseServer", "myserver");
        when(crnService.createCrn(server)).thenReturn(serverCrn);
        when(repository.save(server)).thenReturn(server);

        DatabaseServerConfig createdServer = underTest.create(server, 0L, false);

        assertEquals(DatabaseVendor.POSTGRES.connectionDriver(), createdServer.getConnectionDriver());
    }

    @Test
    public void testCreateAlreadyExists() {
        thrown.expect(BadRequestException.class);

        server.setConnectionDriver("org.postgresql.MyCustomDriver");
        Crn serverCrn = TestData.getTestCrn("databaseServer", "myserver");
        when(crnService.createCrn(server)).thenReturn(serverCrn);
        AccessDeniedException e = new AccessDeniedException("no way", mock(ConstraintViolationException.class));
        when(repository.save(server)).thenThrow(e);

        underTest.create(server, 0L, false);
    }

    @Test
    public void testCreateFailure() {
        thrown.expect(AccessDeniedException.class);

        server.setConnectionDriver("org.postgresql.MyCustomDriver");
        Crn serverCrn = TestData.getTestCrn("databaseServer", "myserver");
        when(crnService.createCrn(server)).thenReturn(serverCrn);
        AccessDeniedException e = new AccessDeniedException("no way");
        when(repository.save(server)).thenThrow(e);

        underTest.create(server, 0L, false);
    }

    @Test
    public void testCreateConnectionFailure() {
        thrown.expect(IllegalArgumentException.class);

        server.setConnectionDriver("org.postgresql.MyCustomDriver");
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Errors errors = invocation.getArgument(1);
                errors.rejectValue("databaseVendor", "", "bad vendor");
                errors.reject("", "epic fail");
                return null;
            }
        }).when(connectionValidator).validate(eq(server), any(Errors.class));

        underTest.create(server, 0L, true);
    }

    @Test
    public void testRelease() {
        server.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        DBStack dbStack = new DBStack();
        server.setDbStack(dbStack);

        when(repository.findByResourceCrn(any())).thenReturn(Optional.of(server));
        when(repository.save(server)).thenReturn(server);

        DatabaseServerConfig releasedServer = underTest.release(SERVER_CRN.toString());

        assertEquals(ResourceStatus.USER_MANAGED, releasedServer.getResourceStatus());
        assertFalse(releasedServer.getDbStack().isPresent());

        verify(dbStackService).delete(dbStack);
        verify(repository).save(server);
    }

    @Test
    public void testGetByCrnFound() {
        when(repository.findByResourceCrn(any())).thenReturn(Optional.of(server));

        DatabaseServerConfig foundServer = underTest.getByCrn(SERVER_CRN.toString());

        assertEquals(server, foundServer);
    }

    @Test
    public void testGetByCrnNotFound() {
        thrown.expect(NotFoundException.class);
        when(repository.findByResourceCrn(SERVER_CRN)).thenReturn(Optional.empty());

        underTest.getByCrn(server.getResourceCrn().toString());
    }

    @Test
    public void testGetByNameFound() {
        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(SERVER_NAME, WORKSPACE_ID, ENVIRONMENT_CRN)).thenReturn(Optional.of(server));

        DatabaseServerConfig foundServer = underTest.getByName(WORKSPACE_ID, ENVIRONMENT_CRN, server.getName());

        assertEquals(server, foundServer);
    }

    @Test
    public void testGetByNameNotFound() {
        thrown.expect(NotFoundException.class);

        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(SERVER_NAME, WORKSPACE_ID, ENVIRONMENT_CRN)).thenReturn(Optional.empty());

        underTest.getByName(WORKSPACE_ID, ENVIRONMENT_CRN, server.getName());
    }

    @Test
    public void testDeleteByCrnFound() {
        when(repository.findByResourceCrn(SERVER_CRN)).thenReturn(Optional.of(server));

        DatabaseServerConfig deletedServer = underTest.deleteByCrn(server.getResourceCrn().toString());

        assertEquals(server, deletedServer);
        assertTrue(deletedServer.isArchived());
        verify(repository, never()).delete(server);
    }

    @Test
    public void testDeleteByCrnServiceManaged() {
        server.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        when(repository.findByResourceCrn(SERVER_CRN)).thenReturn(Optional.of(server));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cannot delete service managed configuration. "
                + "Please use termination to stop the database-server and delete the configuration.");

        try {
            underTest.deleteByCrn(server.getResourceCrn().toString());

        } catch (BadRequestException e) {
            assertFalse(server.isArchived());
            throw e;
        }
    }

    @Test
    public void testDeleteByCrnNotFound() {
        thrown.expect(NotFoundException.class);

        when(repository.findByResourceCrn(SERVER_CRN)).thenReturn(Optional.empty());

        underTest.deleteByCrn(server.getResourceCrn().toString());
    }

    @Test
    public void testDeleteByNameFound() {
        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(SERVER_NAME, WORKSPACE_ID, ENVIRONMENT_CRN)).thenReturn(Optional.of(server));

        DatabaseServerConfig deletedServer = underTest.deleteByName(ENVIRONMENT_CRN, SERVER_NAME);

        assertEquals(server, deletedServer);
        assertTrue(deletedServer.isArchived());
        verify(repository, never()).delete(server);
    }

    @Test
    public void testDeleteByNameServiceManaged() {
        server.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(SERVER_NAME, WORKSPACE_ID, ENVIRONMENT_CRN)).thenReturn(Optional.of(server));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cannot delete service managed configuration. "
                + "Please use termination to stop the database-server and delete the configuration.");

        try {
            underTest.deleteByName(ENVIRONMENT_CRN, SERVER_NAME);

        } catch (BadRequestException e) {
            assertFalse(server.isArchived());
            throw e;
        }
    }

    @Test
    public void testDeleteByNameNotFound() {
        thrown.expect(NotFoundException.class);

        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(SERVER_NAME, WORKSPACE_ID, ENVIRONMENT_CRN)).thenReturn(Optional.empty());

        underTest.deleteByName(ENVIRONMENT_CRN, SERVER_NAME);
    }

    @Test
    public void testDeleteMultipleByNameFound() {
        Set<String> crnSet = Set.of(SERVER_CRN.toString(), SERVER_2_CRN.toString());
        Set<DatabaseServerConfig> serverSet = Set.of(server, server2);
        when(repository.findByResourceCrnIn(any())).thenReturn(serverSet);

        Set<DatabaseServerConfig> deletedServerSet = underTest.deleteMultipleByCrn(crnSet);

        assertEquals(2, deletedServerSet.size());
        assertThat(deletedServerSet, everyItem(hasProperty("archived", is(true))));
    }

    @Test
    public void isSubclassOfArchivist() {
        List<Class> superclasses = new ArrayList<>();
        Class currentParentClass = underTest.getClass().getSuperclass();
        do {
            superclasses.add(currentParentClass);
            currentParentClass = currentParentClass.getSuperclass();
        } while (!currentParentClass.equals(Object.class));

        assertThat(superclasses, hasItem(AbstractArchivistService.class));
    }

    @Test
    public void testDeleteMultipleByNameNotFound() {
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("found with crn(s) " + SERVER_2_CRN);

        Set<String> crnSet = Set.of(SERVER_CRN.toString(), SERVER_2_CRN.toString());
        Set<DatabaseServerConfig> serverSet = Set.of(server);
        when(repository.findByResourceCrnIn(Set.of(SERVER_CRN, SERVER_2_CRN))).thenReturn(serverSet);

        underTest.deleteMultipleByCrn(crnSet);
    }

    @Test
    public void testTestConnectionSuccess() {
        when(repository.findByResourceCrn(SERVER_CRN)).thenReturn(Optional.of(server));

        String result = underTest.testConnection(SERVER_CRN.toString());

        assertEquals("success", result);
        verify(connectionValidator).validate(eq(server), any(Errors.class));
    }

    @Test
    public void testTestConnectionFailure() {
        when(repository.findByResourceCrn(SERVER_CRN)).thenReturn(Optional.of(server));
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Errors errors = invocation.getArgument(1);
                errors.rejectValue("databaseVendor", "", "bad vendor");
                errors.reject("", "epic fail");
                return null;
            }
        }).when(connectionValidator).validate(eq(server), any(Errors.class));

        String result = underTest.testConnection(SERVER_CRN.toString());

        assertTrue(result.contains("epic fail"));
        assertTrue(result.contains("databaseVendor: bad vendor"));
        verify(connectionValidator).validate(eq(server), any(Errors.class));
    }

    @Test
    public void testValidateDatabaseName() {
        assertThat(underTest.validateDatabaseName("goodname"), is(true));
        assertThat(underTest.validateDatabaseName("good_name"), is(true));
        assertThat(underTest.validateDatabaseName("_good_name"), is(true));
        assertThat(underTest.validateDatabaseName("_good-name"), is(true));
        assertThat(underTest.validateDatabaseName("bad##name"), is(false));
        assertThat(underTest.validateDatabaseName("_bad##name"), is(false));
        assertThat(underTest.validateDatabaseName("-bad_name"), is(false));
    }

    @Test
    public void testCreateDatabaseOnServer() {
        when(repository.findByResourceCrn(SERVER_CRN)).thenReturn(Optional.of(server));
        when(databaseConfigService.register(any(DatabaseConfig.class), eq(false)))
                .thenAnswer((Answer<DatabaseConfig>) invocation -> {
                    return invocation.getArgument(0, DatabaseConfig.class);
                });
        when(userGeneratorService.generateUserName()).thenReturn(USERNAME);
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setHost("myhost");
        server.setPort(5432);
        server.setConnectionUserName("root");
        server.setConnectionPassword("rootpassword");
        String databaseName = "mydb";
        String databaseType = "hive";
        Optional<String> databaseDescription = Optional.of("mine not yours");

        String result = underTest.createDatabaseOnServer(SERVER_CRN.toString(), databaseName, databaseType, databaseDescription);

        assertEquals("created", result);
        verify(driverFunctions).execWithDatabaseDriver(eq(server), any());
        ArgumentCaptor<DatabaseConfig> captor = ArgumentCaptor.forClass(DatabaseConfig.class);
        verify(databaseConfigService).register(captor.capture(), eq(false));
        DatabaseConfig db = captor.getValue();
        assertEquals(databaseName, db.getName());
        assertEquals(databaseType, db.getType());
        assertEquals(databaseDescription.get(), db.getDescription());
        String databaseUserName = db.getConnectionUserName().getRaw();
        assertEquals(USERNAME, databaseUserName);
        assertNotEquals(server.getConnectionUserName(), databaseUserName);
    }
}
