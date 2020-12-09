package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;

public final class UserSyncConstants {

    public static final String ADMINS_GROUP = "admins";

    public static final String CDP_USERSYNC_INTERNAL_GROUP = "cdp-usersync-internal";

    public static final String ACCESS_ENVIRONMENT =
            AuthorizationResourceAction.ACCESS_ENVIRONMENT.getRight();

    public static final String ADMIN_FREEIPA =
            AuthorizationResourceAction.ADMIN_FREEIPA.getRight();

    public static final List<String> RIGHTS;

    static {
        RIGHTS = Lists.newArrayList();
        RIGHTS.add(ADMIN_FREEIPA);
        RIGHTS.addAll(Arrays.stream(UmsRight.values()).map(right -> right.getRight()).collect(Collectors.toList()));
    }

    private UserSyncConstants() {
        // private access
    }
}
