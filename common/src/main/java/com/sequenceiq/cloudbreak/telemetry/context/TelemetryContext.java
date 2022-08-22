package com.sequenceiq.cloudbreak.telemetry.context;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public class TelemetryContext {

    private final Map<Class<? extends TelemetryConfigView>, TelemetryConfigView> configs = new HashMap<>();

    private Telemetry telemetry;

    private FluentClusterType clusterType;

    private DatabusContext databusContext;

    private MonitoringContext monitoringContext;

    private LogShipperContext logShipperContext;

    private MeteringContext meteringContext;

    private NodeStatusContext nodeStatusContext;

    private TelemetryClusterDetails clusterDetails;

    private Map<String, Object> paywallConfigs;

    public <T extends TelemetryConfigView> void addConfigView(T configView) {
        configs.put(configView.getClass(), configView);
    }

    public <T extends TelemetryConfigView> T getConfig(Class<T> clazz) {
        return (T) configs.get(clazz);
    }

    public Telemetry getTelemetry() {
        return telemetry;
    }

    public void setTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
    }

    public DatabusContext getDatabusContext() {
        return databusContext;
    }

    public void setDatabusContext(DatabusContext databusContext) {
        this.databusContext = databusContext;
    }

    public MonitoringContext getMonitoringContext() {
        return monitoringContext;
    }

    public void setMonitoringContext(MonitoringContext monitoringContext) {
        this.monitoringContext = monitoringContext;
    }

    public LogShipperContext getLogShipperContext() {
        return logShipperContext;
    }

    public void setLogShipperContext(LogShipperContext logShipperContext) {
        this.logShipperContext = logShipperContext;
    }

    public MeteringContext getMeteringContext() {
        return meteringContext;
    }

    public void setMeteringContext(MeteringContext meteringContext) {
        this.meteringContext = meteringContext;
    }

    public NodeStatusContext getNodeStatusContext() {
        return nodeStatusContext;
    }

    public void setNodeStatusContext(NodeStatusContext nodeStatusContext) {
        this.nodeStatusContext = nodeStatusContext;
    }

    public FluentClusterType getClusterType() {
        return clusterType;
    }

    public void setClusterType(FluentClusterType clusterType) {
        this.clusterType = clusterType;
    }

    public TelemetryClusterDetails getClusterDetails() {
        return clusterDetails;
    }

    public void setClusterDetails(TelemetryClusterDetails clusterDetails) {
        this.clusterDetails = clusterDetails;
    }

    public Map<String, Object> getPaywallConfigs() {
        return paywallConfigs;
    }

    public void setPaywallConfigs(Map<String, Object> paywallConfigs) {
        this.paywallConfigs = paywallConfigs;
    }

    @Override
    public String toString() {
        return "TelemetryContext{" +
                "configs=" + configs +
                ", telemetry=" + telemetry +
                ", clusterType=" + clusterType +
                ", databusContext=" + databusContext +
                ", monitoringContext=" + monitoringContext +
                ", logShipperContext=" + logShipperContext +
                ", meteringContext=" + meteringContext +
                ", nodeStatusContext=" + nodeStatusContext +
                ", clusterDetails=" + clusterDetails +
                ", paywallConfigs=******" +
                '}';
    }
}
