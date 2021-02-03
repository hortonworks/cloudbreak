package com.sequenceiq.cloudbreak.service.cluster;

import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@ExtendWith(MockitoExtension.class)
public class ClusterDBValidationServiceTest {
    @Mock
    private RdsConfigService rdsConfigService;

    @InjectMocks
    private ClusterDBValidationService underTest;

    @Test
    public void testIsGatewayRepairEnabledWhenEmbeddedDBOnAttachedDisk() {
        // GIVEN
        Cluster cluster = new Cluster();
        cluster.setEmbeddedDatabaseOnAttachedDisk(true);
        // WHEN
        Boolean actualResult = underTest.isGatewayRepairEnabled(cluster);
        // THEN
        Assertions.assertTrue(actualResult);
    }

    @Test
    public void testIsGatewayRepairEnabledWhenDatabaseCrnIsGiven() {
        // GIVEN
        Cluster cluster = new Cluster();
        cluster.setEmbeddedDatabaseOnAttachedDisk(false);
        cluster.setDatabaseServerCrn("dbCrn");
        // WHEN
        Boolean actualResult = underTest.isGatewayRepairEnabled(cluster);
        // THEN
        Assertions.assertTrue(actualResult);
    }

    @Test
    public void testIsGatewayRepairEnabledWhenUserManagedRdsConfigsAreGiven() {
        // GIVEN
        Cluster cluster = new Cluster();
        cluster.setId(0L);
        cluster.setEmbeddedDatabaseOnAttachedDisk(false);
        Set<RDSConfig> rdsConfigs = Set.of(createRdsConfig(0L, ResourceStatus.USER_MANAGED, DatabaseType.CLOUDERA_MANAGER),
                createRdsConfig(1L, ResourceStatus.USER_MANAGED, DatabaseType.CLOUDERA_MANAGER_MANAGEMENT_SERVICE_REPORTS_MANAGER));
        when(rdsConfigService.findByClusterId(0L)).thenReturn(rdsConfigs);
        // WHEN
        Boolean actualResult = underTest.isGatewayRepairEnabled(cluster);
        // THEN
        Assertions.assertTrue(actualResult);
    }

    @Test
    public void testIsGatewayRepairEnabledWhenWrongTypeOfUserManagedRdsConfigsAreGiven() {
        // GIVEN
        Cluster cluster = new Cluster();
        cluster.setId(0L);
        cluster.setEmbeddedDatabaseOnAttachedDisk(false);
        Set<RDSConfig> rdsConfigs = Set.of(createRdsConfig(0L, ResourceStatus.USER_MANAGED, DatabaseType.CLOUDERA_MANAGER),
                createRdsConfig(1L, ResourceStatus.USER_MANAGED, DatabaseType.HIVE));
        when(rdsConfigService.findByClusterId(0L)).thenReturn(rdsConfigs);
        // WHEN
        Boolean actualResult = underTest.isGatewayRepairEnabled(cluster);
        // THEN
        Assertions.assertFalse(actualResult);
    }

    @Test
    public void testIsGatewayRepairEnabledShouldReturnFalseWhenDatabaseIsOnRootDisk() {
        // GIVEN
        Cluster cluster = new Cluster();
        cluster.setEmbeddedDatabaseOnAttachedDisk(false);
        // WHEN
        Boolean actualResult = underTest.isGatewayRepairEnabled(cluster);
        // THEN
        Assertions.assertFalse(actualResult);
    }

    private RDSConfig createRdsConfig(Long id, ResourceStatus resourceStatus, DatabaseType databaseType) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(id);
        rdsConfig.setStatus(resourceStatus);
        rdsConfig.setType(databaseType.name());
        return rdsConfig;
    }
}
