package com.sequenceiq.cloudbreak.service.stack.flow;

import javax.annotation.Resource;
import javax.inject.Inject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.CloudPlatformResolver;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationService.class);
    private static final String DELIMITER = "_";

    @Inject
    private CloudPlatformResolver platformResolver;

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

    @Resource
    private Map<FileSystemType, FileSystemConfigurator> fileSystemConfigurators;

    public void terminateStack(Long stackId, CloudPlatform cloudPlatform) {
        final Stack stack = stackRepository.findOneWithLists(stackId);
        try {
            platformResolver.connector(cloudPlatform).deleteStack(stack, stack.getCredential());
        } catch (Exception ex) {
            LOGGER.error("Failed to terminate cluster infrastructure. Stack id {}", stack.getId());
            throw new TerminationFailedException(ex);
        }
    }

    public void finalizeTermination(Long stackId) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        try {
            Date now = new Date();
            String terminatedName = stack.getName() + DELIMITER + now.getTime();
            Cluster cluster = stack.getCluster();
            if (cluster != null) {
                cluster.setName(terminatedName);
                cluster.setBlueprint(null);
                clusterRepository.save(cluster);
                for (HostGroup hostGroup : hostGroupRepository.findHostGroupsInCluster(cluster.getId())) {
                    hostGroup.getRecipes().clear();
                    hostGroupRepository.save(hostGroup);
                }
                deleteFileSystemResources(stackId, cluster.getFileSystem());
            }
            stack.setCredential(null);
            stack.setNetwork(null);
            stack.setSecurityGroup(null);
            stack.setName(terminatedName);
            instanceMetaDataRepository.save(terminateMetaDataInstances(stack));
            stackRepository.save(stack);
        } catch (Exception ex) {
            LOGGER.error("Failed to terminate cluster infrastructure. Stack id {}", stack.getId());
            throw new TerminationFailedException(ex);
        }
    }

    private Map<String, String> deleteFileSystemResources(Long stackId, FileSystem fileSystem) {
        try {
            FileSystemConfigurator fsConfigurator = fileSystemConfigurators.get(FileSystemType.valueOf(fileSystem.getType()));
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(fileSystem.getProperties());
            FileSystemConfiguration fsConfiguration = (FileSystemConfiguration) mapper.readValue(json, FileSystemType.valueOf(fileSystem.getType()).getClazz());
            fsConfiguration.addProperty(FileSystemConfiguration.STORAGE_CONTAINER, "cloudbreak" + stackId);
            return fsConfigurator.deleteResources(fsConfiguration);
        } catch (IOException e) {
            throw new TerminationFailedException("File system resources could not be deleted: ", e);
        }
    }

    private List<InstanceMetaData> terminateMetaDataInstances(Stack stack) {
        List<InstanceMetaData> instanceMetaDatas = new ArrayList<>();
        for (InstanceMetaData metaData : stack.getRunningInstanceMetaData()) {
            long timeInMillis = Calendar.getInstance().getTimeInMillis();
            metaData.setTerminationDate(timeInMillis);
            metaData.setInstanceStatus(InstanceStatus.TERMINATED);
            instanceMetaDatas.add(metaData);
        }
        return instanceMetaDatas;
    }
}