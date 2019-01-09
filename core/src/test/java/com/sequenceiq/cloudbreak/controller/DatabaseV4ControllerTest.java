package com.sequenceiq.cloudbreak.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseTestV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4TestResponse;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.v4.DatabaseV4Controller;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

public class DatabaseV4ControllerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private DatabaseV4Controller underTest;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ConversionService conversionService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private UserService userService;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testRdsConnectionTestIfArgumentsAreNull() {
        mockWorkspaceService();
        thrown.expect(BadRequestException.class);

        underTest.test(1L, new DatabaseTestV4Request());

        verifyZeroInteractions(rdsConfigService);
        verifyZeroInteractions(conversionService);
    }

    @Test
    public void testRdsConnectionTestWithExistingRdsConfigName() {
        mockWorkspaceService();

        String expectedConnectionResult = "connected";
        when(rdsConfigService.testRdsConnection(anyString(), any())).thenReturn(expectedConnectionResult);

        DatabaseTestV4Request databaseTestV4Request = new DatabaseTestV4Request();
        databaseTestV4Request.setName("TestRdsConfig");
        DatabaseV4TestResponse result = underTest.test(1L, databaseTestV4Request);

        verify(rdsConfigService, times(1)).testRdsConnection(anyString(), any());
        verifyZeroInteractions(conversionService);
        assertEquals(expectedConnectionResult, result.getConnectionResult());
    }

    @Test
    public void testRdsConnectionTestWithRdsConfigRequest() {
        mockWorkspaceService();

        String expectedConnectionResult = "connected";
        when(rdsConfigService.testRdsConnection(any(RDSConfig.class))).thenReturn(expectedConnectionResult);
        when(conversionService.convert(any(DatabaseV4Request.class), eq(RDSConfig.class))).thenReturn(new RDSConfig());

        DatabaseTestV4Request databaseTestV4Request = new DatabaseTestV4Request();
        databaseTestV4Request.setRdsConfig(new DatabaseV4Request());
        DatabaseV4TestResponse result = underTest.test(1L, databaseTestV4Request);

        verify(rdsConfigService, times(1)).testRdsConnection(any(RDSConfig.class));
        verify(conversionService, times(1)).convert(any(DatabaseV4Request.class), eq(RDSConfig.class));
        assertEquals(expectedConnectionResult, result.getConnectionResult());
    }

    private void mockWorkspaceService() {
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(new CloudbreakUser("user", "username","email", "tenant"));
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(workspaceService.get(anyLong(), any())).thenReturn(new Workspace());
        when(rdsConfigService.getWorkspaceService()).thenReturn(workspaceService);
    }

}
