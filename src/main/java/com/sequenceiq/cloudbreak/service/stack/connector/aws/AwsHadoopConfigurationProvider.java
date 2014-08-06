package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.HadoopConfigurationProvider;

@Service
public class AwsHadoopConfigurationProvider implements HadoopConfigurationProvider {

    @Override
    public Map<String, String> getYarnSiteConfigs(Stack stack) {
        Map<String, String> yarnSiteConfigs = new HashMap<>();
        AwsTemplate template = (AwsTemplate) stack.getTemplate();
        if (template.getVolumeCount() > 0) {
            yarnSiteConfigs.put(YARN_NODEMANAGER_LOCAL_DIRS, localDirs(template.getVolumeCount()));
        }
        return yarnSiteConfigs;
    }

    @Override
    public Map<String, String> getHdfsSiteConfigs(Stack stack) {
        Map<String, String> hdfsSiteConfigs = new HashMap<>();
        AwsTemplate template = (AwsTemplate) stack.getTemplate();
        if (template.getVolumeCount() > 0) {
            hdfsSiteConfigs.put(HDFS_DATANODE_DATA_DIRS, localDirs(template.getVolumeCount()));
        }
        return hdfsSiteConfigs;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    private String localDirs(Integer volumeCount) {
        StringBuilder localDirs = new StringBuilder("");
        for (int i = 1; i <= volumeCount; i++) {
            localDirs.append("/mnt/fs").append(i);
            if (i != volumeCount) {
                localDirs.append(",");
            }
        }
        return localDirs.toString();
    }

}
