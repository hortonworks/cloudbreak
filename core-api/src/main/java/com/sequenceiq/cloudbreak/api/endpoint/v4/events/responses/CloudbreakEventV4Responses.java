package com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModel;

@ApiModel
public class CloudbreakEventV4Responses {

    private List<CloudbreakEventV4Response> events = new ArrayList<>();

    public List<CloudbreakEventV4Response> getEvents() {
        return events;
    }

    public void setEvents(List<CloudbreakEventV4Response> events) {
        this.events = events;
    }

    public static final CloudbreakEventV4Responses cloudbreakEventV4Responses(List<CloudbreakEventV4Response> events) {
        CloudbreakEventV4Responses cloudbreakEventV4Responses = new CloudbreakEventV4Responses();
        cloudbreakEventV4Responses.setEvents(events);
        return cloudbreakEventV4Responses;
    }
}
