package com.sequenceiq.environment.environment.flow.start.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.common.api.type.DataHubStartAction;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

import reactor.rx.Promise;

public class EnvStartEvent extends BaseNamedFlowEvent {

    private DataHubStartAction dataHubStartAction;

    public EnvStartEvent(String selector, Long resourceId, String resourceName, String resourceCrn, DataHubStartAction dataHubStartAction) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.dataHubStartAction = dataHubStartAction;
    }

    public EnvStartEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, String resourceName,
        String resourceCrn, DataHubStartAction dataHubStartAction) {
        super(selector, resourceId, accepted, resourceName, resourceCrn);
        this.dataHubStartAction = dataHubStartAction;
    }

    public DataHubStartAction getDataHubStartAction() {
        return dataHubStartAction;
    }

    public static final class EnvStartEventBuilder {
        private String resourceName;

        private String resourceCrn;

        private String selector;

        private Long resourceId;

        private Promise<AcceptResult> accepted;

        private DataHubStartAction dataHubStartAction;

        private EnvStartEventBuilder() {
        }

        public static EnvStartEventBuilder anEnvStartEvent() {
            return new EnvStartEventBuilder();
        }

        public EnvStartEventBuilder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public EnvStartEventBuilder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public EnvStartEventBuilder withResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public EnvStartEventBuilder withAccepted(Promise<AcceptResult> accepted) {
            this.accepted = accepted;
            return this;
        }

        public EnvStartEventBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public EnvStartEventBuilder withDataHubStart(DataHubStartAction dataHubStartAction) {
            this.dataHubStartAction = dataHubStartAction;
            return this;
        }

        public EnvStartEvent build() {
            return new EnvStartEvent(selector, resourceId, accepted, resourceName, resourceCrn, dataHubStartAction);
        }
    }
}
