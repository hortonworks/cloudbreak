package com.sequenceiq.cloudbreak.facade;

import java.util.List;

import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters;

public interface CloudbreakUsagesFacade {

    List<CloudbreakUsageJson> getUsagesFor(CbUsageFilterParameters params);

}
