package com.sequenceiq.cloudbreak.service.stack.connector;

import java.util.Map;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface HadoopConfigurationProvider {

    String YARN_NODEMANAGER_LOCAL_DIRS = "yarn.nodemanager.local-dirs";
    String HDFS_DATANODE_DATA_DIRS = "dfs.datanode.data.dir";

    Map<String, String> getYarnSiteConfigs(Stack stack);

    Map<String, String> getHdfsSiteConfigs(Stack stack);

    CloudPlatform getCloudPlatform();

}
