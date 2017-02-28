package com.sequenceiq.cloudbreak.shell.commands;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant;
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone;
import com.sequenceiq.cloudbreak.shell.completion.StackRegion;

public interface StackCommands {

    String create(String name,
            StackRegion region,
            StackAvailabilityZone availabilityZone,
            boolean publicInAccount,
            OnFailureAction onFailureAction,
            AdjustmentType adjustmentType,
            Long threshold,
            Boolean relocateDocker,
            boolean wait,
            PlatformVariant platformVariant,
            String orchestrator,
            String platform,
            String ambariVersion,
            String hdpVersion,
            String imageCatalog,
            Map<String, String> params,
            Map<String, String> userDefinedTags,
            String customImage,
            Long timeout);

    StackResponse create(StackRequest stackRequest,
            Boolean publicInAccount,
            Boolean wait,
            Long timeout);

    boolean createStackAvailable(String platform);
}
