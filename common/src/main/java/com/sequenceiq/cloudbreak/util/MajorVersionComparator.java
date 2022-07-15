package com.sequenceiq.cloudbreak.util;

import java.io.Serializable;
import java.util.Comparator;

import com.sequenceiq.cloudbreak.common.type.Versioned;

public class MajorVersionComparator implements Comparator<Versioned>, Serializable {

    @Override
    public int compare(Versioned o1, Versioned o2) {
        String[] parts1 = o1.getVersion().split(VersionComparator.VERSION_SPLITTER_REGEX);
        String[] parts2 = o2.getVersion().split(VersionComparator.VERSION_SPLITTER_REGEX);

        Integer major1 = Integer.parseInt(parts1[0]);
        Integer major2 = Integer.parseInt(parts2[0]);
        return Integer.signum(major1.compareTo(major2));
    }

}