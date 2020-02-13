package com.sequenceiq.cloudbreak.common.cost;

import java.util.Map;

import com.sequenceiq.cloudbreak.common.service.CDPTagGenerationRequest;
import com.sequenceiq.cloudbreak.common.service.CDPTagMergeRequest;

public interface CostTagging {

    Map<String, String> prepareDefaultTags(CDPTagGenerationRequest t);

    Map<String, String> mergeTags(CDPTagMergeRequest t);
}
