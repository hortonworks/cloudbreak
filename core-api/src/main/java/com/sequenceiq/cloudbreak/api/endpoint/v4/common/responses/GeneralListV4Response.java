package com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses;

import java.util.List;

public class GeneralListV4Response<T> {

    private List<T> responses;

    public GeneralListV4Response(List<T> responses) {
        this.responses = responses;
    }

    public List<T> getResponses() {
        return responses;
    }

    public void setResponses(List<T> responses) {
        this.responses = responses;
    }
}
