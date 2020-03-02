package com.sequenceiq.environment.proxy.v1.controller;

import static com.sequenceiq.environment.TempConstants.TEMP_ACCOUNT_ID;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponses;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.environment.proxy.v1.converter.ProxyConfigToProxyRequestConverter;
import com.sequenceiq.environment.proxy.v1.converter.ProxyConfigToProxyResponseConverter;
import com.sequenceiq.environment.proxy.v1.converter.ProxyRequestToProxyConfigConverter;
import com.sequenceiq.notification.NotificationController;
import com.sequenceiq.cloudbreak.event.ResourceEvent;

@Controller
@AuthorizationResource(type = AuthorizationResourceType.ENVIRONMENT)
@Transactional(TxType.NEVER)
public class ProxyController extends NotificationController implements ProxyEndpoint {

    private final ProxyConfigService proxyConfigService;

    private final ProxyConfigToProxyRequestConverter proxyConfigToProxyRequestConverter;

    private final ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter;

    private final ProxyRequestToProxyConfigConverter proxyRequestToProxyConfigConverter;

    public ProxyController(ProxyConfigService proxyConfigService,
            ProxyConfigToProxyRequestConverter proxyConfigToProxyRequestConverter,
            ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter,
            ProxyRequestToProxyConfigConverter proxyRequestToProxyConfigConverter) {
        this.proxyConfigService = proxyConfigService;
        this.proxyConfigToProxyRequestConverter = proxyConfigToProxyRequestConverter;
        this.proxyConfigToProxyResponseConverter = proxyConfigToProxyResponseConverter;
        this.proxyRequestToProxyConfigConverter = proxyRequestToProxyConfigConverter;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public ProxyResponses list() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Set<ProxyConfig> listInAccount = proxyConfigService.listInAccount(accountId);
        return new ProxyResponses(new HashSet<>(proxyConfigToProxyResponseConverter.convert(listInAccount)));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public ProxyResponse getByName(String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        ProxyConfig config = proxyConfigService.getByNameForAccountId(name, accountId);
        return proxyConfigToProxyResponseConverter.convert(config);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public ProxyResponse getByResourceCrn(String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        ProxyConfig config = proxyConfigService.getByCrnForAccountId(crn, accountId);
        return proxyConfigToProxyResponseConverter.convert(config);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public ProxyResponse post(ProxyRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        ProxyConfig proxyConfig = proxyRequestToProxyConfigConverter.convert(request);
        notify(ResourceEvent.PROXY_CONFIG_CREATED);
        return proxyConfigToProxyResponseConverter
                .convert(proxyConfigService.create(proxyConfig, accountId, creator));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public ProxyResponse deleteByName(String name) {
        ProxyResponse proxyResponse = proxyConfigToProxyResponseConverter.convert(
                proxyConfigService.deleteByNameInAccount(name, ThreadBasedUserCrnProvider.getAccountId()));
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return proxyResponse;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public ProxyResponse deleteByCrn(String crn) {
        ProxyResponse proxyResponse = proxyConfigToProxyResponseConverter.convert(
                proxyConfigService.deleteByCrnInAccount(crn, ThreadBasedUserCrnProvider.getAccountId()));
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return proxyResponse;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public ProxyResponses deleteMultiple(Set<String> names) {
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        Set<ProxyConfig> responses = proxyConfigService.deleteMultipleInAccount(names, ThreadBasedUserCrnProvider.getAccountId());
        return new ProxyResponses(responses
                .stream()
                .map(p -> proxyConfigToProxyResponseConverter.convert(p))
                .collect(Collectors.toSet()));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public ProxyRequest getRequest(String name) {
        ProxyConfig proxyConfig = proxyConfigService.getByNameForAccountId(name, TEMP_ACCOUNT_ID);
        return proxyConfigToProxyRequestConverter.convert(proxyConfig);
    }
}
