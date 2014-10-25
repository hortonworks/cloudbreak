package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.LocalDirBuilderService;

@Service
public class HadoopConfigurationService {

    public static final String YARN_SITE = "yarn-site";
    public static final String HDFS_SITE = "hdfs-site";
    public static final String YARN_NODEMANAGER_LOCAL_DIRS = "yarn.nodemanager.local-dirs";
    public static final String YARN_NODEMANAGER_LOG_DIRS = "yarn.nodemanager.log-dirs";
    public static final String HDFS_DATANODE_DATA_DIRS = "dfs.datanode.data.dir";

    @Autowired
    private LocalDirBuilderService localDirBuilderService;

    public Map<String, Map<String, String>> getConfiguration(Stack stack) {
        Map<String, Map<String, String>> hadoopConfig = new HashMap<>();
        int volumeCount = stack.getTemplate().getVolumeCount();
        if (volumeCount > 0) {
            String localDirs = localDirBuilderService.buildLocalDirs(volumeCount);
            hadoopConfig.put(YARN_SITE, getYarnSiteConfigs(localDirs));
            hadoopConfig.put(HDFS_SITE, getHDFSSiteConfigs(localDirs));
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
