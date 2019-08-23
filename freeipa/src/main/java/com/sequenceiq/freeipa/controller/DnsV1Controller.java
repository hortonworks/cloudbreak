package com.sequenceiq.freeipa.controller;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsResponse;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsRecordService;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsZoneService;
import com.sequenceiq.freeipa.util.CrnService;

public class DnsV1Controller implements DnsV1Endpoint {
    @Inject
    private CrnService crnService;

    @Inject
    private DnsZoneService dnsZoneService;

    @Inject
    private DnsRecordService dnsRecordService;

    @Override
    public AddDnsZoneForSubnetsResponse addDnsZoneForSubnets(AddDnsZoneForSubnetsRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return dnsZoneService.addDnsZonesForSubnets(request, accountId);
    }

    @Override
    public AddDnsZoneForSubnetsResponse addDnsZoneForSubnetIds(@Valid AddDnsZoneForSubnetIdsRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return dnsZoneService.addDnsZonesForSubnetIds(request, accountId);
    }

    @Override
    public Set<String> listDnsZones(String environmentCrn) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return dnsZoneService.listDnsZones(environmentCrn, accountId);
    }

    @Override
    public void deleteDnsZoneBySubnet(String environmentCrn, String subnet) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        dnsZoneService.deleteDnsZoneBySubnet(environmentCrn, accountId, subnet);
    }

    @Override
    public void deleteDnsZoneBySubnetId(String environmentCrn, String networkId, String subnetId) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        dnsZoneService.deleteDnsZoneBySubnetId(environmentCrn, accountId, networkId, subnetId);
    }

    @Override
    public void deleteDnsRecordsByFqdn(@NotEmpty String environmentCrn, @NotEmpty List<String> fqdns) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        dnsRecordService.deleteDnsRecordByFqdn(environmentCrn, accountId, fqdns);
    }
}
