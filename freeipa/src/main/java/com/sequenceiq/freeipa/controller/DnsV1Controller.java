package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsARecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsCnameRecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsPtrRecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsResponse;
import com.sequenceiq.freeipa.api.v1.dns.model.DeleteDnsPtrRecordRequest;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionWrapper;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsPtrRecordService;
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

    @Inject
    private DnsPtrRecordService dnsPtrRecordService;

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
    public AddDnsZoneForSubnetsResponse addDnsZoneForSubnetIds(@RequestObject AddDnsZoneForSubnetIdsRequest request) {
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
    public void deleteDnsRecordsByFqdn(@ResourceCrn String environmentCrn, List<String> fqdns) {
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
        addDnsARecordCommon(request, accountId);
    }

    private void addDnsARecordCommon(@RequestObject AddDnsARecordRequest request, String accountId) {
        try {
            dnsRecordService.addDnsARecord(accountId, request);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @InternalOnly
    public void addDnsARecordInternal(@AccountId String accountId, AddDnsARecordRequest request) {
        addDnsARecordCommon(request, accountId);
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
        addDnsCnameRecordCommon(request, accountId);
    }

    private void addDnsCnameRecordCommon(@RequestObject AddDnsCnameRecordRequest request, String accountId) {
        try {
            dnsRecordService.addDnsCnameRecord(accountId, request);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @InternalOnly
    public void addDnsCnameRecordInternal(@AccountId String accountId, AddDnsCnameRecordRequest request) {
        addDnsCnameRecordCommon(request, accountId);
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

    @Override
    @CheckPermissionByRequestProperty(type = CRN, path = "environmentCrn", action = EDIT_ENVIRONMENT)
    public void addDnsPtrRecord(@RequestObject AddDnsPtrRecordRequest request) {
        try {
            dnsPtrRecordService.addDnsPtrRecord(request, crnService.getCurrentAccountId());
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByRequestProperty(type = CRN, path = "environmentCrn", action = EDIT_ENVIRONMENT)
    public void deleteDnsPtrRecord(@RequestObject DeleteDnsPtrRecordRequest deleteDnsPtrRecordRequest) {
        try {
            dnsPtrRecordService.deleteDnsPtrRecord(deleteDnsPtrRecordRequest, crnService.getCurrentAccountId());
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }
}
