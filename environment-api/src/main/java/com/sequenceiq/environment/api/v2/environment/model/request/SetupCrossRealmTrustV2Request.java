package com.sequenceiq.environment.api.v2.environment.model.request;

import static com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription.REMOTE_ENVIRONMENT_CRN;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SetupCrossRealmTrustV2Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetupCrossRealmTrustV2Request {

    @Schema(description = CrossRealmTrustModelDescriptions.AD)
    private SetupCrossRealmTrustV2ActiveDirectoryRequest ad;

    @Schema(description = CrossRealmTrustModelDescriptions.MIT)
    private SetupCrossRealmTrustV2MitRequest mit;

    @NotEmpty
    @Size(min = 1, max = 1)
    @Schema(description = CrossRealmTrustModelDescriptions.DNS_IPS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> dnsServerIps = new ArrayList<>();

    @NotEmpty
    @Schema(description = REMOTE_ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String remoteEnvironmentCrn;

    @Schema(description = CrossRealmTrustModelDescriptions.TRUST_SECRET)
    private String trustSecret;

    public SetupCrossRealmTrustV2ActiveDirectoryRequest getAd() {
        return ad;
    }

    public void setAd(SetupCrossRealmTrustV2ActiveDirectoryRequest ad) {
        this.ad = ad;
    }

    public SetupCrossRealmTrustV2MitRequest getMit() {
        return mit;
    }

    public void setMit(SetupCrossRealmTrustV2MitRequest mit) {
        this.mit = mit;
    }

    public List<String> getDnsServerIps() {
        return dnsServerIps;
    }

    public void setDnsServerIps(List<String> dnsServerIps) {
        this.dnsServerIps = dnsServerIps;
    }

    public String getRemoteEnvironmentCrn() {
        return remoteEnvironmentCrn;
    }

    public void setRemoteEnvironmentCrn(String remoteEnvironmentCrn) {
        this.remoteEnvironmentCrn = remoteEnvironmentCrn;
    }

    public String getTrustSecret() {
        return trustSecret;
    }

    public void setTrustSecret(String trustSecret) {
        this.trustSecret = trustSecret;
    }

    @Override
    public String toString() {
        return "SetupCrossRealmTrustV2Request{" +
                "ad=" + ad +
                ", mit=" + mit +
                ", dnsServerIps=" + dnsServerIps +
                ", remoteEnvironmentCrn='" + remoteEnvironmentCrn + '\'' +
                '}';
    }
}
