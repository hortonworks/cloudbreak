package com.sequenceiq.cloudbreak.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;

@Service
public class StackMatrixService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackMatrixService.class);

    @Inject
    private DefaultCDHEntries defaultCDHEntries;

    @Inject
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

    @Inject
    private ConverterUtil converterUtil;

    public StackMatrixV4Response getStackMatrix() {
        Map<String, DefaultCDHInfo> cdhEntries = defaultCDHEntries.getEntries();
        StackMatrixV4Response stackMatrixV4Response = new StackMatrixV4Response();

        Map<String, ClouderaManagerStackDescriptorV4Response> cdhStackDescriptors = new HashMap<>();
        for (Entry<String, DefaultCDHInfo> defaultCDHInfoEntry : cdhEntries.entrySet()) {
            DefaultCDHInfo defaultCDHInfo = defaultCDHInfoEntry.getValue();
            ClouderaManagerStackDescriptorV4Response stackDescriptorV4 = getCMStackDescriptor(defaultCDHInfo);
            cdhStackDescriptors.put(defaultCDHInfoEntry.getKey(), stackDescriptorV4);
        }

        stackMatrixV4Response.setCdh(cdhStackDescriptors);
        return stackMatrixV4Response;
    }

    public Set<String> getSupportedOperatingSystems(String clusterVersion) {
        StackMatrixV4Response stackMatrix = getStackMatrix();
        LOGGER.debug("Get Cloudera Manager stack info for determining the supported OS types for version: {}", clusterVersion);
        ClouderaManagerStackDescriptorV4Response cmStackDescriptor = stackMatrix.getCdh().get(clusterVersion);
        return cmStackDescriptor != null ? cmStackDescriptor.getClouderaManager().getRepository().keySet() : Collections.emptySet();
    }

    private ClouderaManagerStackDescriptorV4Response getCMStackDescriptor(DefaultCDHInfo stackInfo) {
        Map<String, RepositoryInfo> clouderaManagerRepoInfoEntries = defaultClouderaManagerRepoService.getEntries();
        ClouderaManagerStackDescriptorV4Response stackDescriptorV4 = converterUtil.convert(stackInfo, ClouderaManagerStackDescriptorV4Response.class);
        RepositoryInfo cmInfo = clouderaManagerRepoInfoEntries.getOrDefault(stackDescriptorV4.getMinCM(), new RepositoryInfo());
        ClouderaManagerInfoV4Response cmInfoJson = converterUtil.convert(cmInfo, ClouderaManagerInfoV4Response.class);
        stackDescriptorV4.setClouderaManager(cmInfoJson);
        return stackDescriptorV4;
    }
}
