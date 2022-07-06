package com.sequenceiq.environment.environment.flow.upgrade.ccm.event;

import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors.FAILED_UPGRADE_CCM_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class UpgradeCcmFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final EnvironmentDto environmentDto;

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public UpgradeCcmFailedEvent(
            @JsonProperty("environmentDto") EnvironmentDto environmentDto,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus) {

        super(FAILED_UPGRADE_CCM_EVENT.name(), environmentDto.getResourceId(), null,
                environmentDto.getName(), environmentDto.getResourceCrn(), exception);
        this.environmentDto = environmentDto;
        this.environmentStatus = environmentStatus;
    }

    public EnvironmentDto getEnvironmentDto() {
        return environmentDto;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }
}
