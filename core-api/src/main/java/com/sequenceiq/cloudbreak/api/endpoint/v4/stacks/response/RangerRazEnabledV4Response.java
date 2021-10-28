package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

public class RangerRazEnabledV4Response {

    private boolean rangerRazEnabled;

    public RangerRazEnabledV4Response() {
    }

    public RangerRazEnabledV4Response(boolean rangerRazEnabled) {
        this.rangerRazEnabled = rangerRazEnabled;
    }

    public boolean isRangerRazEnabled() {
        return rangerRazEnabled;
    }

    public void setRangerRazEnabled(boolean rangerRazEnabled) {
        this.rangerRazEnabled = rangerRazEnabled;
    }

    @Override
    public String toString() {
        return "RangerRazEnabledV4Response{" +
                "rangerRazEnabled=" + rangerRazEnabled +
                '}';
    }
}
