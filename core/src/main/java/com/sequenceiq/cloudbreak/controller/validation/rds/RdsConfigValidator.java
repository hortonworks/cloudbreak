package com.sequenceiq.cloudbreak.controller.validation.rds;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Component
public class RdsConfigValidator {

    @Inject
    private RdsConfigService rdsConfigService;

    public void validateRdsConfigs(ClusterV4Request request, User user, Workspace workspace) {
        if (request.getDatabases() != null) {
            Set<String> multipleTypes = request.getDatabases().stream()
                    .map(rdsConfigName -> rdsConfigService.getByNameForWorkspace(rdsConfigName, workspace))
                    .collect(groupingBy(RDSConfig::getType, counting()))
                    .entrySet().stream()
                    .filter(e -> e.getValue() > 1L)
                    .map(Map.Entry::getKey)
                    .collect(toCollection(TreeSet::new));
            if (!multipleTypes.isEmpty()) {
                throw new BadRequestException("Multiple databases are defined for the following types: "
                        + String.join(",", multipleTypes));
            }
        }
    }
}
