package com.sequenceiq.it.cloudbreak.salt;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SaltFunctionReport {
    private Map<String, Object> changes;

    private String comment;

    private String name;

    private String path;

    private boolean result;

    private String sls;

    private int runNumber;

    private String startTime;

    private double duration;

    private String id;

    private boolean skipWatch;

    private int returnCode;

    @JsonCreator
    public SaltFunctionReport(
            @JsonProperty("changes") Map<String, Object> changes,
            @JsonProperty("comment") String comment,
            @JsonProperty("name") String name,
            @JsonProperty("path") String path,
            @JsonProperty("result") boolean result,
            @JsonProperty("__sls__") String sls,
            @JsonProperty("__run_num__") int runNumber,
            @JsonProperty("start_time") String startTime,
            @JsonProperty("duration") double duration,
            @JsonProperty("__id__") String id,
            @JsonProperty("skip_watch") boolean skipWatch,
            @JsonProperty("retcode") int returnCode) {
        this.changes = changes;
        this.comment = comment;
        this.name = name;
        this.path = path;
        this.result = result;
        this.sls = sls;
        this.runNumber = runNumber;
        this.startTime = startTime;
        this.duration = duration;
        this.id = id;
        this.skipWatch = skipWatch;
        this.returnCode = returnCode;
    }

    public Map<String, Object> getChanges() {
        return changes;
    }

    public String getComment() {
        return comment;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean getResult() {
        return result;
    }

    public String getSls() {
        return sls;
    }

    public int getRunNumber() {
        return runNumber;
    }

    public String getStartTime() {
        return startTime;
    }

    public double getDuration() {
        return duration;
    }

    public String getId() {
        return id;
    }

    public boolean isSkipWatch() {
        return skipWatch;
    }

    public boolean isResult() {
        return result;
    }

    public int getReturnCode() {
        return returnCode;
    }

    @Override
    public String toString() {
        return "SaltFunctionReport{" +
                "changes=" + changes +
                ", comment='" + comment + '\'' +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", result=" + result +
                ", sls='" + sls + '\'' +
                ", runNumber=" + runNumber +
                ", startTime='" + startTime + '\'' +
                ", duration=" + duration +
                ", id='" + id + '\'' +
                ", skipWatch=" + skipWatch +
                ", returnCode=" + returnCode +
                '}';
    }
}
