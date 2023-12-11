package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.rdsconfig.AbstractRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@ExtendWith(MockitoExtension.class)
class RdsConfigUpdateServiceTest {

    private static final String USER_NAME = "originalUserName";

    private static final long CLUSTER_ID = 345L;

    @InjectMocks
    private RdsConfigUpdateService underTest;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private AbstractRdsConfigProvider rdsConfigProvider1;

    @Mock
    private AbstractRdsConfigProvider rdsConfigProvider2;

    @Mock
    private AbstractRdsConfigProvider rdsConfigProvider3;

    @Mock
    private StackDto stackDto;

    @Captor
    private ArgumentCaptor<Set<RDSConfig>> argumentCaptor;

    @BeforeEach
    void before() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "rdsConfigProviders", Set.of(rdsConfigProvider1, rdsConfigProvider2, rdsConfigProvider3), true);
        when(rdsConfigProvider1.getRdsType()).thenReturn(DatabaseType.CLOUDERA_MANAGER);
        when(rdsConfigProvider2.getRdsType()).thenReturn(DatabaseType.HIVE);
        when(rdsConfigProvider3.getRdsType()).thenReturn(DatabaseType.NIFIREGISTRY);
    }

    @Test
    void testUpdateRdsConnectionUserNameShouldUpdateOnlyTheRequiredUserNamesWhenTheStackTypeIsDataHub() {
        setUpStack(WORKLOAD);
        RDSConfig rdsConfig1 = createRDSConfig(0L, DatabaseType.CLOUDERA_MANAGER, USER_NAME + "@cuttable");
        RDSConfig rdsConfig2 = createRDSConfig(1L, DatabaseType.HIVE, USER_NAME);
        RDSConfig rdsConfig3 = createRDSConfig(2L, DatabaseType.NIFIREGISTRY, USER_NAME + "@cuttable");
        RDSConfig rdsConfig4 = createRDSConfig(3L, DatabaseType.HUE, USER_NAME + "@cuttable");

        when(rdsConfigService.getClustersUsingResource(rdsConfig1)).thenReturn(Set.of(mock(Cluster.class)));
        when(rdsConfigService.getClustersUsingResource(rdsConfig2)).thenReturn(Set.of(mock(Cluster.class), mock(Cluster.class)));
        when(rdsConfigService.getClustersUsingResource(rdsConfig3)).thenReturn(Set.of(mock(Cluster.class)));

        when(rdsConfigService.findByClusterId(CLUSTER_ID)).thenReturn(Set.of(rdsConfig1, rdsConfig2, rdsConfig3, rdsConfig4));

        underTest.updateRdsConnectionUserName(stackDto);

        verify(rdsConfigService).findByClusterId(CLUSTER_ID);
        verify(rdsConfigService, times(1)).pureSaveAll(argumentCaptor.capture());
        Set<RDSConfig> rdsConfigsToSave = argumentCaptor.getValue();
        assertEquals(2, rdsConfigsToSave.size());
        Iterator<RDSConfig> iterator = rdsConfigsToSave.iterator();
        assertEquals(USER_NAME, iterator.next().getConnectionUserName());
        assertEquals(USER_NAME, iterator.next().getConnectionUserName());
        assertFalse(iterator.hasNext());
    }

    @Test
    void testUpdateRdsConnectionUserNameShouldUpdateOnlyTheRequiredUserNamesWhenTheStackTypeIsDataLake() {
        setUpStack(DATALAKE);
        RDSConfig rdsConfig1 = createRDSConfig(0L, DatabaseType.CLOUDERA_MANAGER, USER_NAME + "@cuttable");
        RDSConfig rdsConfig2 = createRDSConfig(1L, DatabaseType.HIVE, USER_NAME);
        RDSConfig rdsConfig3 = createRDSConfig(2L, DatabaseType.NIFIREGISTRY, USER_NAME + "@cuttable");
        RDSConfig rdsConfig4 = createRDSConfig(3L, DatabaseType.HUE, USER_NAME + "@cuttable");

        when(rdsConfigService.findByClusterId(CLUSTER_ID)).thenReturn(Set.of(rdsConfig1, rdsConfig2, rdsConfig3, rdsConfig4));

        underTest.updateRdsConnectionUserName(stackDto);

        verify(rdsConfigService).findByClusterId(CLUSTER_ID);
        verify(rdsConfigService, times(1)).pureSaveAll(argumentCaptor.capture());
        Set<RDSConfig> rdsConfigsToSave = argumentCaptor.getValue();
        assertEquals(3, rdsConfigsToSave.size());
        Iterator<RDSConfig> iterator = rdsConfigsToSave.iterator();
        assertEquals(USER_NAME, iterator.next().getConnectionUserName());
        assertEquals(USER_NAME, iterator.next().getConnectionUserName());
        assertEquals(USER_NAME, iterator.next().getConnectionUserName());
        assertFalse(iterator.hasNext());
    }

    private RDSConfig createRDSConfig(long id, DatabaseType databaseType, String connectionUserName) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(id);
        rdsConfig.setName("rds-config-" + id);
        rdsConfig.setType(databaseType.name());
        rdsConfig.setConnectionUserName(connectionUserName);
        return rdsConfig;
    }

    private void setUpStack(StackType stackType) {
        Stack stack = new Stack();
        stack.setType(stackType);
        when(stackDto.getStack()).thenReturn(stack);
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        when(stackDto.getCluster()).thenReturn(cluster);
    }

}