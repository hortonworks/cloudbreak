package com.sequenceiq.cloudbreak.cloud;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.type.Versioned;

public class VersionComparator implements Comparator<Versioned>, Serializable {

    @Override
    public int compare(Versioned o1, Versioned o2) {
        String[] vals1 = o1.getVersion().split("[\\.\\-]");
        String[] vals2 = o2.getVersion().split("[\\.\\-]");

        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length) {
            int diff = StringUtils.isNumeric(vals1[i]) && StringUtils.isNumeric(vals2[i]) ? Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]))
                    : vals1[i].compareTo(vals2[i]);
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other, then shorter wins
        // e.g. "2.4.2.0" is newer than "2.4.2.0-9999"
        return Integer.signum(vals2.length - vals1.length);
    }
}
