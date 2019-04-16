package com.sequenceiq.cloudbreak.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;

@Service
@ConfigurationProperties("cb.clouderamanager")
public class DefaultClouderaManagerRepoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClouderaManagerRepoService.class);

    @Inject
    private StackMatrixService stackMatrixService;

    private Map<String, RepositoryInfo> entries = new HashMap<>();

    public ClouderaManagerRepo getDefault(String osType) {
        for (Entry<String, RepositoryInfo> clouderaManagerInfoEntry : entries.entrySet()) {
            RepositoryInfo clouderaManagerInfo = clouderaManagerInfoEntry.getValue();
            if (clouderaManagerInfo.getRepo().get(osType) == null) {
                LOGGER.info("Missing Cloudera Manager ({}) repo information for os: {}", clouderaManagerInfo.getVersion(), osType);
                continue;
            }
            ClouderaManagerRepo repository = new ClouderaManagerRepo();
            repository.setPredefined(Boolean.FALSE);
            repository.setVersion(clouderaManagerInfo.getVersion());
            repository.setBaseUrl(clouderaManagerInfo.getRepo().get(osType).getBaseurl());
            repository.setGpgKeyUrl(clouderaManagerInfo.getRepo().get(osType).getGpgkey());
            return repository;
        }
        return null;
    }

    public ClouderaManagerRepo getDefault(String osType, String clusterType, String clusterVersion) {
        if (StackType.CDH.name().equals(clusterType)) {
            StackMatrixV4Response stackMatrixV4Response = stackMatrixService.getStackMatrix();
            Map<String, ClouderaManagerStackDescriptorV4Response> stackDescriptorMap = stackMatrixV4Response.getCdh();

            if (stackDescriptorMap != null) {
                Optional<Entry<String, ClouderaManagerStackDescriptorV4Response>> descriptorEntry = stackDescriptorMap.entrySet().stream()
                        .filter(stackDescriptorEntry ->
                                clusterVersion == null || clusterVersion.equals(stackDescriptorEntry.getKey()))
                        .max(Comparator.comparing(Entry::getKey));
                if (descriptorEntry.isPresent()) {
                    Entry<String, ClouderaManagerStackDescriptorV4Response> stackDescriptorEntry = descriptorEntry.get();
                    ClouderaManagerInfoV4Response clouderaManagerInfoJson = stackDescriptorEntry.getValue().getClouderaManager();
                    if (clouderaManagerInfoJson.getRepository().get(osType) != null) {
                        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
                        clouderaManagerRepo.setPredefined(false);
                        clouderaManagerRepo.setVersion(clouderaManagerInfoJson.getVersion());
                        clouderaManagerRepo.setBaseUrl(clouderaManagerInfoJson.getRepository().get(osType).getBaseUrl());
                        clouderaManagerRepo.setGpgKeyUrl(clouderaManagerInfoJson.getRepository().get(osType).getGpgKeyUrl());
                        return clouderaManagerRepo;
                    }
                }
            }
        }

        LOGGER.info("Missing Cloudera Manager Repo information for os: {} clusterType: {} clusterVersion: {}", osType, clusterType, clusterVersion);
        return null;
    }

    public Map<String, RepositoryInfo> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, RepositoryInfo> entries) {
        this.entries = entries;
    }
}
