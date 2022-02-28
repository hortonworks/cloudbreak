package com.sequenceiq.cloudbreak.service.upgrade.image;


import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.BlueprintForUpgrade;

@Component
@ConfigurationProperties(prefix = "cb.upgrade")
public class BlueprintForUpgradeProvider {

    private Map<String, BlueprintForUpgrade> blueprints;

    public Map<String, BlueprintForUpgrade> getBlueprints() {
        return blueprints;
    }

    public void setBlueprints(Map<String, BlueprintForUpgrade> blueprints) {
        this.blueprints = blueprints;
    }
}
