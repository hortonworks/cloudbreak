package com.sequenceiq.common.api.node.status.response;

import java.util.List;

public class NodeStatusResponse {

    private List<NodeReport> nodes;

    private Long timestamp;

    public List<NodeReport> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeReport> nodes) {
        this.nodes = nodes;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "MeteringReportResponse{" +
                "nodes=" + nodes +
                ", timestamp=" + timestamp +
                '}';
    }
}
