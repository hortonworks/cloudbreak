package com.sequenceiq.cloudbreak.structuredevent.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class RdsConfigToRdsDetailsConverterTest {

    private static final Long WORKSPACE_ID_FROM_REQUEST = 4321L;

    private CloudbreakUser cloudbreakUser;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @InjectMocks
    private RdsConfigToRdsDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        cloudbreakUser = TestUtil.cbUser();
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
    }

    public static Object[][] databaseTypeAndVendorDataProvider() {
        return TestUtil.combinationOf(DatabaseType.values(), DatabaseVendor.values());
    }

    @ParameterizedTest(name = "Current RDS type - Database vendor pair: [{0} - {1}]")
    @MethodSource("databaseTypeAndVendorDataProvider")
    public void testWhenSourceWorkspaceIdIsNotNullThenItsValueShouldBePassedBesideAllLogicIndependentData(DatabaseType databaseType, DatabaseVendor vendor) {
        Workspace workspace = new Workspace();
        workspace.setId(23L);
        RDSConfig source = TestUtil.rdsConfig(databaseType, vendor);
        source.setWorkspace(workspace);

        RdsDetails result = underTest.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getWorkspaceId()).isEqualTo(source.getWorkspace().getId());
        verify(restRequestThreadLocalService, never()).getRequestedWorkspaceId();
    }

    @ParameterizedTest(name = "Current RDS type - Database vendor pair: [{0} - {1}]")
    @MethodSource("databaseTypeAndVendorDataProvider")
    public void testWhenSourceWorkspaceIdIsNullThenItsValueShouldBePassedBesideAllLogicIndependentData(DatabaseType databaseType, DatabaseVendor vendor) {
        RDSConfig source = TestUtil.rdsConfig(databaseType, vendor);
        source.setWorkspace(null);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID_FROM_REQUEST);

        RdsDetails result = underTest.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getWorkspaceId()).isEqualTo(WORKSPACE_ID_FROM_REQUEST);
        verify(restRequestThreadLocalService).getRequestedWorkspaceId();
    }

    @ParameterizedTest(name = "Current RDS type - Database vendor pair: [{0} - {1}]")
    @MethodSource("databaseTypeAndVendorDataProvider")
    public void testWhenDatabaseEngineIsEmbeddedThenIsExternalShouldBeFalseOtherwiseTrue(DatabaseType databaseType, DatabaseVendor vendor) {
        RDSConfig source = TestUtil.rdsConfig(databaseType, vendor);

        RdsDetails result = underTest.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getExternal()).isEqualTo(source.getDatabaseEngine() != DatabaseVendor.EMBEDDED);
    }

    @ParameterizedTest(name = "Current RDS type - Database vendor pair: [{0} - {1}]")
    @MethodSource("databaseTypeAndVendorDataProvider")
    public void testWhenSslModeIsNullThenNullShouldBePassed(DatabaseType databaseType, DatabaseVendor vendor) {
        testWhenSslModeInternal(databaseType, vendor, null, null);
    }

    @ParameterizedTest(name = "Current RDS type - Database vendor pair: [{0} - {1}]")
    @MethodSource("databaseTypeAndVendorDataProvider")
    public void testWhenSslModeIsDisabledThenItsNameShouldBePassed(DatabaseType databaseType, DatabaseVendor vendor) {
        testWhenSslModeInternal(databaseType, vendor, RdsSslMode.DISABLED, RdsSslMode.DISABLED.name());
    }

    @ParameterizedTest(name = "Current RDS type - Database vendor pair: [{0} - {1}]")
    @MethodSource("databaseTypeAndVendorDataProvider")
    public void testWhenSslModeIsEnabledThenItsNameShouldBePassed(DatabaseType databaseType, DatabaseVendor vendor) {
        testWhenSslModeInternal(databaseType, vendor, RdsSslMode.ENABLED, RdsSslMode.ENABLED.name());
    }

    private void testWhenSslModeInternal(DatabaseType databaseType, DatabaseVendor vendor, RdsSslMode sslMode, String sslModeStringExpected) {
        RDSConfig source = TestUtil.rdsConfig(databaseType, vendor);
        source.setSslMode(sslMode);

        RdsDetails result = underTest.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getSslMode()).isEqualTo(sslModeStringExpected);
    }

    @ParameterizedTest(name = "Current RDS type - Database vendor pair: [{0} - {1}]")
    @MethodSource("databaseTypeAndVendorDataProvider")
    public void testAllLogicIndependentDataArePassedProperly(DatabaseType databaseType, DatabaseVendor vendor) {
        RDSConfig source = TestUtil.rdsConfig(databaseType, vendor);

        RdsDetails result = underTest.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getConnectionDriver()).isEqualTo(source.getConnectionDriver());
        assertThat(result.getConnectionURL()).isEqualTo(source.getConnectionURL());
        assertThat(result.getConnectorJarUrl()).isEqualTo(source.getConnectorJarUrl());
        assertThat(result.getCreationDate()).isEqualTo(source.getCreationDate());
        assertThat(result.getDatabaseEngine()).isEqualTo(source.getDatabaseEngine().name());
        assertThat(result.getDescription()).isEqualTo(source.getDescription());
        assertThat(result.getId()).isEqualTo(source.getId());
        assertThat(result.getName()).isEqualTo(source.getName());
        assertThat(result.getStackVersion()).isEqualTo(source.getStackVersion());
        assertThat(result.getStatus()).isEqualTo(source.getStatus().name());
        assertThat(result.getType()).isEqualTo(source.getType());
        assertThat(result.getUserName()).isEqualTo(cloudbreakUser.getUsername());
        assertThat(result.getUserId()).isEqualTo(cloudbreakUser.getUserId());
        assertThat(result.getTenantName()).isEqualTo(cloudbreakUser.getTenant());
        verify(restRequestThreadLocalService).getCloudbreakUser();
    }

}