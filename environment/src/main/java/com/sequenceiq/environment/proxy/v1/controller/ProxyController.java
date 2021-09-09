package com.sequenceiq.environment.proxy.v1.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponses;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.environment.proxy.v1.converter.ProxyConfigToProxyResponseConverter;
import com.sequenceiq.environment.proxy.v1.converter.ProxyRequestToProxyConfigConverter;
import com.sequenceiq.notification.NotificationController;

@Controller
@Transactional(TxType.NEVER)
public class ProxyController extends NotificationController implements ProxyEndpoint {

    private final ProxyConfigService proxyConfigService;

    private final ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter;

    private final ProxyRequestToProxyConfigConverter proxyRequestToProxyConfigConverter;

    public ProxyController(ProxyConfigService proxyConfigService,
            ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter,
            ProxyRequestToProxyConfigConverter proxyRequestToProxyConfigConverter) {
        this.proxyConfigService = proxyConfigService;
        this.proxyConfigToProxyResponseConverter = proxyConfigToProxyResponseConverter;
        this.proxyRequestToProxyConfigConverter = proxyRequestToProxyConfigConverter;
    }

    @Override
    @DisableCheckPermissions
    public ProxyResponses list() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Set<ProxyConfig> listInAccount = proxyConfigService.listInAccount(accountId);
        return new ProxyResponses(listInAccount.stream()
                .map(proxy -> proxyConfigToProxyResponseConverter.convert(proxy))
                .collect(Collectors.toSet()));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public ProxyResponse getByName(String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        ProxyConfig config = proxyConfigService.getByNameForAccountId(name, accountId);
        return proxyConfigToProxyResponseConverter.convert(config);
    }

    @Override
    @InternalOnly
    public String getCrnByAccountIdAndName(@AccountId String accountId, String name) {
        return proxyConfigService.getByNameForAccountId(name, accountId).getResourceCrn();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public ProxyResponse getByEnvironmentCrn(@TenantAwareParam String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        ProxyConfig proxyConfig = proxyConfigService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        return proxyConfigToProxyResponseConverter.convert(proxyConfig);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public ProxyResponse getByResourceCrn(@TenantAwareParam String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        ProxyConfig config = proxyConfigService.getByCrnForAccountId(crn, accountId);
        return proxyConfigToProxyResponseConverter.convert(config);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public ProxyResponse post(ProxyRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        ProxyConfig proxyConfig = proxyRequestToProxyConfigConverter.convert(request);
        notify(ResourceEvent.PROXY_CONFIG_CREATED);
        return proxyConfigToProxyResponseConverter
                .convert(proxyConfigService.create(proxyConfig, accountId, creator));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public ProxyResponse deleteByName(String name) {
        ProxyResponse proxyResponse = proxyConfigToProxyResponseConverter.convert(
                proxyConfigService.deleteByNameInAccount(name, ThreadBasedUserCrnProvider.getAccountId()));
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return proxyResponse;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public ProxyResponse deleteByCrn(String crn) {
        ProxyResponse proxyResponse = proxyConfigToProxyResponseConverter.convert(
                proxyConfigService.deleteByCrnInAccount(crn, ThreadBasedUserCrnProvider.getAccountId()));
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return proxyResponse;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public ProxyResponses deleteMultiple(Set<String> names) {
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        Set<ProxyConfig> responses = proxyConfigService.deleteMultipleInAccount(names, ThreadBasedUserCrnProvider.getAccountId());
        return new ProxyResponses(responses
                .stream()
                .map(p -> proxyConfigToProxyResponseConverter.convert(p))
                .collect(Collectors.toSet()));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public ProxyRequest getRequest(String name) {
        throw new UnsupportedOperationException("not supported request");
    }
}
