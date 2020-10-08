package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.service.upgrade.matrix.UpgradeMatrixDefinition;
import com.sequenceiq.cloudbreak.service.upgrade.matrix.UpgradeMatrixDefinitionProvider;

@Configuration
public class UpgradeMatrixConfig {

    @Inject
    private UpgradeMatrixDefinitionProvider upgradeMatrixDefinitionProvider;

    @Bean
    public UpgradeMatrixDefinition upgradeMatrixDefinition() throws IOException {
        return upgradeMatrixDefinitionProvider.getUpgradeMatrix();
    }
}
