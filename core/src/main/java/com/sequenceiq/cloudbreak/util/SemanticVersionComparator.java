package com.sequenceiq.cloudbreak.util;

import java.io.Serializable;
import java.util.Comparator;
import java.util.function.Function;

public class SemanticVersionComparator implements Comparator<String>, Serializable {

    public static final int SEGMENT_COUNT = 3;

    @Override
    public int compare(String ver1, String ver2) {
        Function<String, String[]> segmentize = ver -> ver.split("\\.");
        int ver1SegmentCount = segmentize.apply(ver1).length;
        int ver2SegmentCount = segmentize.apply(ver2).length;
        if (ver1SegmentCount < SEGMENT_COUNT || ver2SegmentCount < SEGMENT_COUNT) {
            throw new IllegalArgumentException("Version must conform to the following format: MAJOR.MINOR.PATCH");
        }
        if (ver1SegmentCount != ver2SegmentCount) {
            throw new IllegalArgumentException(String.format("Segment count mismatch in the provided versions. [%s, %s]", ver1SegmentCount, ver2SegmentCount));
        }

        Function<String, Integer> takeMajorSegment =
                ver -> Integer.parseInt(segmentize.apply(ver)[SemanticVersionSegments.MAJOR.ordinal()]);
        Function<String, Integer> takeMinorSegment =
                ver -> Integer.parseInt(segmentize.apply(ver)[SemanticVersionSegments.MINOR.ordinal()]);
        Function<String, Integer> takePatchSegment =
                ver -> Integer.parseInt(segmentize.apply(ver)[SemanticVersionSegments.PATCH.ordinal()].split("-")[0]);
        Function<String, Integer> takeExtensionSegment =
                ver -> Integer.parseInt(segmentize.apply(ver)[SemanticVersionSegments.EXTENSION.ordinal()].split("-")[0]);

        int ver1Major = takeMajorSegment.apply(ver1);
        int ver2Major = takeMajorSegment.apply(ver2);

        if (ver1Major != ver2Major) {
            return Integer.compare(ver1Major, ver2Major);
        }

        int ver1Minor = takeMinorSegment.apply(ver1);
        int ver2Minor = takeMinorSegment.apply(ver2);

        if (ver1Minor != ver2Minor) {
            return Integer.compare(ver1Minor, ver2Minor);
        }

        int ver1Patch = takePatchSegment.apply(ver1);
        int ver2Patch = takePatchSegment.apply(ver2);

        if (ver1Patch != ver2Patch) {
            return Integer.compare(ver1Patch, ver2Patch);
        }

        if (ver1SegmentCount == SEGMENT_COUNT) {
            return 0;
        }

        int ver1Extension = takeExtensionSegment.apply(ver1);
        int ver2Extension = takeExtensionSegment.apply(ver2);

        if (ver1Extension != ver2Extension) {
            return Integer.compare(ver1Extension, ver2Extension);
        }

        return 0;
    }

    private enum SemanticVersionSegments {
        MAJOR, MINOR, PATCH, EXTENSION
    }
}
