package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;

public class InstanceCount {

    public static final InstanceCount ZERO_OR_MORE = atLeast(0);

    public static final InstanceCount ONE_OR_MORE = atLeast(1);

    public static final InstanceCount EXACTLY_ONE = exactly(1);

    private Integer minimumCount;

    private Integer maximumCount;

    private InstanceCount(Integer minimumCount, Integer maximumCount) {
        this.minimumCount = minimumCount;
        this.maximumCount = maximumCount;
    }

    public static InstanceCount of(Integer minimumCount, Integer maximumCount) {
        return new InstanceCount(minimumCount, maximumCount);
    }

    public static InstanceCount atLeast(Integer minimumCount) {
        return of(minimumCount, Integer.MAX_VALUE);
    }

    public static InstanceCount exactly(Integer count) {
        return of(count, count);
    }

    public Integer getMinimumCount() {
        return minimumCount;
    }

    public Integer getMaximumCount() {
        return maximumCount;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (null == obj || getClass() != obj.getClass()) {
            return false;
        }

        InstanceCount other = (InstanceCount) obj;

        return Objects.equals(minimumCount, other.minimumCount)
                && Objects.equals(maximumCount, other.maximumCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minimumCount, maximumCount);
    }

    @Override
    public String toString() {
        if (Objects.equals(minimumCount, maximumCount)) {
            return Objects.toString(minimumCount);
        }
        if (maximumCount == Integer.MAX_VALUE) {
            return minimumCount + "+";
        }
        return minimumCount + "-" + maximumCount;
    }

    public boolean isAcceptable(int count) {
        return minimumCount <= count && count <= maximumCount;
    }

    /**
     * Logic from UI.
     */
    public static InstanceCount fallbackInstanceCountRecommendation(String hostGroup) {
        return hostGroup.contains("compute")
                ? ZERO_OR_MORE
                : ONE_OR_MORE;
    }

}
