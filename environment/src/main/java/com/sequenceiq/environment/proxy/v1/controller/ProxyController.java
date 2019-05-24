package com.sequenceiq.environment.proxy.v1.controller;

import static com.sequenceiq.environment.TempConstants.TEMP_ACCOUNT_ID;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponses;
import com.sequenceiq.environment.proxy.v1.converter.ProxyConfigToProxyRequestConverter;
import com.sequenceiq.environment.proxy.v1.converter.ProxyConfigToProxyResponseConverter;
import com.sequenceiq.environment.proxy.v1.converter.ProxyRequestToProxyConfigConverter;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.notification.NotificationController;
import com.sequenceiq.notification.ResourceEvent;

@Controller
@Transactional(TxType.NEVER)
public class ProxyController extends NotificationController implements ProxyEndpoint {

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private ProxyConfigToProxyRequestConverter proxyConfigToProxyRequestConverter;

    @Inject
    private ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter;

    @Inject
    private ProxyRequestToProxyConfigConverter proxyRequestToProxyConfigConverter;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public ProxyResponses list() {
        String accountId = getAccountId();
        Set<ProxyConfig> listInAccount = proxyConfigService.listInAccount(accountId);
        return new ProxyResponses(new HashSet<>(proxyConfigToProxyResponseConverter.convert(listInAccount)));
    }

    @Override
    public ProxyResponse get(String name) {
        String accountId = getAccountId();
        ProxyConfig config = proxyConfigService.getByNameForAccountId(name, accountId);
        return proxyConfigToProxyResponseConverter.convert(config);
    }

    @Override
    public ProxyResponse post(ProxyRequest request) {
        String accountId = getAccountId();
        ProxyConfig proxyConfig = proxyRequestToProxyConfigConverter.convert(request);
        notify(ResourceEvent.PROXY_CONFIG_CREATED);
        return proxyConfigToProxyResponseConverter
                .convert(proxyConfigService.create(proxyConfig, accountId));
    }

    @Override
    public ProxyResponse delete(String name) {
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return proxyConfigToProxyResponseConverter.convert(proxyConfigService.deleteInAccount(name, getAccountId()));
    }

    @Override
    public ProxyResponses deleteMultiple(Set<String> names) {
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        Set<ProxyConfig> responses = proxyConfigService.deleteMultipleInAccount(names, getAccountId());
        return new ProxyResponses(responses
                .stream()
                .map(p -> proxyConfigToProxyResponseConverter.convert(p))
                .collect(Collectors.toSet()));
    }

    @Override
    public ProxyRequest getRequest(String name) {
        ProxyConfig proxyConfig = proxyConfigService.getByNameForAccountId(name, TEMP_ACCOUNT_ID);
        return proxyConfigToProxyRequestConverter.convert(proxyConfig);
    }

    private String getAccountId() {
        CloudbreakUser cbUser = authenticatedUserService.getCbUser();
        return cbUser.getTenant();
    }
}
