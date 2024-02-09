package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.repository.RdsConfigWithoutClusterRepository;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Component
public class RdsConfigWithoutClusterService {

    @Inject
    private RdsConfigWithoutClusterRepository rdsConfigWithoutClusterRepository;

    public RdsConfigWithoutCluster findByClusterIdAndType(Long clusterId, DatabaseType databaseType) {
        return rdsConfigWithoutClusterRepository.findByClusterIdAndType(clusterId, databaseType.name());
    }

    public Set<RdsConfigWithoutCluster> findByClusterId(Long clusterId) {
        return rdsConfigWithoutClusterRepository.findByClusterId(clusterId);
    }

    public long countByClusterIdAndStatusInAndTypeIn(Long id, Set<ResourceStatus> statuses, Set<DatabaseType> databaseTypes) {
        return rdsConfigWithoutClusterRepository.countByClusterIdAndStatusAndTypeIn(id, statuses,
                databaseTypes.stream().map(Enum::name).collect(Collectors.toSet()));
    }

    public List<RdsConfigWithoutCluster> findByClusterIdAndStatusInAndTypeIn(Long id, Set<ResourceStatus> statuses, Set<DatabaseType> databaseTypes) {
        return rdsConfigWithoutClusterRepository.findByClusterIdAndStatusInAndTypeIn(id, statuses,
                databaseTypes.stream().map(Enum::name).collect(Collectors.toSet()));
    }

    public List<RdsConfigWithoutCluster> findByClusterIdAndStatusIn(Long id, Set<ResourceStatus> statuses) {
        return rdsConfigWithoutClusterRepository.findByClusterIdAndStatusIn(id, statuses);
    }

    public Set<RdsConfigWithoutCluster> findAllByNamesAndWorkspaceId(Set<String> names, Workspace workspace) {
        return CollectionUtils.isEmpty(names) ? new HashSet<>() : rdsConfigWithoutClusterRepository.findAllByNamesAndWorkspaceId(names, workspace.getId());
    }

    public void saveRdsConfigWithoutCluster(RDSConfig rdsConfig) {
        rdsConfigWithoutClusterRepository.save(rdsConfig);
    }
}
