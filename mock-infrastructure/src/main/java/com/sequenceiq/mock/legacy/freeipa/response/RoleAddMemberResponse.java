package com.sequenceiq.mock.legacy.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.Role;

@Component
public class RoleAddMemberResponse extends AbstractFreeIpaResponse<Role> {
    @Override
    public String method() {
        return "role_add_member";
    }

    @Override
    protected Role handleInternal(String body) {
        Role role = new Role();
        role.setCn("roleName");
        role.setMemberUser(List.of());
        role.setMemberGroup(List.of());
        role.setMemberHost(List.of());
        role.setMemberHostGroup(List.of());
        role.setMemberService(List.of());
        return role;
    }
}
