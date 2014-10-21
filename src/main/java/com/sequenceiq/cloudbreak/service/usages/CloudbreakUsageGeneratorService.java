package com.sequenceiq.cloudbreak.service.usages;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

public interface CloudbreakUsageGeneratorService {
    List<CloudbreakUsage> generateCloudbreakUsages();
}
