package com.sequenceiq.cloudbreak.blueprint.recovery;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessor;

@Component
public class AutoRecoveryConfigProvider implements BlueprintComponentConfigProvider {
    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Override
    public String configure(BlueprintPreparationObject source, String blueprintText) {
        List<BlueprintConfigurationEntry> bpConfigs = new ArrayList<>();
        bpConfigs.add(new BlueprintConfigurationEntry("cluster-env", "recovery_enabled", "true"));
        bpConfigs.add(new BlueprintConfigurationEntry("cluster-env", "recovery_type", "AUTO_START"));
        blueprintText = blueprintProcessor.addConfigEntries(blueprintText, bpConfigs, true);

        bpConfigs = new ArrayList<>();
        bpConfigs.add(new BlueprintConfigurationEntry("recovery_settings", "recovery_enabled", "true"));
        blueprintText = blueprintProcessor.addSettingsEntries(blueprintText, bpConfigs, true);
        return blueprintText;
    }

    @Override
    public boolean additionalCriteria(BlueprintPreparationObject source, String blueprintText) {
        return true;
    }
}
