package com.sequenceiq.cloudbreak.controller.validation.rds;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class RdsConfigValidatorTest {

    @InjectMocks
    private RdsConfigValidator subject = new RdsConfigValidator();

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private Workspace workspace;

    @Test
    void acceptsNoDatabases() {
        ClusterV4Request request = requestWithDatabases();

        subject.validateRdsConfigs(request, null, workspace);
    }

    @Test
    void acceptsMultipleDatabasesOfDifferentType() {
        ClusterV4Request request = requestWithDatabases(DatabaseType.HIVE, DatabaseType.HUE, DatabaseType.RANGER);

        subject.validateRdsConfigs(request, null, workspace);
    }

    @Test
    void rejectsMultipleDatabasesOfSameType() {
        ClusterV4Request request = requestWithDatabases(DatabaseType.HIVE, DatabaseType.HUE, DatabaseType.HIVE, DatabaseType.RANGER, DatabaseType.RANGER);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> subject.validateRdsConfigs(request, null, workspace));
        assertTrue(exception.getMessage().contains("HIVE"));
        assertFalse(exception.getMessage().contains("HUE"));
        assertTrue(exception.getMessage().contains("RANGER"));
    }

    private ClusterV4Request requestWithDatabases(DatabaseType... databaseTypes) {
        List<RDSConfig> rdsConfigs = Arrays.stream(databaseTypes)
                .map(TestUtil::rdsConfig)
                .collect(toList());

        rdsConfigs.forEach(each -> when(rdsConfigService.getByNameForWorkspace(each.getName(), workspace)).thenReturn(each));

        ClusterV4Request request = new ClusterV4Request();
        request.setDatabases(rdsConfigs.stream().map(RDSConfig::getName).collect(toSet()));

        return request;
    }

}
