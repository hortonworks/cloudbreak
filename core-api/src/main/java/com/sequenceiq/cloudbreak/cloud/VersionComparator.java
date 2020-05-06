package com.sequenceiq.cloudbreak.cloud;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.type.Versioned;

public class VersionComparator implements Comparator<Versioned>, Serializable {

    private static final String VERSION_SPLITTER_REGEX = "[\\.\\-]|(?<=b)(?=\\d)";

    @Override
    public int compare(Versioned o1, Versioned o2) {
        String[] vals1 = o1.getVersion().split(VERSION_SPLITTER_REGEX);
        String[] vals2 = o2.getVersion().split(VERSION_SPLITTER_REGEX);

        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length) {
            int diff = StringUtils.isNumeric(vals1[i]) && StringUtils.isNumeric(vals2[i]) ? Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]))
                    : getDiffByPreReleaseIdPrecedence(vals1[i], vals2[i]);
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other, then shorter wins
        // e.g. "2.4.2.0" is newer than "2.4.2.0-9999"
        return Integer.signum(vals2.length - vals1.length);
    }

    private int getDiffByPreReleaseIdPrecedence(String left, String right) {
        PreReleaseIdentifier leftPrecedence = getPreReleaseIdentifierPrecedence(left);
        PreReleaseIdentifier rightPrecedence = getPreReleaseIdentifierPrecedence(right);

        if (PreReleaseIdentifier.UNKNOWN.equals(leftPrecedence) && PreReleaseIdentifier.UNKNOWN.equals(rightPrecedence)) {
            return left.compareTo(right);
        } else {
            return leftPrecedence.getPrecedence().compareTo(rightPrecedence.getPrecedence());
        }
    }

    private PreReleaseIdentifier getPreReleaseIdentifierPrecedence(String preReleaseIdentifier) {
        switch (preReleaseIdentifier.toLowerCase()) {
            case "b":
                return PreReleaseIdentifier.RE_VERSION;
            case "rc":
                return PreReleaseIdentifier.RC_VERSION;
            case "dev":
                return PreReleaseIdentifier.DEV_VERSION;
            default:
                return PreReleaseIdentifier.UNKNOWN;
        }
    }

    private enum PreReleaseIdentifier {
        RE_VERSION(3),
        RC_VERSION(2),
        DEV_VERSION(1),
        UNKNOWN(0);

        private final int precedence;

        PreReleaseIdentifier(int precedence) {
            this.precedence = precedence;
        }

        public Integer getPrecedence() {
            return precedence;
        }
    }
}
