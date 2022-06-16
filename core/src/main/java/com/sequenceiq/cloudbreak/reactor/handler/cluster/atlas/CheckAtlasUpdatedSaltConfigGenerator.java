package com.sequenceiq.cloudbreak.reactor.handler.cluster.atlas;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@Component
public class CheckAtlasUpdatedSaltConfigGenerator {
    public static final String CHECK_ATLAS_UPDATED_PILLAR_PATH = "/atlas/check_atlas_updated.sls";

    public static final String CHECK_ATLAS_UPDATED_KEY = "check_atlas_updated";

    public static final String MAX_RETRY_KEY = "max_retries";

    public SaltConfig createSaltConfig(int maxRetries) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        Map<String, String> checkAtlasUpdatedValues = new HashMap<>();
        checkAtlasUpdatedValues.put(MAX_RETRY_KEY, String.valueOf(maxRetries));
        servicePillar.put("check-atlas-updated", new SaltPillarProperties(
                CHECK_ATLAS_UPDATED_PILLAR_PATH,
                singletonMap(CHECK_ATLAS_UPDATED_KEY, checkAtlasUpdatedValues)
        ));
        return new SaltConfig(servicePillar);
    }
}
