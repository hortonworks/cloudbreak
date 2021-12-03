package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.Group;

@Component
public class GroupAddResponse extends AbstractFreeIpaResponse<Group> {
    @Override
    public String method() {
        return "group_add";
    }

    @Override
    protected Group handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        Group group = new Group();
        group.setCn("admins");
        group.setMemberUser(List.of("admin"));
        return group;
    }
}
