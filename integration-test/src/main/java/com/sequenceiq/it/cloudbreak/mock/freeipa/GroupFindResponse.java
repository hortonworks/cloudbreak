package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.Group;

import spark.Request;
import spark.Response;

@Component
public class GroupFindResponse extends AbstractFreeIpaResponse<Set<Group>> {
    @Override
    public String method() {
        return "group_find";
    }

    @Override
    protected Set<Group> handleInternal(Request request, Response response) {
        Group group = new Group();
        group.setCn("admins");
        group.setMemberUser(List.of("admin"));
        return Set.of(group);
    }
}
