package com.sequenceiq.cloudbreak.telemetry.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("telemetry.monitoring")
public class MonitoringConfiguration {

    private String remoteWriteUrl;

    private String remoteWriteInternalUrl;

    private String paasRemoteWriteUrl;

    private String paasRemoteWriteInternalUrl;

    private Integer scrapeIntervalSeconds;

    private MonitoringAgentConfiguration agent;

    private ExporterConfiguration clouderaManagerExporter;

    private NodeExporterConfiguration nodeExporter;

    private BlackboxExporterConfiguration blackboxExporter;

    private RequestSignerConfiguration requestSigner;

    public String getRemoteWriteUrl() {
        return remoteWriteUrl;
    }

    public void setRemoteWriteUrl(String remoteWriteUrl) {
        this.remoteWriteUrl = remoteWriteUrl;
    }

    public String getRemoteWriteInternalUrl() {
        return remoteWriteInternalUrl;
    }

    public void setRemoteWriteInternalUrl(String remoteWriteInternalUrl) {
        this.remoteWriteInternalUrl = remoteWriteInternalUrl;
    }

    public String getPaasRemoteWriteUrl() {
        return paasRemoteWriteUrl;
    }

    public void setPaasRemoteWriteUrl(String paasRemoteWriteUrl) {
        this.paasRemoteWriteUrl = paasRemoteWriteUrl;
    }

    public String getPaasRemoteWriteInternalUrl() {
        return paasRemoteWriteInternalUrl;
    }

    public void setPaasRemoteWriteInternalUrl(String paasRemoteWriteInternalUrl) {
        this.paasRemoteWriteInternalUrl = paasRemoteWriteInternalUrl;
    }

    public Integer getScrapeIntervalSeconds() {
        return scrapeIntervalSeconds;
    }

    public void setScrapeIntervalSeconds(Integer scrapeIntervalSeconds) {
        this.scrapeIntervalSeconds = scrapeIntervalSeconds;
    }

    public ExporterConfiguration getClouderaManagerExporter() {
        return clouderaManagerExporter;
    }

    public void setClouderaManagerExporter(ExporterConfiguration clouderaManagerExporter) {
        this.clouderaManagerExporter = clouderaManagerExporter;
    }

    public NodeExporterConfiguration getNodeExporter() {
        return nodeExporter;
    }

    public void setNodeExporter(NodeExporterConfiguration nodeExporter) {
        this.nodeExporter = nodeExporter;
    }

    public BlackboxExporterConfiguration getBlackboxExporter() {
        return blackboxExporter;
    }

    public void setBlackboxExporter(BlackboxExporterConfiguration blackboxExporter) {
        this.blackboxExporter = blackboxExporter;
    }

    public MonitoringAgentConfiguration getAgent() {
        return agent;
    }

    public void setAgent(MonitoringAgentConfiguration agent) {
        this.agent = agent;
    }

    public RequestSignerConfiguration getRequestSigner() {
        return requestSigner;
    }

    public void setRequestSigner(RequestSignerConfiguration requestSigner) {
        this.requestSigner = requestSigner;
    }
}
