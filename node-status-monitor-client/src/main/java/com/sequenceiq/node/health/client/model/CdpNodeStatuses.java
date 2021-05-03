package com.sequenceiq.node.health.client.model;

import java.util.Optional;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.sequenceiq.cloudbreak.client.RPCResponse;

public class CdpNodeStatuses {

    private final Optional<RPCResponse<NodeStatusProto.NodeStatusReport>> networkReport;

    private final Optional<RPCResponse<NodeStatusProto.NodeStatusReport>> servicesReport;

    private final Optional<RPCResponse<NodeStatusProto.NodeStatusReport>> systemMetricsReport;

    private final Optional<RPCResponse<NodeStatusProto.NodeStatusReport>> meteringReport;

    private final Optional<RPCResponse<NodeStatusProto.CmMetricsReport>> cmMetricsReport;

    private CdpNodeStatuses(Builder builder) {
        networkReport = Optional.ofNullable(builder.networkReport);
        servicesReport = Optional.ofNullable(builder.servicesReport);
        systemMetricsReport = Optional.ofNullable(builder.systemMetricsReport);
        meteringReport = Optional.ofNullable(builder.meteringReport);
        cmMetricsReport = Optional.ofNullable(builder.cmMetricsReport);
    }

    public Optional<RPCResponse<NodeStatusProto.NodeStatusReport>> getNetworkReport() {
        return networkReport;
    }

    public Optional<RPCResponse<NodeStatusProto.NodeStatusReport>> getServicesReport() {
        return servicesReport;
    }

    public Optional<RPCResponse<NodeStatusProto.NodeStatusReport>> getSystemMetricsReport() {
        return systemMetricsReport;
    }

    public Optional<RPCResponse<NodeStatusProto.NodeStatusReport>> getMeteringReport() {
        return meteringReport;
    }

    public Optional<RPCResponse<NodeStatusProto.CmMetricsReport>> getCmMetricsReport() {
        return cmMetricsReport;
    }

    public static class Builder {

        private RPCResponse<NodeStatusProto.NodeStatusReport> networkReport;

        private RPCResponse<NodeStatusProto.NodeStatusReport> servicesReport;

        private RPCResponse<NodeStatusProto.NodeStatusReport> systemMetricsReport;

        private RPCResponse<NodeStatusProto.NodeStatusReport> meteringReport;

        private RPCResponse<NodeStatusProto.CmMetricsReport> cmMetricsReport;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public CdpNodeStatuses build() {
            return new CdpNodeStatuses(this);
        }

        public Builder withNetworkReport(RPCResponse<NodeStatusProto.NodeStatusReport> networkReport) {
            this.networkReport = networkReport;
            return this;
        }

        public Builder withServicesReport(RPCResponse<NodeStatusProto.NodeStatusReport> servicesReport) {
            this.servicesReport = servicesReport;
            return this;
        }

        public Builder withSystemMetricsReport(RPCResponse<NodeStatusProto.NodeStatusReport> systemMetricsReport) {
            this.systemMetricsReport = systemMetricsReport;
            return this;
        }

        public Builder withMeteringReport(RPCResponse<NodeStatusProto.NodeStatusReport> meteringReport) {
            this.meteringReport = meteringReport;
            return this;
        }

        public Builder withCmMetricsReport(RPCResponse<NodeStatusProto.CmMetricsReport> cmMetricsReport) {
            this.cmMetricsReport = cmMetricsReport;
            return this;
        }
    }
}
