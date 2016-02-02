package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.util.JsonUtil.readValue;
import static com.sequenceiq.cloudbreak.util.JsonUtil.writeValueAsString;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.transaction.Transactional;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.ConstraintRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class ClusterTerminationService {

    private static final String DELIMITER = "_";

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Resource
    private Map<FileSystemType, FileSystemConfigurator> fileSystemConfigurators;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private ConstraintRepository constraintRepository;

    public void finalizeClusterTermination(Long clusterId) {
        Cluster cluster = clusterRepository.findById(clusterId);
        String terminatedName = cluster.getName() + DELIMITER + new Date().getTime();
        cluster.setName(terminatedName);
        cluster.setBlueprint(null);
        clusterRepository.save(cluster);
        for (HostGroup hostGroup : hostGroupRepository.findHostGroupsInCluster(cluster.getId())) {
            hostGroup.getRecipes().clear();
            hostGroup.getHostMetadata().clear();
            Long constraintId = hostGroup.getConstraint().getId();
            hostGroup.setConstraint(null);
            hostGroupRepository.save(hostGroup);
            constraintRepository.delete(constraintId);
        }
        FileSystem fs = cluster.getFileSystem();
        if (fs != null) {
            deleteFileSystemResources(cluster.getStack().getId(), fs);
        }
        for (HostMetadata metadata : hostMetadataRepository.findHostsInCluster(cluster.getId())) {
            hostMetadataRepository.delete(metadata.getId());
        }

    }

    private Map<String, String> deleteFileSystemResources(Long stackId, FileSystem fileSystem) {
        try {
            FileSystemConfigurator fsConfigurator = fileSystemConfigurators.get(FileSystemType.valueOf(fileSystem.getType()));
            String json = writeValueAsString(fileSystem.getProperties());
            FileSystemConfiguration fsConfiguration = (FileSystemConfiguration) readValue(json, FileSystemType.valueOf(fileSystem.getType()).getClazz());
            fsConfiguration.addProperty(FileSystemConfiguration.STORAGE_CONTAINER, "cloudbreak" + stackId);
            return fsConfigurator.deleteResources(fsConfiguration);
        } catch (IOException e) {
            throw new TerminationFailedException("File system resources could not be deleted: ", e);
        }
    }
}
