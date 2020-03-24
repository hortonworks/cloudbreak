package com.sequenceiq.freeipa.controller;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsResponse;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsRecordService;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsZoneService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
@AuthorizationResource(type = AuthorizationResourceType.ENVIRONMENT)
public class DnsV1Controller implements DnsV1Endpoint {
    @Inject
    private CrnService crnService;

    @Inject
    private DnsZoneService dnsZoneService;

    @Inject
    private DnsRecordService dnsRecordService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public AddDnsZoneForSubnetsResponse addDnsZoneForSubnets(AddDnsZoneForSubnetsRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return dnsZoneService.addDnsZonesForSubnets(request, accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public AddDnsZoneForSubnetsResponse addDnsZoneForSubnetIds(@Valid AddDnsZoneForSubnetIdsRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return dnsZoneService.addDnsZonesForSubnetIds(request, accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public Set<String> listDnsZones(String environmentCrn) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return dnsZoneService.listDnsZones(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void deleteDnsZoneBySubnet(String environmentCrn, String subnet) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        dnsZoneService.deleteDnsZoneBySubnet(environmentCrn, accountId, subnet);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void deleteDnsZoneBySubnetId(String environmentCrn, String networkId, String subnetId) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        dnsZoneService.deleteDnsZoneBySubnetId(environmentCrn, accountId, networkId, subnetId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void deleteDnsRecordsByFqdn(@NotEmpty String environmentCrn, @NotEmpty List<String> fqdns) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        dnsRecordService.deleteDnsRecordByFqdn(environmentCrn, accountId, fqdns);
    }
}
