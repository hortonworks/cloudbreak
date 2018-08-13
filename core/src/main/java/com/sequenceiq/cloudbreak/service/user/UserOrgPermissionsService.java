package com.sequenceiq.cloudbreak.service.user;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.organization.UserOrgPermissions;
import com.sequenceiq.cloudbreak.repository.organization.UserOrgPermissionsRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;

@Service
public class UserOrgPermissionsService {

    @Inject
    private TransactionService transactionService;

    @Inject
    private UserOrgPermissionsRepository userOrgPermissionsRepository;

    @Inject
    private UserService userService;

    public Set<UserOrgPermissions> findForCurrentUser() {
        User currentUser = userService.getCurrentUser();
        return userOrgPermissionsRepository.findForUser(currentUser);
    }

    public UserOrgPermissions findForCurrentUserByOrganizationId(Long orgId) {
        User currentUser = userService.getCurrentUser();
        return userOrgPermissionsRepository.findForUserByOrganizationId(currentUser, orgId);
    }

    public Set<UserOrgPermissions> findForUser(User user) {
        return userOrgPermissionsRepository.findForUser(user);
    }

    public Set<UserOrgPermissions> findForOrganization(Organization organization) {
        return userOrgPermissionsRepository.findForOrganization(organization);
    }

    public UserOrgPermissions findForUserAndOrganization(User user, Organization organization) {
        return userOrgPermissionsRepository.findForUserAndOrganization(user, organization);
    }

    public UserOrgPermissions findForUserByOrganizationId(User user, Long organizationId) {
        return userOrgPermissionsRepository.findForUserByOrganizationId(user, organizationId);
    }

    public UserOrgPermissions save(UserOrgPermissions userOrgPermissions) {
        return userOrgPermissionsRepository.save(userOrgPermissions);
    }

    public Set<UserOrgPermissions> deleteAll(Set<UserOrgPermissions> toBeDeleted) {
        userOrgPermissionsRepository.deleteAll(toBeDeleted);
        return toBeDeleted;
    }

    public Set<UserOrgPermissions> findForUserByOrganizationIds(User user, Set<Long> orgIds) {
        return userOrgPermissionsRepository.findForUserByOrganizationIds(user, orgIds);
    }

    public Iterable<UserOrgPermissions> saveAll(Set<UserOrgPermissions> userOrgPermsToAdd) {
        return userOrgPermissionsRepository.saveAll(userOrgPermsToAdd);
    }

    public Long deleteByOrganization(Organization organization) {
        return userOrgPermissionsRepository.deleteByOrganization(organization);
    }
}
