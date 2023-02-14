package com.sequenceiq.cloudbreak.service.account;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.FeatureSwitchV4.DISABLE_SHOW_BLUEPRINT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.FeatureSwitchV4.DISABLE_SHOW_CLI;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.FeatureSwitchV4;

@Service
public class PreferencesService {

    @Value("${cb.disable.show.blueprint:false}")
    private boolean disableShowBlueprint;

    @Value("${cb.disable.show.cli:false}")
    private boolean disableShowCli;

    public Set<FeatureSwitchV4> getFeatureSwitches() {
        Set<FeatureSwitchV4> featureSwitchV4s = Sets.newHashSet();
        if (disableShowBlueprint) {
            featureSwitchV4s.add(DISABLE_SHOW_BLUEPRINT);
        }
        if (disableShowCli) {
            featureSwitchV4s.add(DISABLE_SHOW_CLI);
        }
        return featureSwitchV4s;
    }

}
