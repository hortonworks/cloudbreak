package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.service.upgrade.BlockedUpgradePath;

@Configuration
public class UpgradePathRestrictionsConfig {

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Bean
    public List<BlockedUpgradePath> blockedUpgradePaths() throws IOException {
        String json = cloudbreakResourceReaderService.resourceDefinition("upgrade-path-restrictions");
        return JsonUtil.readValue(json, new TypeReference<>() { });
    }
}
