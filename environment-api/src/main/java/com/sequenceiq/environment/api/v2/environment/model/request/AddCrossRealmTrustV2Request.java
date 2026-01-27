package com.sequenceiq.environment.api.v2.environment.model.request;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AddCrossRealmTrustV2Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddCrossRealmTrustV2Request {

    @Schema(description = CrossRealmTrustModelDescriptions.AD)
    private SetupCrossRealmTrustV2ActiveDirectoryRequest ad;

    @NotEmpty
    @Size(min = 1, max = 1)
    @Schema(description = CrossRealmTrustModelDescriptions.DNS_IPS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> dnsServerIps = new ArrayList<>();

    @Schema(description = CrossRealmTrustModelDescriptions.TRUST_SECRET)
    private String trustSecret;

    public SetupCrossRealmTrustV2ActiveDirectoryRequest getAd() {
        return ad;
    }

    public void setAd(SetupCrossRealmTrustV2ActiveDirectoryRequest ad) {
        this.ad = ad;
    }

    public List<String> getDnsServerIps() {
        return dnsServerIps;
    }

    public void setDnsServerIps(List<String> dnsServerIps) {
        this.dnsServerIps = dnsServerIps;
    }

    public String getTrustSecret() {
        return trustSecret;
    }

    public void setTrustSecret(String trustSecret) {
        this.trustSecret = trustSecret;
    }

    @Override
    public String toString() {
        return "AddCrossRealmTrustV2Request{" +
                "ad=" + ad +
                ", dnsServerIps=" + dnsServerIps +
                '}';
    }
}
