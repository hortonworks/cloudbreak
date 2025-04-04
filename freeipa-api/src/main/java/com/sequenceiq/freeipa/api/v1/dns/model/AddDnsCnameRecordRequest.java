package com.sequenceiq.freeipa.api.v1.dns.model;

import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.CNAME_TARGET_REGEXP;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_CNAME_MSG;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_CNAME_PATTERN;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_ZONE_MSG;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_ZONE_PATTERN;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.dns.doc.DnsModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AddDnsCnameRecordV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddDnsCnameRecordRequest {

    @NotNull
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotEmpty
    @Schema(description = DnsModelDescription.CNAME, required = true)
    @Pattern(regexp = DNS_CNAME_PATTERN, message = DNS_CNAME_MSG)
    private String cname;

    @Schema(description = DnsModelDescription.DNS_ZONE)
    @Pattern(regexp = DNS_ZONE_PATTERN, message = DNS_ZONE_MSG)
    private String dnsZone;

    @NotEmpty
    @Schema(description = DnsModelDescription.CNAME_TARGET_FQDN, required = true)
    @Pattern(regexp = CNAME_TARGET_REGEXP,
            message = "Target FQDN must be valid. Might start with '*.' and can contain alphanumeric characters, dash and dot.")
    private String targetFqdn;

    @Schema(description = DnsModelDescription.FORCE)
    private boolean force;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }

    public String getDnsZone() {
        return dnsZone;
    }

    public void setDnsZone(String dnsZone) {
        this.dnsZone = dnsZone;
    }

    public String getTargetFqdn() {
        return targetFqdn;
    }

    public void setTargetFqdn(String targetFqdn) {
        this.targetFqdn = targetFqdn;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    @Override
    public String toString() {
        return "AddDnsCnameRecordRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", cname='" + cname + '\'' +
                ", dnsZone='" + dnsZone + '\'' +
                ", targetFqdn='" + targetFqdn + '\'' +
                ", force=" + force +
                '}';
    }
}
