package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrustResponse {
    @Schema(description = CrossRealmTrustModelDescriptions.KDC_FQDN, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String fqdn;

    @Schema(description = CrossRealmTrustModelDescriptions.TRUST_STATUS, requiredMode = Schema.RequiredMode.REQUIRED)
    private String trustStatus;

    @Schema(description = CrossRealmTrustModelDescriptions.OPERATION_ID, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String operationId;

    @Schema(description = CrossRealmTrustModelDescriptions.KDC_REALM, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String realm;

    @Schema(description = CrossRealmTrustModelDescriptions.KDC_IP, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String ip;

    @Schema(description = CrossRealmTrustModelDescriptions.DNS_IP, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String dnsIp;

    @Schema(description = CrossRealmTrustModelDescriptions.KDC_TYPE, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String kdcType;

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public String getTrustStatus() {
        return trustStatus;
    }

    public void setTrustStatus(String trustStatus) {
        this.trustStatus = trustStatus;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDnsIp() {
        return dnsIp;
    }

    public void setDnsIp(String dnsIp) {
        this.dnsIp = dnsIp;
    }

    public String getKdcType() {
        return kdcType;
    }

    public void setKdcType(String kdcType) {
        this.kdcType = kdcType;
    }

    @Override
    public String toString() {
        return "TrustResponse{" +
                "fqdn='" + fqdn + '\'' +
                ", trustStatus='" + trustStatus + '\'' +
                ", operationId='" + operationId + '\'' +
                ", realm='" + realm + '\'' +
                ", ip='" + ip + '\'' +
                ", dnsIp='" + dnsIp + '\'' +
                ", kdcType='" + kdcType + '\'' +
                '}';
    }
}
