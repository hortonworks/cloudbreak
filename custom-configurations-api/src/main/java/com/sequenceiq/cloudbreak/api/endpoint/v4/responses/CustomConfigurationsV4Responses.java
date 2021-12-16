package com.sequenceiq.cloudbreak.api.endpoint.v4.responses;

import java.util.List;

import com.google.common.collect.Lists;

public class CustomConfigurationsV4Responses extends GeneralCollectionV4Response<CustomConfigurationsV4Response> {

    public CustomConfigurationsV4Responses(List<CustomConfigurationsV4Response> customConfigurationsV4ResponseList) {
        super(customConfigurationsV4ResponseList);
    }

    public CustomConfigurationsV4Responses() {
        super(Lists.newArrayList());
    }
}
