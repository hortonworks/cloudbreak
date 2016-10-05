package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.Comparator;
import java.util.Map;

public class RunnerInfo {

    private String stateId;

    private String comment;

    private String name;

    private String startTime;

    private boolean result;

    private double duration;

    private int runNum;

    private Map<String, Object> changes;

    public RunnerInfo() {
    }

    public String getStateId() {
        return stateId;
    }

    public void setStateId(String stateId) {
        this.stateId = stateId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public int getRunNum() {
        return runNum;
    }

    public void setRunNum(int runNum) {
        this.runNum = runNum;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public Map<String, Object> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, Object> changes) {
        this.changes = changes;
    }

    public static class RunNumComparator implements Comparator<RunnerInfo> {

        @Override
        public int compare(RunnerInfo o1, RunnerInfo o2) {
            return Integer.compare(o1.getRunNum(), o2.getRunNum());
        }

    }

    public static class DurationComparator implements Comparator<RunnerInfo> {

        @Override
        public int compare(RunnerInfo o1, RunnerInfo o2) {
            return Double.compare(o1.getDuration(), o2.getDuration());
        }

    }
}
