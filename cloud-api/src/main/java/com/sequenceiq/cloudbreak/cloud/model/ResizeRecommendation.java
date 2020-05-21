package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;
import java.util.Set;

public class ResizeRecommendation {

    private Set<String> scaleUpHostGroups;

    private Set<String> scaleDownHostGroups;

    public ResizeRecommendation(Set<String> scaleUpHostGroups, Set<String> scaleDownHostGroups) {
        this.scaleUpHostGroups = Set.copyOf(scaleUpHostGroups);
        this.scaleDownHostGroups = Set.copyOf(scaleDownHostGroups);
    }

    public Set<String> getScaleUpHostGroups() {
        return scaleUpHostGroups;
    }

    public Set<String> getScaleDownHostGroups() {
        return scaleDownHostGroups;
    }

    public void setScaleUpHostGroups(Set<String> scaleUpHostGroups) {
        this.scaleUpHostGroups = Set.copyOf(scaleUpHostGroups);
    }

    public void setScaleDownHostGroups(Set<String> scaleDownHostGroups) {
        this.scaleDownHostGroups = Set.copyOf(scaleDownHostGroups);
    }

    @Override
    public String toString() {
        return "ResizeRecommendation{" +
                "scaleUpHostGroups=" + scaleUpHostGroups +
                ", scaleDownHostGroups=" + scaleDownHostGroups +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        ResizeRecommendation that = (ResizeRecommendation) o;
        return Objects.equals(scaleUpHostGroups, that.scaleUpHostGroups) &&
                Objects.equals(scaleDownHostGroups, that.scaleDownHostGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scaleUpHostGroups, scaleDownHostGroups);
    }
}
