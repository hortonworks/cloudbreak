package com.sequenceiq.redbeams.service.dbserverconfig;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.repository.DatabaseServerConfigRepository;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DatabaseServerConfigServiceTest {

    @InjectMocks
    private DatabaseServerConfigService service;

    @Mock
    private DatabaseServerConfigRepository repository;

    private DatabaseServerConfig server;

    @Before
    public void setUp() {
        initMocks(this);

        server = new DatabaseServerConfig();
        server.setId(1L);
    }

    @Test
    public void testFindAllInWorkspaceAndEnvironment() {
        when(repository.findAllByWorkspaceIdAndEnvironmentId(0L, "myenvironment")).thenReturn(Collections.singleton(server));

        Set<DatabaseServerConfig> servers = service.findAllInWorkspaceAndEnvironment(0L, "myenvironment", false);

        assertEquals(1, servers.size());
        assertEquals(1L, servers.iterator().next().getId().longValue());
    }
}
