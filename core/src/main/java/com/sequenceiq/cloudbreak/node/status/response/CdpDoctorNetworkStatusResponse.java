package com.sequenceiq.cloudbreak.node.status.response;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdpDoctorNetworkStatusResponse {

    private CdpDoctorCheckStatus archiveClouderaComAccessible = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus databusAccessible = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus databusS3Accessible = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus s3Accessible = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus stsAccessible = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus adlsV2Accessible = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus azureManagementAccessible = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus gcsAccessible = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus serviceDeliveryCacheS3Accessible = CdpDoctorCheckStatus.UNKNOWN;

    private boolean ccmEnabled;

    private boolean neighbourScan;

    private CdpDoctorCheckStatus anyNeighboursAccessible = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus ccmAccessible = CdpDoctorCheckStatus.UNKNOWN;

    private CdpDoctorCheckStatus clouderaComAccessible = CdpDoctorCheckStatus.UNKNOWN;

    private String cdpTelemetryVersion;

    private CdpDoctorCheckStatus computeMonitoringAccessible = CdpDoctorCheckStatus.UNKNOWN;

    public CdpDoctorCheckStatus getArchiveClouderaComAccessible() {
        return archiveClouderaComAccessible;
    }

    public void setArchiveClouderaComAccessible(CdpDoctorCheckStatus archiveClouderaComAccessible) {
        if (archiveClouderaComAccessible != null) {
            this.archiveClouderaComAccessible = archiveClouderaComAccessible;
        }
    }

    public CdpDoctorCheckStatus getDatabusAccessible() {
        return databusAccessible;
    }

    public void setDatabusAccessible(CdpDoctorCheckStatus databusAccessible) {
        if (databusAccessible != null) {
            this.databusAccessible = databusAccessible;
        }
    }

    public CdpDoctorCheckStatus getDatabusS3Accessible() {
        return databusS3Accessible;
    }

    public void setDatabusS3Accessible(CdpDoctorCheckStatus databusS3Accessible) {
        if (databusS3Accessible != null) {
            this.databusS3Accessible = databusS3Accessible;
        }
    }

    public CdpDoctorCheckStatus getS3Accessible() {
        return s3Accessible;
    }

    public void setS3Accessible(CdpDoctorCheckStatus s3Accessible) {
        if (s3Accessible != null) {
            this.s3Accessible = s3Accessible;
        }
    }

    public CdpDoctorCheckStatus getStsAccessible() {
        return stsAccessible;
    }

    public void setStsAccessible(CdpDoctorCheckStatus stsAccessible) {
        if (stsAccessible != null) {
            this.stsAccessible = stsAccessible;
        }
    }

    public CdpDoctorCheckStatus getAdlsV2Accessible() {
        return adlsV2Accessible;
    }

    public void setAdlsV2Accessible(CdpDoctorCheckStatus adlsV2Accessible) {
        if (adlsV2Accessible != null) {
            this.adlsV2Accessible = adlsV2Accessible;
        }
    }

    public CdpDoctorCheckStatus getAzureManagementAccessible() {
        return azureManagementAccessible;
    }

    public void setAzureManagementAccessible(CdpDoctorCheckStatus azureManagementAccessible) {
        if (azureManagementAccessible != null) {
            this.azureManagementAccessible = azureManagementAccessible;
        }
    }

    public CdpDoctorCheckStatus getGcsAccessible() {
        return gcsAccessible;
    }

    public void setGcsAccessible(CdpDoctorCheckStatus gcsAccessible) {
        if (gcsAccessible != null) {
            this.gcsAccessible = gcsAccessible;
        }
    }

    public CdpDoctorCheckStatus getServiceDeliveryCacheS3Accessible() {
        return serviceDeliveryCacheS3Accessible;
    }

    public void setServiceDeliveryCacheS3Accessible(CdpDoctorCheckStatus serviceDeliveryCacheS3Accessible) {
        if (serviceDeliveryCacheS3Accessible != null) {
            this.serviceDeliveryCacheS3Accessible = serviceDeliveryCacheS3Accessible;
        }
    }

    public boolean getCcmEnabled() {
        return ccmEnabled;
    }

    public void setCcmEnabled(boolean ccmEnabled) {
        this.ccmEnabled = ccmEnabled;
    }

    public boolean getNeighbourScan() {
        return neighbourScan;
    }

    public void setNeighbourScan(boolean neighbourScan) {
        this.neighbourScan = neighbourScan;
    }

    public CdpDoctorCheckStatus getAnyNeighboursAccessible() {
        return anyNeighboursAccessible;
    }

    public void setAnyNeighboursAccessible(CdpDoctorCheckStatus anyNeighboursAccessible) {
        if (anyNeighboursAccessible != null) {
            this.anyNeighboursAccessible = anyNeighboursAccessible;
        }
    }

    public CdpDoctorCheckStatus getCcmAccessible() {
        return ccmAccessible;
    }

    public void setCcmAccessible(CdpDoctorCheckStatus ccmAccessible) {
        if (ccmAccessible != null) {
            this.ccmAccessible = ccmAccessible;
        }
    }

    public CdpDoctorCheckStatus getClouderaComAccessible() {
        return clouderaComAccessible;
    }

    public void setClouderaComAccessible(CdpDoctorCheckStatus clouderaComAccessible) {
        if (clouderaComAccessible != null) {
            this.clouderaComAccessible = clouderaComAccessible;
        }
    }

    public String getCdpTelemetryVersion() {
        return cdpTelemetryVersion;
    }

    public void setCdpTelemetryVersion(String cdpTelemetryVersion) {
        if (cdpTelemetryVersion != null) {
            this.cdpTelemetryVersion = cdpTelemetryVersion;
        }
    }

    public CdpDoctorCheckStatus getComputeMonitoringAccessible() {
        return computeMonitoringAccessible;
    }

    public void setComputeMonitoringAccessible(CdpDoctorCheckStatus computeMonitoringAccessible) {
        if (computeMonitoringAccessible != null) {
            this.computeMonitoringAccessible = computeMonitoringAccessible;
        }
    }

    @Override
    @SuppressWarnings({"checkstyle:CyclomaticComplexity", "checkstyle:NPathComplexity"})
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpDoctorNetworkStatusResponse that = (CdpDoctorNetworkStatusResponse) o;
        return getCcmEnabled() == that.getCcmEnabled() &&
                getNeighbourScan() == that.getNeighbourScan() &&
                getArchiveClouderaComAccessible() == that.getArchiveClouderaComAccessible() &&
                getDatabusAccessible() == that.getDatabusAccessible() &&
                getDatabusS3Accessible() == that.getDatabusS3Accessible() &&
                getS3Accessible() == that.getS3Accessible() &&
                getStsAccessible() == that.getStsAccessible() &&
                getAdlsV2Accessible() == that.getAdlsV2Accessible() &&
                getAzureManagementAccessible() == that.getAzureManagementAccessible() &&
                getGcsAccessible() == that.getGcsAccessible() &&
                getServiceDeliveryCacheS3Accessible() == that.getServiceDeliveryCacheS3Accessible() &&
                getAnyNeighboursAccessible() == that.getAnyNeighboursAccessible() &&
                getCcmAccessible() == that.getCcmAccessible() &&
                getClouderaComAccessible() == that.getClouderaComAccessible() &&
                Objects.equals(getCdpTelemetryVersion(), that.getCdpTelemetryVersion()) &&
                getComputeMonitoringAccessible() == that.getComputeMonitoringAccessible();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getArchiveClouderaComAccessible(), getDatabusAccessible(), getDatabusS3Accessible(), getS3Accessible(),
                getStsAccessible(), getAdlsV2Accessible(), getAzureManagementAccessible(), getGcsAccessible(), getServiceDeliveryCacheS3Accessible(),
                getCcmEnabled(), getNeighbourScan(), getAnyNeighboursAccessible(), getCcmAccessible(), getClouderaComAccessible(),
                getCdpTelemetryVersion(), getComputeMonitoringAccessible());
    }

    @Override
    public String toString() {
        return "CdpDoctorNetworkStatusResponse{" +
                "archiveClouderaComAccessible=" + archiveClouderaComAccessible +
                ", databusAccessible=" + databusAccessible +
                ", databusS3Accessible=" + databusS3Accessible +
                ", s3Accessible=" + s3Accessible +
                ", stsAccessible=" + stsAccessible +
                ", adlsV2Accessible=" + adlsV2Accessible +
                ", azureManagementAccessible=" + azureManagementAccessible +
                ", gcsAccessible=" + gcsAccessible +
                ", serviceDeliveryCacheS3Accessible=" + serviceDeliveryCacheS3Accessible +
                ", ccmEnabled=" + ccmEnabled +
                ", neighbourScan=" + neighbourScan +
                ", anyNeighboursAccessible=" + anyNeighboursAccessible +
                ", ccmAccessible=" + ccmAccessible +
                ", clouderaComAccessible=" + clouderaComAccessible +
                ", cdpTelemetryVersion='" + cdpTelemetryVersion + '\'' +
                ", computeMonitoringAccessible=" + computeMonitoringAccessible +
                '}';
    }
}
