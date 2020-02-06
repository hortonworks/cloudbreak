package com.sequenceiq.cloudbreak.common.cost;

import java.util.Map;

import com.sequenceiq.cloudbreak.common.service.CDPTagGenerationRequest;

@FunctionalInterface
public interface CostTagging {

    Map<String, String> prepareDefaultTags(CDPTagGenerationRequest t);

}
