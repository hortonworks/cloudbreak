package com.sequenceiq.cloudbreak.conf;

import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cb.db.env.embedded.volume")
public class EmbeddedDatabaseConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDatabaseConfig.class);

    private Integer size;

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

    public Optional<String> getPlatformVolumeType(String cloudPlatform) {
        return Optional.ofNullable(platformVolumeTypeMap.get(cloudPlatform));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EmbeddedDatabaseConfig.class.getSimpleName() + "[", "]")
                .add("size=" + size)
                .add("platformVolumeTypeMap=" + platformVolumeTypeMap)
                .toString();
    }
}
