package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.LdapConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.ldap.LDAPTestRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapTestResult;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapValidationRequest;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
@Transactional(TxType.NEVER)
public class LdapController extends NotificationController implements LdapConfigEndpoint {

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private LdapConfigValidator ldapConfigValidator;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public LdapConfigResponse postPrivate(LdapConfigRequest ldapConfigRequest) {
        return createConfig(ldapConfigRequest);
    }

    @Override
    public LdapConfigResponse postPublic(LdapConfigRequest ldapConfigRequest) {
        return createConfig(ldapConfigRequest);
    }

    @Override
    public Set<LdapConfigResponse> getPrivates() {
        return listForUsersDefaultWorkspace();
    }

    @Override
    public Set<LdapConfigResponse> getPublics() {
        return listForUsersDefaultWorkspace();
    }

    @Override
    public LdapConfigResponse getPrivate(String name) {
        return getLdapConfigResponse(name);
    }

    @Override
    public LdapConfigResponse getPublic(String name) {
        return getLdapConfigResponse(name);
    }

    @Override
    public LdapConfigResponse get(Long id) {
        LdapConfig config = ldapConfigService.get(id);
        return conversionService.convert(config, LdapConfigResponse.class);
    }

    @Override
    public void delete(Long id) {
        executeAndNotify(user -> ldapConfigService.delete(id), ResourceEvent.LDAP_DELETED);
    }

    @Override
    public void deletePublic(String name) {
        deleteInDefaultWorkspace(name);
    }

    @Override
    public void deletePrivate(String name) {
        deleteInDefaultWorkspace(name);
    }

    @Override
    public LdapTestResult testLdapConnection(LDAPTestRequest ldapTestRequest) {
        String existingLDAPConfigName = ldapTestRequest.getName();
        LdapValidationRequest validationRequest = ldapTestRequest.getValidationRequest();
        if (existingLDAPConfigName == null && validationRequest == null) {
            throw new BadRequestException("Either an existing resource 'id' or an LDAP 'validationRequest' needs to be specified in the request. ");
        }

        LdapTestResult ldapTestResult = new LdapTestResult();
        try {
            if (existingLDAPConfigName != null) {
                User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
                Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
                LdapConfig ldapConfig = ldapConfigService.getByNameForWorkspaceId(existingLDAPConfigName, workspace.getId());
                ldapConfigValidator.validateLdapConnection(ldapConfig);
            } else {
                ldapConfigValidator.validateLdapConnection(validationRequest);
            }
            ldapTestResult.setConnectionResult("connected");
        } catch (BadRequestException e) {
            ldapTestResult.setConnectionResult(e.getMessage());
        }
        return ldapTestResult;
    }

    @Override
    public LdapConfigRequest getRequestFromName(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        LdapConfig ldapConfig = ldapConfigService.getByNameForWorkspaceId(name, workspace.getId());
        return conversionService.convert(ldapConfig, LdapConfigRequest.class);
    }

    private LdapConfigResponse createConfig(LdapConfigRequest request) {
        LdapConfig ldapConfig = conversionService.convert(request, LdapConfig.class);
        LdapConfig response = ldapConfigService.createInEnvironment(ldapConfig, request.getEnvironments(),
                restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.LDAP_CREATED);
        return conversionService.convert(response, LdapConfigResponse.class);
    }

    private LdapConfigResponse getLdapConfigResponse(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return conversionService.convert(ldapConfigService.getByNameForWorkspace(name, workspace), LdapConfigResponse.class);
    }

    private Set<LdapConfigResponse> listForUsersDefaultWorkspace() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return ldapConfigService.findAllByWorkspace(workspace).stream()
                .map(ldapConfig -> conversionService.convert(ldapConfig, LdapConfigResponse.class))
                .collect(Collectors.toSet());
    }

    private void deleteInDefaultWorkspace(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        executeAndNotify(identityUser -> ldapConfigService.deleteByNameFromWorkspace(name, workspace.getId()), ResourceEvent.LDAP_DELETED);
    }
}
