package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;

@ExtendWith(MockitoExtension.class)
public class ClusterDBValidationServiceTest {
    @Mock
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

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
        assertTrue(actualResult);
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
        assertTrue(actualResult);
    }

    @Test
    public void testIsGatewayRepairEnabledWhenUserManagedRdsConfigsAreGiven() {
        // GIVEN
        Cluster cluster = new Cluster();
        cluster.setId(0L);
        cluster.setEmbeddedDatabaseOnAttachedDisk(false);
        when(rdsConfigWithoutClusterService.countByClusterIdAndStatusInAndTypeIn(0L, Set.of(ResourceStatus.USER_MANAGED),
                Set.of(DatabaseType.CLOUDERA_MANAGER))).thenReturn(1L);
        // WHEN
        Boolean actualResult = underTest.isGatewayRepairEnabled(cluster);
        // THEN
        assertTrue(actualResult);
    }

    @Test
    public void testIsGatewayRepairEnabledWhenWrongTypeOfUserManagedRdsConfigsAreGiven() {
        // GIVEN
        Cluster cluster = new Cluster();
        cluster.setId(0L);
        cluster.setEmbeddedDatabaseOnAttachedDisk(false);
        when(rdsConfigWithoutClusterService.countByClusterIdAndStatusInAndTypeIn(0L, Set.of(ResourceStatus.USER_MANAGED),
                Set.of(DatabaseType.CLOUDERA_MANAGER))).thenReturn(0L);
        // WHEN
        Boolean actualResult = underTest.isGatewayRepairEnabled(cluster);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsGatewayRepairEnabledShouldReturnFalseWhenDatabaseIsOnRootDisk() {
        // GIVEN
        Cluster cluster = new Cluster();
        cluster.setEmbeddedDatabaseOnAttachedDisk(false);
        // WHEN
        Boolean actualResult = underTest.isGatewayRepairEnabled(cluster);
        // THEN
        assertFalse(actualResult);
    }

    private RdsConfigWithoutCluster createRdsConfig(Long id, ResourceStatus resourceStatus, DatabaseType databaseType) {
        RdsConfigWithoutCluster rdsConfig = mock(RdsConfigWithoutCluster.class);
        when(rdsConfig.getId()).thenReturn(id);
        when(rdsConfig.getStatus()).thenReturn(resourceStatus);
        when(rdsConfig.getType()).thenReturn(databaseType.name());
        return rdsConfig;
    }
}
