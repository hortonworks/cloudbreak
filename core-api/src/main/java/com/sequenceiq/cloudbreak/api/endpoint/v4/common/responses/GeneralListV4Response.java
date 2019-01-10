package com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses;

import java.util.List;

import com.google.common.collect.Lists;

public class GeneralListV4Response<T> {

    private List<T> responses = Lists.newArrayList();

    public List<T> getResponses() {
        return responses;
    }

    public void setResponses(List<T> responses) {
        this.responses = responses;
    }

    public static <T> GeneralListV4Response<T> propagateResponses(List<T> responses) {
        GeneralListV4Response<T> v4ResponsesBase = new GeneralListV4Response<>();
        v4ResponsesBase.setResponses(responses);
        return v4ResponsesBase;
    }
}
