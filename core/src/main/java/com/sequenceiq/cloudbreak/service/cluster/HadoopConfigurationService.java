package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.service.stack.connector.DiskAttachUtils.buildDiskPathString;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;

@Service
public class HadoopConfigurationService {

    public static final String YARN_SITE = "yarn-site";
    public static final String HDFS_SITE = "hdfs-site";
    public static final String YARN_NODEMANAGER_LOCAL_DIRS = "yarn.nodemanager.local-dirs";
    public static final String YARN_NODEMANAGER_LOG_DIRS = "yarn.nodemanager.log-dirs";
    public static final String HDFS_DATANODE_DATA_DIRS = "dfs.datanode.data.dir";

    @Autowired
    private HostGroupRepository hostGroupRepository;

    public Map<String, Map<String, Map<String, String>>> getConfiguration(Stack stack) {
        Set<HostGroup> hostGroups = hostGroupRepository.findHostGroupsInCluster(stack.getCluster().getId());
        Map<String, Map<String, Map<String, String>>> hadoopConfig = new HashMap<>();
        for (HostGroup hostGroup : hostGroups) {
            Map<String, Map<String, String>> tmpConfig = new HashMap<>();
            int volumeCount = hostGroup.getInstanceGroup().getTemplate().getVolumeCount();
            if (volumeCount > 0) {
                tmpConfig.put(YARN_SITE, getYarnSiteConfigs(buildDiskPathString(volumeCount, "nodemanager")));
                tmpConfig.put(HDFS_SITE, getHDFSSiteConfigs(buildDiskPathString(volumeCount, "datanode")));
                hadoopConfig.put(hostGroup.getName(), tmpConfig);
            }
        }
        return hadoopConfig;
    }

    private Map<String, String> getYarnSiteConfigs(String localDirs) {
        Map<String, String> yarnConfigs = new HashMap<>();
        yarnConfigs.put(YARN_NODEMANAGER_LOCAL_DIRS, localDirs);
        yarnConfigs.put(YARN_NODEMANAGER_LOG_DIRS, localDirs);
        return yarnConfigs;
    }

    private Map<String, String> getHDFSSiteConfigs(String localDirs) {
        return Collections.singletonMap(HDFS_DATANODE_DATA_DIRS, localDirs);
    }
}
