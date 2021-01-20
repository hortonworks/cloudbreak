package com.sequenceiq.periscope.model;

import java.util.StringJoiner;

import com.sequenceiq.periscope.monitor.Monitored;

public class RejectedThread implements Monitored {

    private long id;

    private String json;

    private long rejectedCount;

    private String type;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public long getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(long rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void setLastEvaluated(long lastEvaluated) {

    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RejectedThread.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("rejectedCount=" + rejectedCount)
                .add("type='" + type + "'")
                .toString();
    }
}
