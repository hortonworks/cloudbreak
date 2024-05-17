package com.sequenceiq.environment.environment.flow.externalizedcluster.create.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class ExternalizedComputeClusterCreationFailedEvent extends BaseFailedFlowEvent {

        private final Exception exception;

        private final EnvironmentDto environmentDto;

        private final EnvironmentStatus environmentStatus;

        @JsonCreator
        public ExternalizedComputeClusterCreationFailedEvent(
                @JsonProperty("environmentDto") EnvironmentDto environmentDto,
                @JsonProperty("exception") Exception exception,
                @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus) {

            super(ExternalizedComputeClusterCreationStateSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_EVENT.name(), environmentDto.getResourceId(), null,
                    environmentDto.getName(), environmentDto.getResourceCrn(), exception);
            this.exception = exception;
            this.environmentDto = environmentDto;
            this.environmentStatus = environmentStatus;
        }

        @Override
        public String selector() {
            return ExternalizedComputeClusterCreationStateSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_EVENT.name();
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
