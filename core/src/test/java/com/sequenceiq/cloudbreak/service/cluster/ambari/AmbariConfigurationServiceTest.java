package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.converter.mapper.AmbariDatabaseMapper;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@RunWith(MockitoJUnitRunner.class)
public class AmbariConfigurationServiceTest {

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("URF_UNREAD_FIELD")
    private final AmbariDatabaseMapper ambariDatabaseMapper = Mappers.getMapper(AmbariDatabaseMapper.class);

    @InjectMocks
    private AmbariConfigurationService ambariConfigurationService;

    @Test
    public void testRdsConfigNotNeeded() {
        Cluster cluster = new Cluster();
        cluster.setRdsConfigs(new HashSet<>());
        RDSConfig config = new RDSConfig();
        config.setType(RdsType.AMBARI.name());
        cluster.getRdsConfigs().add(config);
        Optional<RDSConfig> rdsConfig = ambariConfigurationService.createDefaultRdsConfigIfNeeded(null, cluster);
        assertFalse(rdsConfig.isPresent());
    }

    @Test
    public void testCreateRdsConfig() {
        ReflectionTestUtils.setField(ambariConfigurationService, "databaseEngine", "MYSQL");
        Cluster cluster = new Cluster();
        Stack stack = new Stack();
        when(rdsConfigService.create(any(RDSConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Optional<RDSConfig> rdsConfig = ambariConfigurationService.createDefaultRdsConfigIfNeeded(stack, cluster);
        assertTrue(rdsConfig.isPresent());
    }
}