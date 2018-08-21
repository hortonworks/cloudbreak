package com.sequenceiq.cloudbreak.service.flex;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.service.organization.OrganizationAwareResourceService;

public interface FlexSubscriptionService extends OrganizationAwareResourceService<FlexSubscription> {

    void setDefaultFlexSubscription(String name, IdentityUser identityUser);

    void setUsedForControllerFlexSubscription(String name, IdentityUser identityUser);

    FlexSubscription findFirstByUsedForController(boolean usedForController);

    FlexSubscription findFirstByIsDefault(boolean byDefault);
}
