package com.sequenceiq.freeipa.api.v1.dns;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.freeipa.api.v1.dns.doc.DnsOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.service.api.doc.ContentType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/dns")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/dns", description = "Amange DNS in FreeIPA", protocols = "http,https")
public interface DnsV1Endpoint {
    @POST
    @Path("zone/cidr")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DnsOperationDescriptions.ADD_DNS_ZONE_FOR_SUBNETS, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "addDnsZoneForSubnetsV1")
    AddDnsZoneForSubnetsResponse addDnsZoneForSubnets(@Valid AddDnsZoneForSubnetsRequest request) throws Exception;

    @POST
    @Path("zone/id")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DnsOperationDescriptions.ADD_DNS_ZONE_FOR_SUBNET_IDS, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "addDnsZoneForSubnetIdsV1")
    AddDnsZoneForSubnetsResponse addDnsZoneForSubnetIds(@Valid AddDnsZoneForSubnetIdsRequest request) throws Exception;

    @GET
    @Path("zone")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DnsOperationDescriptions.LIST_DNS_ZONES, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "listDnsZonesV1")
    Set<String> listDnsZones(@QueryParam("environment") @NotEmpty String environmentCrn) throws Exception;

    @DELETE
    @Path("zone/cidr")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DnsOperationDescriptions.DELETE_DNS_ZONE_BY_SUBNET, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "deleteDnsZoneBySubnetV1")
    void deleteDnsZoneBySubnet(@QueryParam("environment") @NotEmpty String environmentCrn,
            @QueryParam("subnet") @NotEmpty
            @Pattern(regexp = "(^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\/([0-9]|[1-2][0-9]|3[0-2]))$)",
                    message = "Must be in valid CIDR format eg. 192.168.1.0/24") String subnet) throws Exception;

    @DELETE
    @Path("zone/id")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DnsOperationDescriptions.DELETE_DNS_ZONE_BY_SUBNET_ID, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "deleteDnsZoneBySubnetIdV1")
    void deleteDnsZoneBySubnetId(@QueryParam("environment") @NotEmpty String environmentCrn, @QueryParam("networkId") @NotEmpty String networkId,
            @QueryParam("subnetId") @NotEmpty String subnetId) throws Exception;

    @DELETE
    @Path("record")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DnsOperationDescriptions.DELETE_DNS_RECORD_BY_FQDN, produces = ContentType.JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "deleteDnsRecordByFqdnV1")
    void deleteDnsRecordsByFqdn(@QueryParam("environment") @NotEmpty String environmentCrn, @QueryParam("fqdn") @NotEmpty List<String> fqdns) throws Exception;
}
