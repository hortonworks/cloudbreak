package com.sequenceiq.cloudbreak.conf;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.domain.Template;

@Configuration
@ConfigurationProperties(prefix = "cb.db.env.embedded.volume")
public class EmbeddedDatabaseConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDatabaseConfig.class);

    private Integer size;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    private Map<String, String> platformVolumeTypeMap;

    @PostConstruct
    public void log() {
        LOGGER.info("Configuration of embedded database: {}", toString());
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Map<String, String> getPlatformVolumeTypeMap() {
        return platformVolumeTypeMap;
    }

    public void setPlatformVolumeTypeMap(Map<String, String> platformVolumeTypeMap) {
        this.platformVolumeTypeMap = platformVolumeTypeMap;
    }

    public Optional<String> getPlatformVolumeType(String cloudPlatform, Template template) {
        return Optional.ofNullable(cloudPlatformConnectors.getDefault(platform(cloudPlatform))
                .parameters()
                .embeddedDatabaseDiskType(template.getInstanceType()));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EmbeddedDatabaseConfig.class.getSimpleName() + "[", "]")
                .add("size=" + size)
                .add("platformVolumeTypeMap=" + platformVolumeTypeMap)
                .toString();
    }
}
