package com.sequenceiq.redbeams.service.dbserverconfig;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.redbeams.TestData;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.repository.DatabaseServerConfigRepository;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.dbconfig.DatabaseConfigService;
import com.sequenceiq.redbeams.service.drivers.DriverFunctions;
import com.sequenceiq.redbeams.service.validation.DatabaseServerConnectionValidator;

public class DatabaseServerConfigServiceTest {

    private static final String USERNAME = "username";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private DatabaseServerConfigService underTest;

    @Mock
    private DatabaseServerConfigRepository repository;

    @Mock
    private DatabaseConfigService databaseConfigService;

    @Mock
    private DriverFunctions driverFunctions;

    @Mock
    private DatabaseCommon databaseCommon;

    @Mock
    private DatabaseServerConnectionValidator connectionValidator;

    @Mock
    private Clock clock;

    @Mock
    private CrnService crnService;

    @Mock
    private UserGeneratorService userGeneratorService;

    private DatabaseServerConfig server;

    private DatabaseServerConfig server2;

    @Before
    public void setUp() {
        initMocks(this);

        server = new DatabaseServerConfig();
        server.setId(1L);
        server.setName("myserver");

        server2 = new DatabaseServerConfig();
        server2.setId(2L);
        server2.setName("myotherserver");
    }

    @Test
    public void testFindAll() {
        when(repository.findByWorkspaceIdAndEnvironmentId(0L, "myenv")).thenReturn(Collections.singleton(server));

        Set<DatabaseServerConfig> servers = underTest.findAll(0L, "myenv", false);

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

        DatabaseServerConfig createdServer = underTest.create(server, 0L);

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

        DatabaseServerConfig createdServer = underTest.create(server, 0L);

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

        underTest.create(server, 0L);
    }

    @Test
    public void testCreateFailure() {
        thrown.expect(AccessDeniedException.class);

        server.setConnectionDriver("org.postgresql.MyCustomDriver");
        Crn serverCrn = TestData.getTestCrn("databaseServer", "myserver");
        when(crnService.createCrn(server)).thenReturn(serverCrn);
        AccessDeniedException e = new AccessDeniedException("no way");
        when(repository.save(server)).thenThrow(e);

        underTest.create(server, 0L);
    }

    @Test
    public void testCreateConnectionFailure() {
        thrown.expect(IllegalArgumentException.class);

        server.setConnectionDriver("org.postgresql.MyCustomDriver");
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Errors errors = invocation.getArgument(1);
                errors.rejectValue("connectorJarUrl", "", "bad jar");
                errors.reject("", "epic fail");
                return null;
            }
        }).when(connectionValidator).validate(eq(server), any(Errors.class));

        underTest.create(server, 0L);
    }

    @Test
    public void testGetByNameFound() {
        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(server.getName(), 0L, "myenv")).thenReturn(Optional.of(server));

        DatabaseServerConfig foundServer = underTest.getByNameOrCrn(0L, "myenv", server.getName());

        assertEquals(server, foundServer);
    }

    @Test
    public void testGetByNameNotFound() {
        thrown.expect(NotFoundException.class);

        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(server.getName(), 0L, "myenv")).thenReturn(Optional.empty());

        underTest.getByNameOrCrn(0L, "myenv", server.getName());
    }

    @Test
    public void testDeleteByNameFound() {
        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(server.getName(), 0L, "myenv")).thenReturn(Optional.of(server));

        DatabaseServerConfig deletedServer = underTest.deleteByName(0L, "myenv", server.getName());

        assertEquals(server, deletedServer);
        assertTrue(deletedServer.isArchived());
        verify(repository, never()).delete(server);
    }

    @Test
    public void testDeleteByNameNotFound() {
        thrown.expect(NotFoundException.class);

        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(server.getName(), 0L, "myenv")).thenReturn(Optional.empty());

        try {
            underTest.deleteByName(0L, "myenv", server.getName());
        } finally {
            verify(repository, never()).delete(server);
        }
    }

    @Test
    public void testDeleteMultipleByNameFound() {
        Set<String> nameSet = new HashSet<>();
        nameSet.add(server.getName());
        nameSet.add(server2.getName());
        Set<DatabaseServerConfig> serverSet = new HashSet<>();
        serverSet.add(server);
        serverSet.add(server2);
        when(repository.findByNameInAndWorkspaceIdAndEnvironmentId(nameSet, 0L, "myenv")).thenReturn(serverSet);

        Set<DatabaseServerConfig> deletedServerSet = underTest.deleteMultipleByName(0L, "myenv", nameSet);

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

        Set<String> nameSet = new HashSet<>();
        nameSet.add(server.getName());
        nameSet.add(server2.getName());
        Set<DatabaseServerConfig> serverSet = new HashSet<>();
        serverSet.add(server);
        when(repository.findByNameInAndWorkspaceIdAndEnvironmentId(nameSet, 0L, "myenv")).thenReturn(serverSet);

        try {
            underTest.deleteMultipleByName(0L, "myenv", nameSet);
        } finally {
            verify(repository, never()).delete(server);
            verify(repository, never()).delete(server2);
        }
    }

    @Test
    public void testTestConnectionSuccess() {
        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(server.getName(), 0L, "myenv"))
                .thenReturn(Optional.of(server));

        String result = underTest.testConnection(0L, "myenv", server.getName());

        assertEquals("success", result);
        verify(connectionValidator).validate(eq(server), any(Errors.class));
    }

    @Test
    public void testTestConnectionFailure() {
        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(server.getName(), 0L, "myenv"))
                .thenReturn(Optional.of(server));
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Errors errors = invocation.getArgument(1);
                errors.rejectValue("connectorJarUrl", "", "bad jar");
                errors.reject("", "epic fail");
                return null;
            }
        }).when(connectionValidator).validate(eq(server), any(Errors.class));

        String result = underTest.testConnection(0L, "myenv", server.getName());

        assertTrue(result.contains("epic fail"));
        assertTrue(result.contains("connectorJarUrl: bad jar"));
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
        when(repository.findByNameAndWorkspaceIdAndEnvironmentId(server.getName(), 0L, "myenv")).thenReturn(Optional.of(server));
        when(databaseConfigService.register(any(DatabaseConfig.class)))
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

        String result = underTest.createDatabaseOnServer(0L, "myenv", server.getName(), databaseName, databaseType);

        assertEquals("created", result);
        verify(driverFunctions).execWithDatabaseDriver(eq(server), any());
        ArgumentCaptor<DatabaseConfig> captor = ArgumentCaptor.forClass(DatabaseConfig.class);
        verify(databaseConfigService).register(captor.capture());
        DatabaseConfig db = captor.getValue();
        assertEquals(databaseName, db.getName());
        assertEquals(databaseType, db.getType());
        String databaseUserName = db.getConnectionUserName().getRaw();
        assertEquals(USERNAME, databaseUserName);
        assertNotEquals(server.getConnectionUserName(), databaseUserName);
    }
}
