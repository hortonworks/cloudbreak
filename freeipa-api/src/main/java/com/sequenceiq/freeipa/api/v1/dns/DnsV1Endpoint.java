package com.sequenceiq.freeipa.api.v1.dns;

import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_CNAME_MSG;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_CNAME_PATTERN;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_HOSTNAME_MSG;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_HOSTNAME_PATTERN;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_ZONE_MSG;
import static com.sequenceiq.freeipa.api.v1.dns.model.DnsRecordRegexpPatterns.DNS_ZONE_PATTERN;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.dns.doc.DnsOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsARecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsCnameRecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsPtrRecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsResponse;
import com.sequenceiq.freeipa.api.v1.dns.model.DeleteDnsPtrRecordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/dns")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/dns", description = "Manage DNS in FreeIPA")
public interface DnsV1Endpoint {
    @POST
    @Path("zone/cidr")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.ADD_DNS_ZONE_FOR_SUBNETS, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "addDnsZoneForSubnetsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AddDnsZoneForSubnetsResponse addDnsZoneForSubnets(@Valid AddDnsZoneForSubnetsRequest request);

    @POST
    @Path("zone/id")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.ADD_DNS_ZONE_FOR_SUBNET_IDS, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "addDnsZoneForSubnetIdsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AddDnsZoneForSubnetsResponse addDnsZoneForSubnetIds(@Valid AddDnsZoneForSubnetIdsRequest request);

    @GET
    @Path("zone")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.LIST_DNS_ZONES, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "listDnsZonesV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Set<String> listDnsZones(@QueryParam("environment") @NotEmpty String environmentCrn);

    @DELETE
    @Path("zone/cidr")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.DELETE_DNS_ZONE_BY_SUBNET, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "deleteDnsZoneBySubnetV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteDnsZoneBySubnet(@QueryParam("environment") @NotEmpty String environmentCrn,
            @QueryParam("subnet") @NotEmpty
            @Pattern(regexp = "(^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\/([0-9]|[1-2][0-9]|3[0-2]))$)",
                    message = "Must be in valid CIDR format eg. 192.168.1.0/24") String subnet);

    @DELETE
    @Path("zone/id")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.DELETE_DNS_ZONE_BY_SUBNET_ID, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "deleteDnsZoneBySubnetIdV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteDnsZoneBySubnetId(@QueryParam("environment") @NotEmpty String environmentCrn, @QueryParam("networkId") @NotEmpty String networkId,
            @QueryParam("subnetId") @NotEmpty String subnetId);

    @DELETE
    @Path("record")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.DELETE_DNS_RECORD_BY_FQDN, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "deleteDnsRecordByFqdnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteDnsRecordsByFqdn(@QueryParam("environment") @NotEmpty String environmentCrn, @QueryParam("fqdn") @NotEmpty List<String> fqdns);

    @POST
    @Path("record/a")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.ADD_DNS_A_RECORD, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "addDnsARecordV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void addDnsARecord(@Valid @NotNull AddDnsARecordRequest request);

    @POST
    @Path("record/a/internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.ADD_DNS_A_RECORD, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "addDnsARecordV1Internal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void addDnsARecordInternal(@QueryParam("accountId") @NotEmpty String accountId, @Valid @NotNull AddDnsARecordRequest request);

    @DELETE
    @Path("record/a")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.DELETE_DNS_A_RECORD, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "deleteDnsARecordV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteDnsARecord(@QueryParam("environment") @NotEmpty String environmentCrn,
            @QueryParam("dnsZone") @Pattern(regexp = DNS_ZONE_PATTERN, message = DNS_ZONE_MSG) String dnsZone,
            @QueryParam("hostname") @NotEmpty @Pattern(regexp = DNS_HOSTNAME_PATTERN, message = DNS_HOSTNAME_MSG) String hostname);

    @POST
    @Path("record/cname")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.ADD_DNS_CNAME_RECORD, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "addDnsCnameRecordV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void addDnsCnameRecord(@Valid @NotNull AddDnsCnameRecordRequest request);

    @POST
    @Path("record/cname/internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.ADD_DNS_CNAME_RECORD, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "addDnsCnameRecordV1Internal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void addDnsCnameRecordInternal(@QueryParam("accountId") @NotEmpty String accountId, @Valid @NotNull AddDnsCnameRecordRequest request);

    @DELETE
    @Path("record/cname")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.DELETE_DNS_CNAME_RECORD, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "deleteDnsCnameRecordV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteDnsCnameRecord(@QueryParam("environment") @NotEmpty String environmentCrn,
            @QueryParam("dnsZone") @Pattern(regexp = DNS_ZONE_PATTERN, message = DNS_ZONE_MSG) String dnsZone,
            @QueryParam("cname") @NotEmpty @Pattern(regexp = DNS_CNAME_PATTERN, message = DNS_CNAME_MSG) String cname);

    @POST
    @Path("record/ptr")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.ADD_DNS_PTR_RECORD, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "addDnsPtrRecordV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void addDnsPtrRecord(@Valid @NotNull AddDnsPtrRecordRequest request);

    @DELETE
    @Path("record/ptr")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DnsOperationDescriptions.DELETE_DNS_PTR_RECORD, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "deleteDnsPtrRecordV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteDnsPtrRecord(@Valid @NotNull @BeanParam DeleteDnsPtrRecordRequest deleteDnsPtrRecordRequest);
}
