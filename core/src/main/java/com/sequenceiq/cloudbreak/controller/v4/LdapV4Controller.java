package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.LdapConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapTestV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapTestV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(LdapConfig.class)
public class LdapV4Controller extends NotificationController implements LdapConfigV4Endpoint {
    @Inject
    private ConverterUtil converterUtil;

    @Override
    public LdapV4Responses list(Long workspaceId, String environment, Boolean attachGlobal) {
//        Set<LdapConfig> allInWorkspaceAndEnvironment = ldapConfigService.findAllInWorkspace(workspaceId);
//        return new LdapV4Responses(converterUtil.convertAllAsSet(allInWorkspaceAndEnvironment, LdapV4Response.class));
        return new LdapV4Responses();
    }

    @Override
    public LdapV4Response get(Long workspaceId, String name) {
//        LdapConfig ldapConfig = ldapConfigService.getByNameForWorkspaceId(name, workspaceId);
//        return converterUtil.convert(ldapConfig, LdapV4Response.class);
        return null;
    }

    @Override
    public LdapV4Response post(Long workspaceId, LdapV4Request request) {
//        LdapConfig ldapConfig = converterUtil.convert(request, LdapConfig.class);
//        ldapConfig = ldapConfigService.createForLoggedInUser(ldapConfig, workspaceId);
//        notify(ResourceEvent.LDAP_CREATED);
//        return converterUtil.convert(ldapConfig, LdapV4Response.class);
        return null;
    }

    @Override
    public LdapV4Response delete(Long workspaceId, String name) {
//        LdapConfig config = ldapConfigService.deleteByNameFromWorkspace(name, workspaceId);
//        notify(ResourceEvent.LDAP_DELETED);
//        return converterUtil.convert(config, LdapV4Response.class);
        return null;
    }

    @Override
    public LdapV4Responses deleteMultiple(Long workspaceId, Set<String> names) {
//        Set<LdapConfig> deleted = ldapConfigService.deleteMultipleByNameFromWorkspace(names, workspaceId);
//        notify(ResourceEvent.LDAP_DELETED);
//        return new LdapV4Responses(converterUtil.convertAllAsSet(deleted, LdapV4Response.class));
        return null;
    }

    @Override
    public LdapTestV4Response test(Long workspaceId, LdapTestV4Request ldapValidationRequest) {
//        LdapTestV4Response ldapTestV4Response = new LdapTestV4Response();
//        ldapTestV4Response.setResult(ldapConfigService.testConnection(workspaceId,
//                ldapValidationRequest.getExistingLdapName(), ldapValidationRequest.getLdap()));
//        return ldapTestV4Response;
        return null;
    }

    @Override
    public LdapV4Request getRequest(Long workspaceId, String name) {
//        LdapConfig ldapConfig = ldapConfigService.getByNameForWorkspaceId(name, workspaceId);
//        return converterUtil.convert(ldapConfig, LdapV4Request.class);
        return null;
    }
}
