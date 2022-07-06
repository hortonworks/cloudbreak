package com.sequenceiq.environment.environment.flow.config.update.event;



import static com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors.FAILED_ENV_STACK_CONIFG_UPDATES_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class EnvStackConfigUpdatesFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final EnvironmentDto environmentDto;

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public EnvStackConfigUpdatesFailedEvent(
            @JsonProperty("environmentDto") EnvironmentDto environmentDto,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus) {

        super(FAILED_ENV_STACK_CONIFG_UPDATES_EVENT.name(), environmentDto.getResourceId(),
                environmentDto.getName(), environmentDto.getResourceCrn(), exception);
        this.environmentDto = environmentDto;
        this.environmentStatus = environmentStatus;
    }

    @Override
    public String selector() {
        return FAILED_ENV_STACK_CONIFG_UPDATES_EVENT.event();
    }

    public EnvironmentDto getEnvironmentDto() {
        return environmentDto;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }
}
