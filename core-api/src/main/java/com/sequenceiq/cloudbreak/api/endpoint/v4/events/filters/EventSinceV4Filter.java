package com.sequenceiq.cloudbreak.api.endpoint.v4.events.filters;

import javax.ws.rs.QueryParam;

import io.swagger.annotations.ApiModel;

@ApiModel
public class EventSinceV4Filter {

    @QueryParam("since")
    private Long since;

    public Long getSince() {
        return since;
    }

    public void setSince(Long since) {
        this.since = since;
    }
}
