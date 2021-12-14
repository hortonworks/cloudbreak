package com.sequenceiq.mock.clouderamanager;

public class CmProfile {

    private String profile;

    private int times;

    public CmProfile(String profile, int times) {
        this.profile = profile;
        this.times = times;
    }

    public String getProfile() {
        return profile;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }
}
