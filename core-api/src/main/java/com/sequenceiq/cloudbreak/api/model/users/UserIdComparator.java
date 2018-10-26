package com.sequenceiq.cloudbreak.api.model.users;

import java.io.Serializable;
import java.util.Comparator;

public class UserIdComparator implements Comparator<UserResponseJson>, Serializable {

    @Override
    public int compare(UserResponseJson o1, UserResponseJson o2) {
        return o1.getUserId().compareTo(o2.getUserId());
    }
}
