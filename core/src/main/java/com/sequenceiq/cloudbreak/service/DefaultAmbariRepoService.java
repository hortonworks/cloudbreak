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

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;

@Service
@ConfigurationProperties("cb.ambari")
public class DefaultAmbariRepoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAmbariRepoService.class);

    @Inject
    private StackMatrixService stackMatrixService;

    private Map<String, RepositoryInfo> entries = new HashMap<>();

    public AmbariRepo getDefault(String osType) {
        for (Entry<String, RepositoryInfo> ambariEntry : entries.entrySet()) {
            RepositoryInfo repositoryInfo = ambariEntry.getValue();
            if (repositoryInfo.getRepo().get(osType) == null) {
                LOGGER.info("Missing Ambari ({}) repo information for os: {}", repositoryInfo.getVersion(), osType);
                continue;
            }
            AmbariRepo ambariRepo = new AmbariRepo();
            ambariRepo.setPredefined(Boolean.FALSE);
            ambariRepo.setVersion(repositoryInfo.getVersion());
            ambariRepo.setBaseUrl(repositoryInfo.getRepo().get(osType).getBaseurl());
            ambariRepo.setGpgKeyUrl(repositoryInfo.getRepo().get(osType).getGpgkey());
            return ambariRepo;
        }
        return null;
    }

    public AmbariRepo getDefault(String osType, String clusterType, String clusterVersion) {
        StackMatrixV4Response stackMatrixV4Response = stackMatrixService.getStackMatrix();
        Map<String, AmbariStackDescriptorV4Response> stackDescriptorMap;

        if (clusterType != null) {
            switch (clusterType) {
                case "HDP":
                    stackDescriptorMap = stackMatrixV4Response.getHdp();
                    break;
                case "HDF":
                    stackDescriptorMap = stackMatrixV4Response.getHdf();
                    break;
                default:
                    stackDescriptorMap = null;
            }
        } else {
            stackDescriptorMap = stackMatrixV4Response.getHdp();
        }

        if (stackDescriptorMap != null) {
            Optional<Entry<String, AmbariStackDescriptorV4Response>> descriptorEntry = stackDescriptorMap.entrySet().stream()
                    .filter(stackDescriptorEntry ->
                            clusterVersion == null || clusterVersion.equals(stackDescriptorEntry.getKey()))
                    .max(Comparator.comparing(Entry::getKey));
            if (descriptorEntry.isPresent()) {
                Entry<String, AmbariStackDescriptorV4Response> stackDescriptorEntry = descriptorEntry.get();
                AmbariInfoV4Response ambariInfoJson = stackDescriptorEntry.getValue().getAmbari();
                if (ambariInfoJson.getRepository().get(osType) != null) {
                    AmbariRepo ambariRepo = new AmbariRepo();
                    ambariRepo.setPredefined(false);
                    ambariRepo.setVersion(ambariInfoJson.getVersion());
                    ambariRepo.setBaseUrl(ambariInfoJson.getRepository().get(osType).getBaseUrl());
                    ambariRepo.setGpgKeyUrl(ambariInfoJson.getRepository().get(osType).getGpgKeyUrl());
                    return ambariRepo;
                }
            }
        }

        LOGGER.info("Missing Ambari repo information for os: {} clusterType: {} clusterVersion: {}", osType, clusterType, clusterVersion);
        return null;
    }

    public Map<String, RepositoryInfo> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, RepositoryInfo> entries) {
        this.entries = entries;
    }
}
