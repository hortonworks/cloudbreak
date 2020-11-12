package com.sequenceiq.mock.legacy.freeipa.response;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.Permission;

@Component
public class PermissionFindResponse extends AbstractFreeIpaResponse<Set<Permission>> {

    private static final String SET_PASSWORD_EXPIRATION_PERMISSION = "Set Password Expiration";

    @Override
    public String method() {
        return "permission_find";
    }

    @Override
    protected Set<Permission> handleInternal(String body) {
        Permission permission = new Permission();
        permission.setCn(SET_PASSWORD_EXPIRATION_PERMISSION);
        return Set.of(permission);
    }
}
