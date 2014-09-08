package com.sequenceiq.cloudbreak.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;

public class UserRolesUtil {
    private UserRolesUtil() {
    }

    public static boolean isUserInRole(User user, UserRole userRole) {
        return user.getUserRoles().contains(userRole);
    }

    public static Set<UserRole> getGroupForRole(UserRole userRole) {
        Set<UserRole> roleGroup = new HashSet<>();
        switch (userRole) {
            case DEPLOYER:
                roleGroup = new HashSet<>(Arrays.asList(UserRole.DEPLOYER, UserRole.ACCOUNT_ADMIN, UserRole.ACCOUNT_USER));
                break;
            case ACCOUNT_ADMIN:
                roleGroup = new HashSet<>(Arrays.asList(UserRole.ACCOUNT_ADMIN, UserRole.ACCOUNT_USER));
                break;
            case ACCOUNT_USER:
                roleGroup = new HashSet<>(Arrays.asList(UserRole.ACCOUNT_USER));
                break;
            default:
                throw new IllegalStateException(String.format("Unsupported role: %s", userRole));
        }
        return roleGroup;
    }
}
