package com.sequenceiq.freeipa.api.v1.dns.model;

import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_FQDN_MSG;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_HOSTNAME_PATTERN;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_IP_MSG;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_IP_PATTERN;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_REVERSEZONE_MSG;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_REVERSEZONE_PATTERN;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.api.v1.dns.doc.DnsModelDescription;
import com.sequenceiq.freeipa.api.v1.dns.validation.AddDnsPtrRecordRequestValidatorGroup;
import com.sequenceiq.freeipa.api.v1.dns.validation.ValidAddDnsPtrRecordRequest;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AddDnsPtrRecordV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@GroupSequence({AddDnsPtrRecordRequest.class, AddDnsPtrRecordRequestValidatorGroup.class})
@ValidAddDnsPtrRecordRequest(groups = AddDnsPtrRecordRequestValidatorGroup.class)
public class AddDnsPtrRecordRequest {

    @ResourceCrn
    @NotNull
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    private String environmentCrn;

    @NotEmpty
    @Schema(description = DnsModelDescription.FQDN, required = true)
    @Pattern(regexp = DNS_HOSTNAME_PATTERN, message = DNS_FQDN_MSG)
    private String fqdn;

    @NotEmpty
    @Schema(description = DnsModelDescription.IP, required = true)
    @Pattern(regexp = DNS_IP_PATTERN, message = DNS_IP_MSG)
    private String ip;

    @Schema(description = DnsModelDescription.DNS_ZONE)
    @Pattern(regexp = DNS_REVERSEZONE_PATTERN, message = DNS_REVERSEZONE_MSG)
    private String reverseDnsZone;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public String getReverseDnsZone() {
        return reverseDnsZone;
    }

    public void setReverseDnsZone(String reverseDnsZone) {
        this.reverseDnsZone = reverseDnsZone;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public String toString() {
        return "AddDnsPtrRecordRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", fqdn='" + fqdn + '\'' +
                ", ip='" + ip + '\'' +
                ", reverseDnsZone='" + reverseDnsZone + '\'' +
                '}';
    }
}
