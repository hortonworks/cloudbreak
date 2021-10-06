package com.sequenceiq.environment.environment.flow.deletion.event;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.FAILED_ENV_CLUSTERS_DELETE_EVENT;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class EnvClusterDeleteFailedEvent extends BaseNamedFlowEvent implements Selectable {

    private final Exception exception;

    private final String message;

    public EnvClusterDeleteFailedEvent(Long environmentId, String resourceName, Exception exception, String message, String resourceCrn) {
        super(FAILED_ENV_CLUSTERS_DELETE_EVENT.name(), environmentId, null, resourceName, resourceCrn);
        this.exception = exception;
        this.message = message;
    }

    @Override
    public String selector() {
        return FAILED_ENV_CLUSTERS_DELETE_EVENT.name();
    }

    public Exception getException() {
        return exception;
    }

    public String getMessage() {
        return message;
    }

    public static EnvClusterDeleteFailedEventBuilder builder() {
        return new EnvClusterDeleteFailedEventBuilder();
    }

    public static final class EnvClusterDeleteFailedEventBuilder {
        private Long environmentId;

        private String resourceName;

        private String resourceCrn;

        private Exception exception;

        private String message;

        private EnvClusterDeleteFailedEventBuilder() {
        }

        public EnvClusterDeleteFailedEventBuilder withEnvironmentID(Long environmentId) {
            this.environmentId = environmentId;
            return this;
        }

        public EnvClusterDeleteFailedEventBuilder withException(Exception exception) {
            this.exception = exception;
            return this;
        }

        public EnvClusterDeleteFailedEventBuilder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public EnvClusterDeleteFailedEventBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public EnvClusterDeleteFailedEventBuilder withMessage(String message) {
            this.message = message;
            return this;
        }

        public EnvClusterDeleteFailedEvent build() {
            return new EnvClusterDeleteFailedEvent(environmentId, resourceName, exception, message, resourceCrn);
        }
    }
}
