package com.sequenceiq.cloudbreak.service.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.organization.UserOrgPermissions;
import com.sequenceiq.cloudbreak.repository.organization.UserOrgPermissionsRepository;

@Service
public class UserOrgPermissionsService {

    private static final String VALID_PERMISSIONS_MESSAGE = "Valid permissions are: " + Arrays.stream(OrganizationPermissions.values())
            .map(OrganizationPermissions::value)
            .collect(Collectors.joining(", "));

    @Inject
    private UserOrgPermissionsRepository userOrgPermissionsRepository;

    @Inject
    private UserOrgPermissionsValidator userOrgPermissionsValidator;

    public Set<UserOrgPermissions> findForCurrentUser(User currentUser) {
        return userOrgPermissionsRepository.findForUser(currentUser);
    }

    public UserOrgPermissions findForUserByOrganizationId(Long orgId, User currentUser) {
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

    public Set<UserOrgPermissions> deleteAll(Set<UserOrgPermissions> toBeDeleted) {
        userOrgPermissionsRepository.deleteAll(toBeDeleted);
        return toBeDeleted;
    }

    public Set<UserOrgPermissions> findForUserByOrganizationIds(User user, Set<Long> orgIds) {
        return userOrgPermissionsRepository.findForUserByOrganizationIds(user, orgIds);
    }

    public Long deleteByOrganization(Organization organization) {
        return userOrgPermissionsRepository.deleteByOrganization(organization);
    }

    public UserOrgPermissions save(UserOrgPermissions userOrgPermissions) {
        validateSaveAll(Collections.singleton(userOrgPermissions));
        return userOrgPermissionsRepository.save(userOrgPermissions);
    }

    public Iterable<UserOrgPermissions> saveAll(Set<UserOrgPermissions> userOrgPermsToAdd) {
        validateSaveAll(userOrgPermsToAdd);
        return userOrgPermissionsRepository.saveAll(userOrgPermsToAdd);
    }

    private void validateSaveAll(Set<UserOrgPermissions> userOrgPermsToAdd) {
        Set<ValidationResult> validationResults = userOrgPermsToAdd.stream()
                .map(userOrgPermissionsValidator::validate)
                .collect(Collectors.toSet());
        Optional<ValidationResult> mergedResults = validationResults.stream()
                .reduce(ValidationResult::merge);
        if (mergedResults.isPresent() && mergedResults.get().hasError()) {
            throw new IllegalArgumentException(buildErrorMessage(mergedResults.get()));
        }
    }

    private String buildErrorMessage(ValidationResult validationResult) {
        return new StringBuilder("Invalid permissions: ")
                .append(validationResult.getFormattedErrors()).append('\n')
                .append(VALID_PERMISSIONS_MESSAGE)
                .toString();
    }
}
