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
import com.sequenceiq.cloudbreak.api.model.stack.StackDescriptor;
import com.sequenceiq.cloudbreak.api.model.stack.StackMatrix;
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
        return getDefault(osType, null, null);
    }

    public AmbariRepo getDefault(String osType, String clusterType, String clusterVersion) {
        StackMatrix stackMatrix = stackMatrixService.getStackMatrix();
        Map<String, StackDescriptor> stackDescriptorMap;

        if (clusterType != null) {
            switch (clusterType) {
                case "HDP":
                    stackDescriptorMap = stackMatrix.getHdp();
                    break;
                case "HDF":
                    stackDescriptorMap = stackMatrix.getHdf();
                    break;
                default:
                    stackDescriptorMap = stackMatrix.getHdp();
            }
        } else {
            stackDescriptorMap = stackMatrix.getHdp();
        }

        if (stackDescriptorMap != null) {
            Optional<Entry<String, StackDescriptor>> descriptorEntry = stackDescriptorMap.entrySet().stream()
                    .filter(stackDescriptorEntry ->
                            clusterVersion == null || clusterVersion.equals(stackDescriptorEntry.getKey()))
                    .max(Comparator.comparing(Entry::getKey));
            if (descriptorEntry.isPresent()) {
                Entry<String, StackDescriptor> stackDescriptorEntry = descriptorEntry.get();
                AmbariInfoJson ambariInfoJson = stackDescriptorEntry.getValue().getAmbari();
                AmbariRepo ambariRepo = new AmbariRepo();
                ambariRepo.setPredefined(false);
                ambariRepo.setVersion(ambariInfoJson.getVersion());
                ambariRepo.setBaseUrl(ambariInfoJson.getRepo().get(osType).getBaseUrl());
                ambariRepo.setGpgKeyUrl(ambariInfoJson.getRepo().get(osType).getGpgKeyUrl());
                return ambariRepo;
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
