package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.authorization.OrganizationResource.STACK;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackValidationRequest;
import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action;
import com.sequenceiq.cloudbreak.authorization.PermissionCheckerService;
import com.sequenceiq.cloudbreak.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Transactional(TxType.NEVER)
public class StackV1Controller extends NotificationController implements StackV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV1Controller.class);

    @Autowired
    private StackCreatorService stackCreatorService;

    @Autowired
    private StackCommonService stackCommonService;

    @Inject
    private StackService stackService;

    @Inject
    private UserService userService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private PermissionCheckerService permissionCheckerService;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Override
    public StackResponse postPrivate(StackRequest stackRequest) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return stackCommonService.createInOrganization(stackRequest, identityUser, user, organization);
    }

    @Override
    public StackResponse postPublic(StackRequest stackRequest) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return stackCommonService.createInOrganization(stackRequest, identityUser, user, organization);
    }

    @Override
    public Set<StackResponse> getStacksInDefaultOrg() {
        return stackCommonService.getStacksInDefaultOrg();
    }

    @Override
    public Set<StackResponse> getPublics() {
        return stackCommonService.getStacksInDefaultOrg();
    }

    @Override
    public StackResponse getStackFromDefaultOrg(String name, Set<String> entries) {
        return stackCommonService.getStackFromDefaultOrg(name, entries);
    }

    @Override
    public StackResponse getPublic(String name, Set<String> entries) {
        return stackCommonService.getStackFromDefaultOrg(name, entries);
    }

    @Override
    public StackResponse get(Long id, Set<String> entries) {
        return stackCommonService.get(id, entries);
    }

    @Override
    public void deleteInDefaultOrg(String name, Boolean forced, Boolean deleteDependencies) {
        stackCommonService.deleteInDefaultOrg(name, forced, deleteDependencies);
    }

    @Override
    public void deletePrivate(String name, Boolean forced, Boolean deleteDependencies) {
        stackCommonService.deleteInDefaultOrg(name, forced, deleteDependencies);
    }

    @Override
    public void deleteById(Long id, Boolean forced, Boolean deleteDependencies) {
        stackCommonService.deleteById(id, forced, deleteDependencies);
    }

    @Override
    public Response put(Long id, UpdateStackJson updateRequest) {
        return stackCommonService.putInDefaultOrg(id, updateRequest);
    }

    @Override
    public Map<String, Object> status(Long id) {
        return stackCommonService.status(id);
    }

    @Override
    public PlatformVariantsJson variants() {
        return stackCommonService.variants();
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId) {
        return stackCommonService.deleteInstance(stackId, instanceId, false);
    }

    @Override
    public Response deleteInstance(Long stackId, String instanceId, boolean forced) {
        return stackCommonService.deleteInstance(stackId, instanceId, forced);
    }

    @Override
    public Response deleteInstances(Long stackId, Set<String> instanceIds) {
        return stackCommonService.deleteInstances(stackId, instanceIds);
    }

    @Override
    public CertificateResponse getCertificate(Long stackId) {
        return stackCommonService.getCertificate(stackId);
    }

    @Override
    public Response validate(StackValidationRequest stackValidationRequest) {
        return stackCommonService.validate(stackValidationRequest);
    }

    @Override
    public StackResponse getStackForAmbari(AmbariAddressJson json) {
        return stackCommonService.getStackForAmbari(json);
    }

    @Override
    public Set<AutoscaleStackResponse> getAllForAutoscale() {
        return stackCommonService.getAllForAutoscale();
    }

    @Override
    public Boolean authorizeForAutoscale(Long id, String owner, String permission) {
        try {
            restRequestThreadLocalService.setIdentityUserByOwner(owner);
            Stack stack = stackService.get(id);
            if (Action.WRITE.name().equalsIgnoreCase(permission)) {
                User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
                permissionCheckingUtils.checkPermissionByOrgIdForUser(stack.getOrganization().getId(), STACK, Action.WRITE, user);
            }
            return true;
        } catch (RuntimeException ignore) {
            return false;
        }
    }
}
