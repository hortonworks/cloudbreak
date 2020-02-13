package com.sequenceiq.cloudbreak.tag;

import java.util.Map;

import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;

public interface CostTagging {

    Map<String, String> prepareDefaultTags(CDPTagGenerationRequest t);

    Map<String, String> mergeTags(CDPTagMergeRequest t);
}
