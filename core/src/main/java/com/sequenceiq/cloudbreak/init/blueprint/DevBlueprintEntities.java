package com.sequenceiq.cloudbreak.init.blueprint;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cb.devblueprint.cm")
public class DevBlueprintEntities extends AbstractBlueprintEntities {

}
