package com.sequenceiq.datalake.flow.datalake.scale.event;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.sdx.api.model.DatalakeHorizontalScaleRequest;

public class DatalakeHorizontalScaleFlowEvent extends BaseNamedFlowEvent {

    private final String userId;

    private final BigDecimal commandId;

    private final DatalakeHorizontalScaleRequest scaleRequest;

    private final Exception exception;

    @JsonCreator
    public DatalakeHorizontalScaleFlowEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("userId") String userId,
            @JsonProperty("scaleRequest") DatalakeHorizontalScaleRequest scaleRequest,
            @JsonProperty("commandId") BigDecimal commandId,
            @JsonProperty("exception") Exception exception) {
        super(selector, resourceId, resourceName, resourceCrn);
        this.userId = userId;
        this.scaleRequest = scaleRequest;
        this.commandId = commandId;
        this.exception = exception;
    }

    public static DatalakeHorizontalScaleFlowEventBuilder datalakeHorizontalScaleFlowEventBuilderFactory(DatalakeHorizontalScaleFlowEvent event) {
        return new DatalakeHorizontalScaleFlowEventBuilder()
                .setResourceId(event.getResourceId())
                .setResourceName(event.getResourceName())
                .setResourceCrn(event.getResourceCrn())
                .setUserId(event.getUserId())
                .setSelector(event.selector())
                .setException(event.getException());
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getCommandId() {
        return commandId;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    public DatalakeHorizontalScaleRequest getScaleRequest() {
        return scaleRequest;
    }

    @JsonPOJOBuilder
    public static class DatalakeHorizontalScaleFlowEventBuilder {

        private String userId;

        private BigDecimal commandId;

        private DatalakeHorizontalScaleRequest scaleRequest;

        private String selector;

        private Long resourceId;

        private String resourceName;

        private String resourceCrn;

        private Exception exception;

        public DatalakeHorizontalScaleFlowEventBuilder setUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public DatalakeHorizontalScaleFlowEventBuilder setCommandId(BigDecimal commandId) {
            this.commandId = commandId;
            return this;
        }

        public DatalakeHorizontalScaleFlowEventBuilder setScaleRequest(DatalakeHorizontalScaleRequest scaleRequest) {
            this.scaleRequest = scaleRequest;
            return this;
        }

        public DatalakeHorizontalScaleFlowEventBuilder setSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public DatalakeHorizontalScaleFlowEventBuilder setResourceId(Long resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public DatalakeHorizontalScaleFlowEventBuilder setResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public DatalakeHorizontalScaleFlowEventBuilder setResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public DatalakeHorizontalScaleFlowEventBuilder setException(Exception exception) {
            this.exception = exception;
            return this;
        }

        public DatalakeHorizontalScaleFlowEvent build() {
            return new DatalakeHorizontalScaleFlowEvent(
                    selector,
                    resourceId,
                    resourceName,
                    resourceCrn,
                    userId,
                    scaleRequest,
                    commandId,
                    exception);
        }
    }
}
