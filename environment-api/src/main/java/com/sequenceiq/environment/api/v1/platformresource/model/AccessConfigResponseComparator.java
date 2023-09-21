package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Locale;

public class AccessConfigResponseComparator implements Serializable, Comparator<AccessConfigResponse> {

    @Override
    public int compare(AccessConfigResponse o1, AccessConfigResponse o2) {
        return o1.getName().toLowerCase(Locale.ROOT).compareTo(o2.getName().toLowerCase(Locale.ROOT));
    }
}
