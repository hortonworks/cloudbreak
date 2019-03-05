package com.sequenceiq.cloudbreak.structuredevent.converter;

import static org.junit.runners.Parameterized.Parameters;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;

@RunWith(Parameterized.class)
public class RdsConfigToRdsDetailsConverterTest {

    private static final Long WORKSPACE_ID_FROM_REQUEST = 4321L;

    private CloudbreakUser cloudbreakUser;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @InjectMocks
    private RdsConfigToRdsDetailsConverter underTest;

    private RDSConfig source;

    public RdsConfigToRdsDetailsConverterTest(DatabaseType databaseType, DatabaseVendor vendor) {
        source = TestUtil.rdsConfig(databaseType, vendor);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        cloudbreakUser = TestUtil.cbUser();
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID_FROM_REQUEST);
    }

    @Parameters(name = "Current RDS type - Database vendor pair: [{0} - {1}]")
    public static Object[][] data() {
        return TestUtil.combinationOf(DatabaseType.values(), DatabaseVendor.values());
    }

    @Test
    public void testWhenSourceWorkspaceIdIsNotNullThenItsValueShouldBePassedBesideAllLogicIndependentData() {
        Workspace workspace = new Workspace();
        workspace.setId(23L);
        source.setWorkspace(workspace);
        RdsDetails result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(source.getWorkspace().getId(), result.getWorkspaceId());
        verify(restRequestThreadLocalService, times(0)).getRequestedWorkspaceId();
    }

    @Test
    public void testWhenSourceWorkspaceIdIsNullThenItsValueShouldBePassedBesideAllLogicIndependentData() {
        source.setWorkspace(null);
        RdsDetails result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(WORKSPACE_ID_FROM_REQUEST, result.getWorkspaceId());
        verify(restRequestThreadLocalService, times(1)).getRequestedWorkspaceId();
    }

    @Test
    public void testAllLogicIndependentDataArePassedProperly() {
        RdsDetails result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(source.getConnectionDriver(), result.getConnectionDriver());
        assertEquals(source.getConnectionURL(), result.getConnectionURL());
        assertEquals(source.getConnectorJarUrl(), result.getConnectorJarUrl());
        assertEquals(source.getCreationDate(), result.getCreationDate());
        assertEquals(source.getDatabaseEngine().name(), result.getDatabaseEngine());
        assertEquals(source.getDescription(), result.getDescription());
        assertEquals(source.getId(), result.getId());
        assertEquals(source.getName(), result.getName());
        assertEquals(source.getStackVersion(), result.getStackVersion());
        assertEquals(source.getStatus().name(), result.getStatus());
        assertEquals(source.getType(), result.getType());
        assertEquals(cloudbreakUser.getUsername(), result.getUserName());
        assertEquals(cloudbreakUser.getUserId(), result.getUserId());
        assertEquals(cloudbreakUser.getTenant(), result.getTenantName());
        verify(restRequestThreadLocalService, times(3)).getCloudbreakUser();
    }

}