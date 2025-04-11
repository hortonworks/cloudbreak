package com.sequenceiq.redbeams.service.dbserverconfig;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.common.archive.AbstractArchivistService;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.redbeams.TestData;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.repository.DatabaseServerConfigRepository;
import com.sequenceiq.redbeams.service.PasswordGeneratorService;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.dbconfig.DatabaseConfigService;
import com.sequenceiq.redbeams.service.drivers.DriverFunctions;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.validation.DatabaseServerConnectionValidator;

@ExtendWith(MockitoExtension.class)
public class DatabaseServerConfigServiceTest {

    private static final String USERNAME = "username";

    private static final String PASSWORD = "password";

    private static final String SERVER_NAME = "myserver";

    private static final long WORKSPACE_ID = 0;

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final Crn CLUSTER_CRN = CrnTestUtil.getDatahubCrnBuilder()
            .setAccountId(ACCOUNT_ID)
            .setResource("resource")
            .build();

    private static final Crn SERVER_CRN = CrnTestUtil.getDatabaseServerCrnBuilder()
            .setAccountId(ACCOUNT_ID)
            .setResource("resource")
            .build();

    private static final Crn SERVER_2_CRN = CrnTestUtil.getDatabaseServerCrnBuilder()
            .setAccountId(ACCOUNT_ID)
            .setResource("resourceother")
            .build();

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

    @Mock
    private PasswordGeneratorService passwordGeneratorService;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    private DatabaseServerConfig server;

    private DatabaseServerConfig server2;

    @BeforeEach
    public void setUp() throws Exception {
        server = new DatabaseServerConfig();
        server.setId(1L);
        server.setResourceCrn(SERVER_CRN);
        server.setName(SERVER_NAME);
        server.setResourceStatus(ResourceStatus.USER_MANAGED);

        server2 = new DatabaseServerConfig();
        server2.setId(2L);
        server2.setName("myotherserver");
        server2.setResourceCrn(SERVER_2_CRN);

//        doNothing().when(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(anyString(), anyString());
        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any(Supplier.class));
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

        DatabaseServerConfig createdServer = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.create(server, 0L));

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
        when(repository.save(server)).thenReturn(server);

        DatabaseServerConfig createdServer = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.create(server, 0L));

        assertEquals(DatabaseVendor.POSTGRES.connectionDriver(), createdServer.getConnectionDriver());
    }

    @Test
    public void testCreateAlreadyExists() {
        when(repository.findByName(anyString())).thenReturn(Optional.of(server));

        assertThrows(BadRequestException.class, () -> underTest.create(server, 0L));
    }

    @Test
    public void testCreateFailure() {
        server.setConnectionDriver("org.postgresql.MyCustomDriver");
        ForbiddenException e = new ForbiddenException("no way");
        when(repository.save(server)).thenThrow(e);

        assertThrows(ForbiddenException.class, () -> underTest.create(server, 0L));
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
        when(repository.findByResourceCrn(SERVER_CRN)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.getByCrn(server.getResourceCrn().toString()));
    }

    @Test
    public void testGetByNameFound() {
        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(SERVER_NAME, WORKSPACE_ID, ENVIRONMENT_CRN)).thenReturn(Optional.of(server));

        DatabaseServerConfig foundServer = underTest.getByName(WORKSPACE_ID, ENVIRONMENT_CRN, server.getName());

        assertEquals(server, foundServer);
    }

    @Test
    public void testGetByNameNotFound() {
        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(SERVER_NAME, WORKSPACE_ID, ENVIRONMENT_CRN)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> underTest.getByName(WORKSPACE_ID, ENVIRONMENT_CRN, server.getName()));
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
    public void testDeleteByCrnNotFound() {
        when(repository.findByResourceCrn(SERVER_CRN)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.deleteByCrn(server.getResourceCrn().toString()));
    }

    @Test
    public void testDeleteCreatedServerWithDatabases() {
        when(repository.findByResourceCrn(SERVER_CRN)).thenReturn(Optional.of(server));

        server.setResourceStatus(ResourceStatus.SERVICE_MANAGED);

        DatabaseConfig databaseConfig1 = new DatabaseConfig();
        databaseConfig1.setId(1L);
        DatabaseConfig databaseConfig2 = new DatabaseConfig();
        databaseConfig2.setId(2L);
        Set<DatabaseConfig> databases = new HashSet<>();
        databases.add(databaseConfig1);
        databases.add(databaseConfig2);
        server.setDatabases(databases);

        underTest.delete(server);

        verify(databaseConfigService).delete(databaseConfig1, true, true);
        verify(databaseConfigService).delete(databaseConfig2, true, true);
    }

    @Test
    public void testGetByCrnsFound() {
        Set<String> crnSet = Set.of(SERVER_CRN.toString(), SERVER_2_CRN.toString());
        Set<DatabaseServerConfig> serverSet = Set.of(server, server2);
        when(repository.findByResourceCrnIn(any())).thenReturn(serverSet);

        Set<DatabaseServerConfig> gottenSet = underTest.getByCrns(crnSet);

        assertEquals(serverSet, gottenSet);
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
    public void testGetByCrnsNotFound() {
        Set<String> crnSet = Set.of(SERVER_CRN.toString(), SERVER_2_CRN.toString());
        Set<DatabaseServerConfig> serverSet = Set.of(server);
        when(repository.findByResourceCrnIn(Set.of(SERVER_CRN, SERVER_2_CRN))).thenReturn(serverSet);
        NotFoundException exception = assertThrows(NotFoundException.class, () -> underTest.getByCrns(crnSet));
        assertThat(exception.getMessage(), containsString("found with crn(s) " + SERVER_2_CRN));
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
        when(passwordGeneratorService.generatePassword(any())).thenReturn(PASSWORD);
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
        assertEquals(PASSWORD, db.getConnectionPassword().getRaw());
    }

    @Test
    public void testFindByClusterCrnFound() {
        when(repository.findByEnvironmentIdAndClusterCrn(anyString(), anyString())).thenReturn(List.of(server));

        Optional<DatabaseServerConfig> foundServer = underTest.findByClusterCrn(ENVIRONMENT_CRN, CLUSTER_CRN.toString());

        assertTrue(foundServer.isPresent());
        assertEquals(server, foundServer.get());
    }

    @Test
    public void testFindByClusterCrnShouldThrowExceptionWhenMoreThanOneServerPresentForTheCluster() {
        when(repository.findByEnvironmentIdAndClusterCrn(anyString(), anyString())).thenReturn(List.of(server, server));

        assertThrows(BadRequestException.class, () ->  underTest.findByClusterCrn(ENVIRONMENT_CRN, CLUSTER_CRN.toString()));
    }

    @Test
    public void testFindByClusterCrnShouldReturnOptionalEmpty() {
        when(repository.findByEnvironmentIdAndClusterCrn(anyString(), anyString())).thenReturn(Collections.emptyList());

        Optional<DatabaseServerConfig> actual = underTest.findByClusterCrn(ENVIRONMENT_CRN, CLUSTER_CRN.toString());
        assertTrue(actual.isEmpty());
    }

    @Test
    public void testListByClusterCrnShouldReturnTheConfigList() {
        when(repository.findByEnvironmentIdAndClusterCrn(anyString(), anyString())).thenReturn(List.of(server));

        List<DatabaseServerConfig> foundServer = underTest.listByClusterCrn(ENVIRONMENT_CRN, CLUSTER_CRN.toString());

        assertEquals(server, foundServer.getFirst());
    }

    @Test
    public void testListByClusterCrnShouldThrowsExceptionWhenTheConfigsNotFound() {
        when(repository.findByEnvironmentIdAndClusterCrn(anyString(), anyString())).thenReturn(Collections.emptyList());

        assertThrows(NotFoundException.class, () -> underTest.listByClusterCrn(ENVIRONMENT_CRN, CLUSTER_CRN.toString()));
    }

}
