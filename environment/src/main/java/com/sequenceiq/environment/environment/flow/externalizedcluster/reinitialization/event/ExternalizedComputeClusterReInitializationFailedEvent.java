package com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class ExternalizedComputeClusterReInitializationFailedEvent extends BaseFailedFlowEvent {

        private final Exception exception;

        private final EnvironmentDto environmentDto;

        private final EnvironmentStatus environmentStatus;

        @JsonCreator
        public ExternalizedComputeClusterReInitializationFailedEvent(
                @JsonProperty("environmentDto") EnvironmentDto environmentDto,
                @JsonProperty("exception") Exception exception,
                @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus) {

            super(ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_EVENT.name(),
                    environmentDto.getResourceId(), null, environmentDto.getName(), environmentDto.getResourceCrn(), exception);
            this.exception = exception;
            this.environmentDto = environmentDto;
            this.environmentStatus = environmentStatus;
        }

        @Override
        public String selector() {
            return ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_EVENT.name();
        }

        public Exception getException() {
            return exception;
        }

        public EnvironmentDto getEnvironmentDto() {
            return environmentDto;
        }

        public EnvironmentStatus getEnvironmentStatus() {
            return environmentStatus;
        }
    }
