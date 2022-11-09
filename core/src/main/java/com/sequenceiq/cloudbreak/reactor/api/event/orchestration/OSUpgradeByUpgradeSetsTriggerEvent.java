package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

public class OSUpgradeByUpgradeSetsTriggerEvent extends StackEvent {

    private final String platformVariant;

    private final ImageChangeDto imageChangeDto;

    private final List<OrderedOSUpgradeSet> upgradeSets;

    @JsonCreator
    public OSUpgradeByUpgradeSetsTriggerEvent(@JsonProperty("resourceId") Long resourceId, @JsonProperty("platformVariant") String platformVariant,
            @JsonProperty("imageChangeDto") ImageChangeDto imageChangeDto, @JsonProperty("upgradeSets") List<OrderedOSUpgradeSet> upgradeSets) {
        super(resourceId);
        this.platformVariant = platformVariant;
        this.imageChangeDto = imageChangeDto;
        this.upgradeSets = upgradeSets;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public ImageChangeDto getImageChangeDto() {
        return imageChangeDto;
    }

    public List<OrderedOSUpgradeSet> getUpgradeSets() {
        return upgradeSets;
    }

}
