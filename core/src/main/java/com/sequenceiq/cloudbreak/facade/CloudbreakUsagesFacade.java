package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters;
import com.sequenceiq.cloudbreak.model.CloudbreakUsageJson;

public interface CloudbreakUsagesFacade {

    List<CloudbreakUsageJson> getUsagesFor(CbUsageFilterParameters params);

    void generateUserUsages();

}
