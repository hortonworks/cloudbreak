package com.sequenceiq.freeipa.api.v1.dns.model;

import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.CNAME_TARGET_REGEXP;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_CNAME_MSG;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_CNAME_PATTERN;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_ZONE_MSG;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_ZONE_PATTERN;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.dns.doc.DnsModelDescription;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AddDnsCnameRecordV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddDnsCnameRecordRequest {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotEmpty
    @ApiModelProperty(value = DnsModelDescription.CNAME, required = true)
    @Pattern(regexp = DNS_CNAME_PATTERN, message = DNS_CNAME_MSG)
    private String cname;

    @ApiModelProperty(DnsModelDescription.DNS_ZONE)
    @Pattern(regexp = DNS_ZONE_PATTERN, message = DNS_ZONE_MSG)
    private String dnsZone;

    @NotEmpty
    @ApiModelProperty(value = DnsModelDescription.CNAME_TARGET_FQDN, required = true)
    @Pattern(regexp = CNAME_TARGET_REGEXP,
            message = "Target FQDN must be valid. Might start with '*.' and can contain alphanumeric characters, dash and dot.")
    private String targetFqdn;

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

    @Override
    public String toString() {
        return "AddDnsCnameRecordRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", cname='" + cname + '\'' +
                ", dnsZone='" + dnsZone + '\'' +
                ", targetFqdn='" + targetFqdn + '\'' +
                '}';
    }
}
