package com.sequenceiq.periscope.model.yarn;

public class DecommissionCandidate {

    private Integer amCount;

    private Integer runningAppCount;

    private Integer decommissionTimeout;

    private String nodeId;

    private String nodeState;

    public Integer getAmCount() {
        return amCount;
    }

    public void setAmCount(Integer amCount) {
        this.amCount = amCount;
    }

    public Integer getRunningAppCount() {
        return runningAppCount;
    }

    public void setRunningAppCount(Integer runningAppCount) {
        this.runningAppCount = runningAppCount;
    }

    public Integer getDecommissionTimeout() {
        return decommissionTimeout;
    }

    public void setDecommissionTimeout(Integer decommissionTimeout) {
        this.decommissionTimeout = decommissionTimeout;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeState() {
        return nodeState;
    }

    public void setNodeState(String nodeState) {
        this.nodeState = nodeState;
    }

    @Override
    public String toString() {
        return "DecommissionCandidate{" +
                "amCount=" + amCount +
                ", runningAppCount=" + runningAppCount +
                ", decommissionTimeout=" + decommissionTimeout +
                ", nodeId='" + nodeId + '\'' +
                ", nodeState='" + nodeState + '\'' +
                '}';
    }
}
