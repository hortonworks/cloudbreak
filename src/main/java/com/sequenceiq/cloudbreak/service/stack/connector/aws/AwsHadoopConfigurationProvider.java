package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.HadoopConfigurationProvider;
import com.sequenceiq.cloudbreak.service.stack.connector.LocalDirBuilderService;

@Service
public class AwsHadoopConfigurationProvider implements HadoopConfigurationProvider {

    @Autowired
    private LocalDirBuilderService localDirBuilderService;

    @Override
    public Map<String, String> getYarnSiteConfigs(Stack stack) {
        Map<String, String> yarnSiteConfigs = new HashMap<>();
        AwsTemplate template = (AwsTemplate) stack.getTemplate();
        if (template.getVolumeCount() > 0) {
            yarnSiteConfigs.put(YARN_NODEMANAGER_LOCAL_DIRS, localDirBuilderService.buildLocalDirs(template.getVolumeCount()));
        }
        return yarnSiteConfigs;
    }

    @Override
    public Map<String, String> getHdfsSiteConfigs(Stack stack) {
        Map<String, String> hdfsSiteConfigs = new HashMap<>();
        AwsTemplate template = (AwsTemplate) stack.getTemplate();
        if (template.getVolumeCount() > 0) {
            hdfsSiteConfigs.put(HDFS_DATANODE_DATA_DIRS, localDirBuilderService.buildLocalDirs(template.getVolumeCount()));
        }
        return hdfsSiteConfigs;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

}
