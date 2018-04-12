package com.sequenceiq.cloudbreak.facade;

import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.api.model.flex.CloudbreakFlexUsageJson;
import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters;

import java.util.List;

public interface CloudbreakUsagesFacade {

    List<CloudbreakUsageJson> getUsagesFor(CbUsageFilterParameters params);

    CloudbreakFlexUsageJson getFlexUsagesFor(CbUsageFilterParameters params);

}
