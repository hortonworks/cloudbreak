package com.sequenceiq.it.cloudbreak.config.user;

import static com.sequenceiq.it.cloudbreak.config.user.DefaultUserConfig.DEFAULT_USERS;
import static com.sequenceiq.it.cloudbreak.config.user.UmsUserConfig.UMS_USER;

public enum TestUserSelectors {
    DEFAULT(new DefaultTestUserSelector(DEFAULT_USERS)),
    UMS_ONLY(new DefaultTestUserSelector(UMS_USER)),
    UMS_PREFERED(new PreferedUmsTestUserSelector());

    private final TestUserSelector selector;

    TestUserSelectors(TestUserSelector selector) {
        this.selector = selector;
    }

    TestUserSelector getSelector() {
        return selector;
    }
}
