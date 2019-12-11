package com.sequenceiq.it.cloudbreak.performance;

public class PerformanceIndicator {
    private long start;

    private long duration;

    private boolean stopped;

    private String action;

    private String testName;

    public PerformanceIndicator(String action) {
        start = System.currentTimeMillis();
        stopped = false;
        this.action = action;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void stop() {
        if (!stopped) {
            duration = System.currentTimeMillis() - start;
            stopped = true;
        } else {
            throw new IllegalArgumentException("Performance indicator is already stopped");
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    @Override
    public String toString() {
        return testName + "," + action + "," + start + "," + duration;
    }
}
