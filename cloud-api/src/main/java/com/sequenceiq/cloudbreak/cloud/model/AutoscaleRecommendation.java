package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;
import java.util.Set;

public class AutoscaleRecommendation {

    private Set<String> timeBasedHostGroups;

    private Set<String> loadBasedHostGroups;

    public AutoscaleRecommendation(Set<String> timeBasedHostGroups, Set<String> loadBasedHostGroups) {
        this.timeBasedHostGroups = Set.copyOf(timeBasedHostGroups);
        this.loadBasedHostGroups = Set.copyOf(loadBasedHostGroups);
    }

    public Set<String> getTimeBasedHostGroups() {
        return timeBasedHostGroups;
    }

    public Set<String> getLoadBasedHostGroups() {
        return loadBasedHostGroups;
    }

    public void setTimeBasedHostGroups(Set<String> timeBasedHostGroups) {
        this.timeBasedHostGroups = timeBasedHostGroups;
    }

    public void setLoadBasedHostGroups(Set<String> loadBasedHostGroups) {
        this.loadBasedHostGroups = loadBasedHostGroups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        AutoscaleRecommendation that = (AutoscaleRecommendation) o;
        return Objects.equals(timeBasedHostGroups, that.timeBasedHostGroups)
                && Objects.equals(loadBasedHostGroups, that.loadBasedHostGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeBasedHostGroups, loadBasedHostGroups);
    }

    @Override
    public String toString() {
        return "AutoscaleRecommendation{" +
                "timeBasedHostGroups=" + timeBasedHostGroups +
                ", loadBasedHostGroups=" + loadBasedHostGroups +
                '}';
    }
}
