package com.sequenceiq.cloudbreak.api.model.users;

import java.io.Serializable;
import java.util.Comparator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.UserV4Response;

public class UserIdComparator implements Comparator<UserV4Response>, Serializable {

    @Override
    public int compare(UserV4Response o1, UserV4Response o2) {
        return o1.getUserId().compareTo(o2.getUserId());
    }
}
