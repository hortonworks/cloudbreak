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

import com.sequenceiq.cloudbreak.api.model.AmbariInfoJson;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackDescriptorV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariInfo;

@Service
@ConfigurationProperties("cb.ambari")
public class DefaultAmbariRepoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAmbariRepoService.class);

    @Inject
    private StackMatrixService stackMatrixService;

    private Map<String, AmbariInfo> entries = new HashMap<>();

    public AmbariRepo getDefault(String osType) {
        for (Entry<String, AmbariInfo> ambariEntry : entries.entrySet()) {
            AmbariInfo ambariInfo = ambariEntry.getValue();
            if (ambariInfo.getRepo().get(osType) == null) {
                LOGGER.info("Missing Ambari ({}) repo information for os: {}", ambariInfo.getVersion(), osType);
                continue;
            }
            AmbariRepo ambariRepo = new AmbariRepo();
            ambariRepo.setPredefined(Boolean.FALSE);
            ambariRepo.setVersion(ambariInfo.getVersion());
            ambariRepo.setBaseUrl(ambariInfo.getRepo().get(osType).getBaseurl());
            ambariRepo.setGpgKeyUrl(ambariInfo.getRepo().get(osType).getGpgkey());
            return ambariRepo;
        }
        return null;
    }

    public AmbariRepo getDefault(String osType, String clusterType, String clusterVersion) {
        StackMatrixV4 stackMatrixV4 = stackMatrixService.getStackMatrix();
        Map<String, StackDescriptorV4> stackDescriptorMap;

        if (clusterType != null) {
            switch (clusterType) {
                case "HDP":
                    stackDescriptorMap = stackMatrixV4.getHdp();
                    break;
                case "HDF":
                    stackDescriptorMap = stackMatrixV4.getHdf();
                    break;
                default:
                    stackDescriptorMap = null;
            }
        } else {
            stackDescriptorMap = stackMatrixV4.getHdp();
        }

        if (stackDescriptorMap != null) {
            Optional<Entry<String, StackDescriptorV4>> descriptorEntry = stackDescriptorMap.entrySet().stream()
                    .filter(stackDescriptorEntry ->
                            clusterVersion == null || clusterVersion.equals(stackDescriptorEntry.getKey()))
                    .max(Comparator.comparing(Entry::getKey));
            if (descriptorEntry.isPresent()) {
                Entry<String, StackDescriptorV4> stackDescriptorEntry = descriptorEntry.get();
                AmbariInfoJson ambariInfoJson = stackDescriptorEntry.getValue().getAmbari();
                if (ambariInfoJson.getRepo().get(osType) != null) {
                    AmbariRepo ambariRepo = new AmbariRepo();
                    ambariRepo.setPredefined(false);
                    ambariRepo.setVersion(ambariInfoJson.getVersion());
                    ambariRepo.setBaseUrl(ambariInfoJson.getRepo().get(osType).getBaseUrl());
                    ambariRepo.setGpgKeyUrl(ambariInfoJson.getRepo().get(osType).getGpgKeyUrl());
                    return ambariRepo;
                }
            }
        }

        LOGGER.info("Missing Ambari repo information for os: {} clusterType: {} clusterVersion: {}", osType, clusterType, clusterVersion);
        return null;
    }

    public Map<String, AmbariInfo> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, AmbariInfo> entries) {
        this.entries = entries;
    }
}
