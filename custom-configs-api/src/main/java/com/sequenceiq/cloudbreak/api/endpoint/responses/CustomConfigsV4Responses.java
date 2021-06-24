package com.sequenceiq.cloudbreak.api.endpoint.responses;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

public class CustomConfigsV4Responses extends GeneralCollectionV4Response<CustomConfigsV4Response> {

    public CustomConfigsV4Responses(List<CustomConfigsV4Response> customConfigsV4ResponseList) {
        super(customConfigsV4ResponseList);
    }
}
