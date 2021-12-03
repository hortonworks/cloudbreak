package com.sequenceiq.mock.freeipa.response;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.Role;

@Component
public class RoleFindResponse extends AbstractFreeIpaResponse<Set<Role>> {
    @Override
    public String method() {
        return "role_find";
    }

    @Override
    protected Set<Role> handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        Role role = new Role();
        role.setCn("roleName");
        role.setMemberUser(List.of());
        role.setMemberGroup(List.of());
        role.setMemberHost(List.of());
        role.setMemberHostGroup(List.of());
        role.setMemberService(List.of());
        return Set.of(role);
    }
}
