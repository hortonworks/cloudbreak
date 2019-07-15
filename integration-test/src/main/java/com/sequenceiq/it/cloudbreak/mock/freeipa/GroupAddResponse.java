package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.Group;

import spark.Request;
import spark.Response;

@Component
public class GroupAddResponse extends AbstractFreeIpaResponse<Group> {
    @Override
    public String method() {
        return "group_add";
    }

    @Override
    protected Group handleInternal(Request request, Response response) {
        Group group = new Group();
        group.setCn("admins");
        group.setMemberUser(List.of("admin"));
        return group;
    }
}
