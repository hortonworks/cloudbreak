package com.sequenceiq.freeipa.api.v1.dns.model;

import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_HOSTNAME_MSG;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_HOSTNAME_PATTERN;
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

@Schema(name = "AddDnsARecordV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddDnsARecordRequest {

    @NotNull
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotEmpty
    @Schema(description = DnsModelDescription.HOSTNAME, required = true)
    @Pattern(regexp = DNS_HOSTNAME_PATTERN, message = DNS_HOSTNAME_MSG)
    private String hostname;

    @NotEmpty
    @Schema(description = DnsModelDescription.IP, required = true)
    @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
            message = "Must be a valid IPv4 format like 1.2.3.4")
    private String ip;

    @Schema(description = DnsModelDescription.DNS_ZONE)
    @Pattern(regexp = DNS_ZONE_PATTERN, message = DNS_ZONE_MSG)
    private String dnsZone;

    @Schema(description = DnsModelDescription.CREATE_REVERSE)
    private boolean createReverse;

    @Schema(description = DnsModelDescription.FORCE)
    private boolean force;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getDnsZone() {
        return dnsZone;
    }

    public void setDnsZone(String dnsZone) {
        this.dnsZone = dnsZone;
    }

    public boolean isCreateReverse() {
        return createReverse;
    }

    public void setCreateReverse(boolean createReverse) {
        this.createReverse = createReverse;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    @Override
    public String toString() {
        return "AddDnsARecordRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", hostname='" + hostname + '\'' +
                ", ip='" + ip + '\'' +
                ", dnsZone='" + dnsZone + '\'' +
                ", createReverse=" + createReverse +
                ", force=" + force +
                '}';
    }
}
