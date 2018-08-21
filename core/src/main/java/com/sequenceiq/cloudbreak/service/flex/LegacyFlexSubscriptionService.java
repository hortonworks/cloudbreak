package com.sequenceiq.cloudbreak.service.flex;

import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.service.organization.LegacyOrganizationAwareResourceService;

public interface LegacyFlexSubscriptionService extends LegacyOrganizationAwareResourceService<FlexSubscription>, FlexSubscriptionService {
}
