package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.Privilege;

@Component
public class PrivilegeShowResponse extends AbstractFreeIpaResponse<Privilege> {

    @Override
    public String method() {
        return "privilege_show";
    }

    @Override
    protected Privilege handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        Privilege privilege = new Privilege();
        privilege.setCn("Host Enrollment");
        privilege.setMemberofPermission(List.of("System: Add Hosts", "System: Remove Services", "System: Remove Hosts"));
        return privilege;
    }
}
