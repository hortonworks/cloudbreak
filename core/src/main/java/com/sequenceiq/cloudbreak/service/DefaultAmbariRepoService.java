package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@ConfigurationProperties("cb.ambari")
public class DefaultAmbariRepoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAmbariRepoService.class);

    @Value("${cb.ambari.version}")
    private String version;

    private Map<String, Map<String, String>> repo = new HashMap<>();

    public String getVersion() {
        return version;
    }

    public AmbariRepo getDefault(String osType) {
        if (repo.get(osType) == null) {
            LOGGER.error(String.format("Missing Ambari (%s) repo information for os: %s", version, osType));
            return null;
        }
        AmbariRepo ambariRepo = new AmbariRepo();
        ambariRepo.setPredefined(Boolean.FALSE);
        ambariRepo.setVersion(version);
        ambariRepo.setBaseUrl(repo.get(osType).get("baseurl"));
        ambariRepo.setGpgKeyUrl(repo.get(osType).get("gpgkey"));
        return ambariRepo;
    }

    public Map<String, Map<String, String>> getRepo() {
        return repo;
    }

    public void setRepo(Map<String, Map<String, String>> repo) {
        this.repo = repo;
    }
}
