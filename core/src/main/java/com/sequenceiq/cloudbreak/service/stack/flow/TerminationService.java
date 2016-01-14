package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.util.JsonUtil.readValue;
import static com.sequenceiq.cloudbreak.util.JsonUtil.writeValueAsString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;

@Service
@Transactional
public class TerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationService.class);
    private static final String DELIMITER = "_";

    @Inject
    private ServiceProviderConnectorAdapter connector;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Resource
    private Map<FileSystemType, FileSystemConfigurator> fileSystemConfigurators;

    public void finalizeTermination(Long stackId, boolean force) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        try {
            Date now = new Date();
            String terminatedName = stack.getName() + DELIMITER + now.getTime();
            Cluster cluster = stack.getCluster();
            if (!force && cluster != null) {
                throw new TerminationFailedException(String.format("There is a cluster installed on stack '%s', terminate it first!.", stackId ));
            } else if (cluster!=null) {
                cluster.setName(terminatedName);
                cluster.setBlueprint(null);
                cluster.setSssdConfig(null);
                clusterRepository.save(cluster);
                for (HostGroup hostGroup : hostGroupRepository.findHostGroupsInCluster(cluster.getId())) {
                    hostGroup.getRecipes().clear();
                    hostGroup.getHostMetadata().clear();
                    hostGroupRepository.save(hostGroup);
                }
                FileSystem fs = cluster.getFileSystem();
                if (fs != null) {
                    deleteFileSystemResources(stackId, fs);
                }
                for (HostMetadata metadata : hostMetadataRepository.findHostsInCluster(stack.getCluster().getId())) {
                    hostMetadataRepository.delete(metadata.getId());
                }
            }
            stack.setCredential(null);
            stack.setNetwork(null);
            stack.setSecurityGroup(null);
            stack.setName(terminatedName);
            terminateMetaDataInstances(stack);
            stackRepository.save(stack);
            stackUpdater.updateStackStatus(stackId, DELETE_COMPLETED, "Stack was terminated successfully.");
        } catch (Exception ex) {
            LOGGER.error("Failed to terminate cluster infrastructure. Stack id {}", stack.getId());
            throw new TerminationFailedException(ex);
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

    private void terminateMetaDataInstances(Stack stack) {
        List<InstanceMetaData> instanceMetaDatas = new ArrayList<>();
        for (InstanceMetaData metaData : stack.getRunningInstanceMetaData()) {
            long timeInMillis = Calendar.getInstance().getTimeInMillis();
            metaData.setTerminationDate(timeInMillis);
            metaData.setInstanceStatus(InstanceStatus.TERMINATED);
            instanceMetaDatas.add(metaData);
        }
        instanceMetaDataRepository.save(instanceMetaDatas);
    }
}