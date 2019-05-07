package com.sequenceiq.redbeams.controller.v4.databaseserver;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
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
    private DatabaseServerV4Controller controller;

    @Mock
    private DatabaseServerConfigService service;

    @Mock
    private ConverterUtil converterUtil;

    private DatabaseServerConfig server;

    private DatabaseServerV4Response response;

    private Set<DatabaseServerV4Response> responseSet;

    @Before
    public void setUp() {
        initMocks(this);

        server = new DatabaseServerConfig();
        server.setId(1L);

        response = new DatabaseServerV4Response();
        response.setId(1L);

        responseSet = new HashSet<>();
        responseSet.add(response);
    }

    @Test
    public void testList() {
        Set<DatabaseServerConfig> serverSet = Collections.singleton(server);
        when(service.findAllInWorkspaceAndEnvironment(0L, "myenvironment", Boolean.TRUE)).thenReturn(serverSet);
        when(converterUtil.convertAllAsSet(serverSet, DatabaseServerV4Response.class)).thenReturn(responseSet);

        DatabaseServerV4Responses responses = controller.list(0L, "myenvironment", Boolean.TRUE);

        assertEquals(1, responses.getResponses().size());
        assertEquals(response.getId(), responses.getResponses().iterator().next().getId());
    }
}
