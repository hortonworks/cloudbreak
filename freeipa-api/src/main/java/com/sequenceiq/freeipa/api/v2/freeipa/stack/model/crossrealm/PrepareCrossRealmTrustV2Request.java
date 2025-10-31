package com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustBase;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PrepareCrossRealmTrustV2Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrepareCrossRealmTrustV2Request extends PrepareCrossRealmTrustBase {

    @Schema(description = CrossRealmTrustModelDescriptions.AD)
    private PrepareCrossRealmTrustV2ActiveDirectoryRequest ad;

    @Schema(description = CrossRealmTrustModelDescriptions.MIT)
    private PrepareCrossRealmTrustV2MitRequest mit;

    @NotEmpty
    @Size(min = 1, max = 1)
    @Schema(description = CrossRealmTrustModelDescriptions.DNS_IPS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> dnsServerIps = new ArrayList<>();

    @NotEmpty
    @Schema(description = ModelDescriptions.REMOTE_ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String remoteEnvironmentCrn;

    @Schema(description = CrossRealmTrustModelDescriptions.TRUST_SECRET)
    private String trustSecret;

    public PrepareCrossRealmTrustV2ActiveDirectoryRequest getAd() {
        return ad;
    }

    public void setAd(PrepareCrossRealmTrustV2ActiveDirectoryRequest ad) {
        this.ad = ad;
    }

    public PrepareCrossRealmTrustV2MitRequest getMit() {
        return mit;
    }

    public void setMit(PrepareCrossRealmTrustV2MitRequest mit) {
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
        return "PrepareCrossRealmTrustV2Request{" +
                "ad=" + ad +
                ", mit=" + mit +
                ", dnsServerIps=" + dnsServerIps +
                ", remoteEnvironmentCrn='" + remoteEnvironmentCrn + '\'' +
                ", trustSecret='" + trustSecret + '\'' +
                '}';
    }
}
