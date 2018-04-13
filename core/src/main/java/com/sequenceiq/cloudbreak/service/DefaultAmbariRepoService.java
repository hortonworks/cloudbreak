package com.sequenceiq.cloudbreak.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariInfo;

@Service
@ConfigurationProperties("cb.ambari")
public class DefaultAmbariRepoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAmbariRepoService.class);

    private Map<String, AmbariInfo> entries = new HashMap<>();

    public AmbariRepo getDefault(String osType) {
        AmbariInfo ambariInfo = entries.get(entries.keySet().iterator().next());
        if (ambariInfo.getRepo().get(osType) == null) {
            LOGGER.error(String.format("Missing Ambari (%s) repo information for os: %s", ambariInfo.getVersion(), osType));
            return null;
        }
        AmbariRepo ambariRepo = new AmbariRepo();
        ambariRepo.setPredefined(Boolean.FALSE);
        ambariRepo.setVersion(ambariInfo.getVersion());
        ambariRepo.setBaseUrl(ambariInfo.getRepo().get(osType).getBaseurl());
        ambariRepo.setGpgKeyUrl(ambariInfo.getRepo().get(osType).getGpgkey());
        return ambariRepo;
    }

    public Map<String, AmbariInfo> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, AmbariInfo> entries) {
        this.entries = entries;
    }
}
