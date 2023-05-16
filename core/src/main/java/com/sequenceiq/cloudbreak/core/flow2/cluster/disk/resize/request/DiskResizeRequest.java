package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

@JsonDeserialize(builder = DiskResizeRequest.Builder.class)
public class DiskResizeRequest extends BaseFlowEvent implements Selectable {

    private final String instanceGroup;

    public DiskResizeRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("instanceGroup") String instanceGroup) {
        super(selector, stackId, null);
        this.instanceGroup = instanceGroup;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public String toString() {
        return new StringJoiner(", ", DiskResizeRequest.class.getSimpleName() + "[", "]")
                .add("instanceGroup=" + instanceGroup)
                .toString();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private Long stackId;

        private String selector;

        private String instanceGroup;

        private Builder() {
        }

        public Builder withStackId(Long stackId) {
            this.stackId = stackId;
            return this;
        }

        public Builder withSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder withInstanceGroup(String instanceGroup) {
            this.instanceGroup = instanceGroup;
            return this;
        }

        public static Builder builder() {
            return new Builder();
        }

        public DiskResizeRequest build() {
            return new DiskResizeRequest(selector, stackId, instanceGroup);
        }
    }
}
