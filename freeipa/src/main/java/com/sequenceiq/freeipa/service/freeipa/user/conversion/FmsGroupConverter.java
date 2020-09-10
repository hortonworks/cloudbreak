package com.sequenceiq.freeipa.service.freeipa.user.conversion;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.base.Strings;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkArgument;

@Component
public class FmsGroupConverter {

    public FmsGroup umsGroupToGroup(UserManagementProto.Group umsGroup) {
        return nameToGroup(umsGroup.getGroupName());
    }

    public FmsGroup nameToGroup(String name) {
        checkArgument(!Strings.isNullOrEmpty(name));
        FmsGroup fmsGroup = new FmsGroup();
        fmsGroup.withName(name);
        return fmsGroup;
    }
}
