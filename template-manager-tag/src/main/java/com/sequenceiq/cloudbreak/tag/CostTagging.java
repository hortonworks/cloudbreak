package com.sequenceiq.cloudbreak.tag;

import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;
import com.sequenceiq.common.api.tag.model.Tags;

public interface CostTagging {

    Tags prepareDefaultTags(CDPTagGenerationRequest t);

    Tags mergeTags(CDPTagMergeRequest t);

    Tags generateAccountTags(CDPTagGenerationRequest t);
}
