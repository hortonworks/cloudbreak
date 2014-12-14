package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.service.stack.connector.DiskAttachUtils.buildDiskPathString;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Stack;

@Service
public class HadoopConfigurationService {

    public static final String YARN_SITE = "yarn-site";
    public static final String HDFS_SITE = "hdfs-site";
    public static final String YARN_NODEMANAGER_LOCAL_DIRS = "yarn.nodemanager.local-dirs";
    public static final String YARN_NODEMANAGER_LOG_DIRS = "yarn.nodemanager.log-dirs";
    public static final String HDFS_DATANODE_DATA_DIRS = "dfs.datanode.data.dir";

    public Map<String, Map<String, String>> getConfiguration(Stack stack) {
        Map<String, Map<String, String>> hadoopConfig = new HashMap<>();
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
