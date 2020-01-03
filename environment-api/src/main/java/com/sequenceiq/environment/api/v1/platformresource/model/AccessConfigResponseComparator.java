package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.Comparator;

public class AccessConfigResponseComparator implements Serializable, Comparator<AccessConfigResponse> {

    @Override
    public int compare(AccessConfigResponse o1, AccessConfigResponse o2) {
        return o1.getName().compareTo(o2.getName());
    }
}
