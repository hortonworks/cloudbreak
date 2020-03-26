package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanceCountV4Response implements JsonEntity {

    private Integer minimumCount;

    private Integer maximumCount;

    public InstanceCountV4Response(Integer minimumCount, Integer maximumCount) {
        this.minimumCount = minimumCount;
        this.maximumCount = maximumCount;
    }

    public Integer getMinimumCount() {
        return minimumCount;
    }

    public Integer getMaximumCount() {
        return maximumCount;
    }

}
