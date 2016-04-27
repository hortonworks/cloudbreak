package com.sequenceiq.cloudbreak.shell.commands;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant;
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone;
import com.sequenceiq.cloudbreak.shell.completion.StackRegion;

public interface StackCommands {

    String create(String name,
            StackRegion region,
            StackAvailabilityZone availabilityZone,
            Boolean publicInAccount,
            OnFailureAction onFailureAction,
            AdjustmentType adjustmentType,
            Long threshold,
            Boolean relocateDocker,
            Boolean wait,
            PlatformVariant platformVariant,
            String orchestrator,
            String platform,
            Map<String, String> params);

    boolean createStackAvailable(String platform);
}
