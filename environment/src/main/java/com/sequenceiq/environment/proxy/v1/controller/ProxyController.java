package com.sequenceiq.environment.proxy.v1.controller;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponses;
import com.sequenceiq.environment.authorization.ProxyFiltering;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.environment.proxy.v1.converter.ProxyConfigToProxyResponseConverter;
import com.sequenceiq.environment.proxy.v1.converter.ProxyRequestToProxyConfigConverter;
import com.sequenceiq.notification.WebSocketNotificationController;

@Controller
@Transactional(TxType.NEVER)
public class ProxyController extends WebSocketNotificationController implements ProxyEndpoint {

    private final ProxyConfigService proxyConfigService;

    private final ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter;

    private final ProxyRequestToProxyConfigConverter proxyRequestToProxyConfigConverter;

    private final ProxyFiltering proxyFiltering;

    public ProxyController(ProxyConfigService proxyConfigService,
            ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter,
            ProxyRequestToProxyConfigConverter proxyRequestToProxyConfigConverter,
            ProxyFiltering proxyFiltering) {
        this.proxyConfigService = proxyConfigService;
        this.proxyConfigToProxyResponseConverter = proxyConfigToProxyResponseConverter;
        this.proxyRequestToProxyConfigConverter = proxyRequestToProxyConfigConverter;
        this.proxyFiltering = proxyFiltering;
    }

    @Override
    @FilterListBasedOnPermissions
    public ProxyResponses list() {
        return proxyFiltering.filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()),
                AuthorizationResourceAction.DESCRIBE_PROXY, Map.of());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_PROXY)
    public ProxyResponse getByName(@ResourceName String name) {
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
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public ProxyResponse getByEnvironmentCrn(@ResourceCrn String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        ProxyConfig proxyConfig = proxyConfigService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        return proxyConfigToProxyResponseConverter.convert(proxyConfig);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_PROXY)
    public ProxyResponse getByResourceCrn(@ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        ProxyConfig config = proxyConfigService.getByCrnForAccountId(crn, accountId);
        return proxyConfigToProxyResponseConverter.convert(config);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_PROXY)
    public ProxyResponse post(ProxyRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        ProxyConfig proxyConfig = proxyRequestToProxyConfigConverter.convert(request);
        notify(ResourceEvent.PROXY_CONFIG_CREATED);
        return proxyConfigToProxyResponseConverter
                .convert(proxyConfigService.create(proxyConfig, accountId, creator));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_PROXY)
    public ProxyResponse deleteByName(@ResourceName String name) {
        ProxyResponse proxyResponse = proxyConfigToProxyResponseConverter.convert(
                proxyConfigService.deleteByNameInAccount(name, ThreadBasedUserCrnProvider.getAccountId()));
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return proxyResponse;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_PROXY)
    public ProxyResponse deleteByCrn(@ResourceCrn String crn) {
        ProxyResponse proxyResponse = proxyConfigToProxyResponseConverter.convert(
                proxyConfigService.deleteByCrnInAccount(crn, ThreadBasedUserCrnProvider.getAccountId()));
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return proxyResponse;
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_PROXY)
    public ProxyResponses deleteMultiple(@ResourceNameList Set<String> names) {
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        Set<ProxyConfig> responses = proxyConfigService.deleteMultipleInAccount(names, ThreadBasedUserCrnProvider.getAccountId());
        return new ProxyResponses(responses
                .stream()
                .map(p -> proxyConfigToProxyResponseConverter.convert(p))
                .collect(Collectors.toSet()));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_PROXY)
    public ProxyRequest getRequest(@ResourceName String name) {
        throw new UnsupportedOperationException("not supported request");
    }
}
