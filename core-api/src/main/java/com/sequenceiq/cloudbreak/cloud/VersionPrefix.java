package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.common.type.Versioned;

public class VersionPrefix {

    public boolean prefixMatch(Versioned o1, Versioned o2, PrefixMatchLength matchLength) {
        return prefixMatch(o1, o2, matchLength.getMatchLength());
    }

    public boolean prefixMatch(Versioned o1, Versioned o2, int digits) {
        String[] vals1 = o1.getVersion().split("[\\.\\-]");
        String[] vals2 = o2.getVersion().split("[\\.\\-]");

        // At least both of the should be long enough to compare
        if (vals1.length <= digits || vals2.length <= digits) {
            return false;
        }

        for (int i = 0; i < digits; i++) {
            if (!vals1[i].equals(vals2[i])) {
                return false;
            }
        }

        return true;
    }
}
