package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;

public final class UserSyncConstants {

    public static final String ADMINS_GROUP = "admins";

    public static final String CDP_USERSYNC_INTERNAL_GROUP = "cdp-usersync-internal";

    public static final String ADMIN_FREEIPA =
            AuthorizationResourceAction.ADMIN_FREEIPA.getRight();

    public static final List<String> RIGHTS;

    static {
        List<String> rights = Lists.newArrayList();
        rights.add(ADMIN_FREEIPA);
        rights.addAll(Arrays.stream(UmsRight.values()).map(right -> right.getRight()).collect(Collectors.toList()));
        RIGHTS = Collections.unmodifiableList(rights);
    }

    private UserSyncConstants() {
        // private access
    }
}
