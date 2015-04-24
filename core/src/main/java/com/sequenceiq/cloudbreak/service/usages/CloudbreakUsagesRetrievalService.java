package com.sequenceiq.cloudbreak.service.usages;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

public interface CloudbreakUsagesRetrievalService {
    List<CloudbreakUsage> findUsagesFor(CbUsageFilterParameters params);
}
