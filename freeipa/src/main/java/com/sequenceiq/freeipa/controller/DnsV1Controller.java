package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsARecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsCnameRecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsResponse;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionWrapper;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsRecordService;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsZoneService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
public class DnsV1Controller implements DnsV1Endpoint {
    @Inject
    private CrnService crnService;

    @Inject
    private DnsZoneService dnsZoneService;

    @Inject
    private DnsRecordService dnsRecordService;

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public AddDnsZoneForSubnetsResponse addDnsZoneForSubnets(@RequestObject AddDnsZoneForSubnetsRequest request) {
        String accountId = crnService.getCurrentAccountId();
        try {
            return dnsZoneService.addDnsZonesForSubnets(request, accountId);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public AddDnsZoneForSubnetsResponse addDnsZoneForSubnetIds(@RequestObject @Valid AddDnsZoneForSubnetIdsRequest request) {
        String accountId = crnService.getCurrentAccountId();
        try {
            return dnsZoneService.addDnsZonesForSubnetIds(request, accountId);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public Set<String> listDnsZones(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        try {
            return dnsZoneService.listDnsZones(environmentCrn, accountId);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public void deleteDnsZoneBySubnet(@ResourceCrn String environmentCrn, String subnet) {
        String accountId = crnService.getCurrentAccountId();
        try {
            dnsZoneService.deleteDnsZoneBySubnet(environmentCrn, accountId, subnet);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public void deleteDnsZoneBySubnetId(@ResourceCrn String environmentCrn, String networkId, String subnetId) {
        String accountId = crnService.getCurrentAccountId();
        try {
            dnsZoneService.deleteDnsZoneBySubnetId(environmentCrn, accountId, networkId, subnetId);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public void deleteDnsRecordsByFqdn(@ResourceCrn @NotEmpty String environmentCrn, @NotEmpty List<String> fqdns) {
        String accountId = crnService.getCurrentAccountId();
        try {
            dnsRecordService.deleteDnsRecordByFqdn(environmentCrn, accountId, fqdns);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public void addDnsARecord(@RequestObject AddDnsARecordRequest request) {
        String accountId = crnService.getCurrentAccountId();
        try {
            dnsRecordService.addDnsARecord(accountId, request);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public void deleteDnsARecord(@ResourceCrn String environmentCrn, String dnsZone, String hostname) {
        String accountId = crnService.getCurrentAccountId();
        try {
            dnsRecordService.deleteDnsRecord(accountId, environmentCrn, dnsZone, hostname);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public void addDnsCnameRecord(@RequestObject AddDnsCnameRecordRequest request) {
        String accountId = crnService.getCurrentAccountId();
        try {
            dnsRecordService.addDnsCnameRecord(accountId, request);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public void deleteDnsCnameRecord(@ResourceCrn String environmentCrn, String dnsZone, String cname) {
        String accountId = crnService.getCurrentAccountId();
        try {
            dnsRecordService.deleteDnsRecord(accountId, environmentCrn, dnsZone, cname);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }
}
