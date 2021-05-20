package com.sequenceiq.cloudbreak.cloud;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import com.sequenceiq.cloudbreak.common.type.Versioned;

public class VersionComparator implements Comparator<Versioned>, Serializable {

    private static final String VERSION_SPLITTER_REGEX = "[\\.\\-]|(?<=b)(?=\\d)";

    @Override
    public int compare(Versioned o1, Versioned o2) {
        ImmutablePair<Versioned, Versioned> versionedVersionedPair = transformByLength(new ImmutablePair<>(o1, o2));
        String[] vals1 = versionedVersionedPair.getLeft().getVersion().split(VERSION_SPLITTER_REGEX);
        String[] vals2 = versionedVersionedPair.getRight().getVersion().split(VERSION_SPLITTER_REGEX);

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

    private ImmutablePair<Versioned, Versioned> transformByLength(ImmutablePair<Versioned, Versioned> input) {
        String first = input.getLeft().getVersion();
        String last = input.getRight().getVersion();

        // splitting with '-'
        String[] firstSplit = splitWithHyphens(first);
        String[] lastSplit = splitWithHyphens(last);

        if (!lastSplit[0].equals(firstSplit[0])) {
            if (lastSplit.length < firstSplit.length) {
                last = getLastSegmentWithZeros(last, firstSplit[1]);
            } else if (firstSplit.length < lastSplit.length) {
                first = getLastSegmentWithZeros(first, lastSplit[1]);
            }
        }

        firstSplit = splitWithHyphens(first);
        lastSplit = splitWithHyphens(last);
        String[] firstSplitFirstSegment = splitWithPoint(first);
        String[] lastSplitFirstSegment = splitWithPoint(last);

        if (!lastSplit[0].equals(firstSplit[0])) {
            if (lastSplitFirstSegment.length < firstSplitFirstSegment.length) {
                last = getFirstSegmentWithZeros(lastSplit,
                        firstSplitFirstSegment.length - lastSplitFirstSegment.length);
            } else if (firstSplitFirstSegment.length < lastSplitFirstSegment.length) {
                first = getFirstSegmentWithZeros(firstSplit,
                        lastSplitFirstSegment.length - firstSplitFirstSegment.length);
            }
        }
        String finalFirst = first;
        String finalLast = last;

        return new ImmutablePair<>(() -> finalFirst, () -> finalLast);
    }

    private String getFirstSegmentWithZeros(String[] lastSplit, int difference1) {
        String last;
        int difference = difference1;
        String firstSegment = lastSplit[0];
        for (int i = 0; i < difference; i++) {
            firstSegment += ".0";
        }
        if (lastSplit.length > 1) {
            last = firstSegment + "-" + lastSplit[1];
        } else {
            last = firstSegment;
        }
        return last;
    }

    private String[] splitWithPoint(String first) {
        return first.trim().split("-")[0].trim().split("\\.");
    }

    private String[] splitWithHyphens(String input) {
        return input.trim().split("-");
    }

    private String getLastSegmentWithZeros(String last, String end1) {
        String end = end1;
        last = last + "-" + end.replaceAll("[0-9]", "0");
        return last;
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
