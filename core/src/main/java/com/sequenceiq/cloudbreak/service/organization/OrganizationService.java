package com.sequenceiq.cloudbreak.service.organization;

import static com.sequenceiq.cloudbreak.api.model.v2.OrganizationStatus.DELETED;
import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;
import static com.sequenceiq.cloudbreak.validation.Permissions.ALL_READ;
import static com.sequenceiq.cloudbreak.validation.Permissions.ALL_WRITE;
import static com.sequenceiq.cloudbreak.validation.Permissions.ORG_MANAGE;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.users.ChangeOrganizationUsersJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.domain.security.UserOrgPermissions;
import com.sequenceiq.cloudbreak.repository.security.OrganizationRepository;
import com.sequenceiq.cloudbreak.repository.security.UserOrgPermissionsRepository;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Service
public class OrganizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationService.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private OrganizationRepository organizationRepository;

    @Inject
    private UserOrgPermissionsRepository userOrgPermissionsRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserService userService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private OrganizationDeleteVerifierService organizationDeleteVerifierService;

    public Organization create(IdentityUser identityUser, Organization organization) {
        User user = userService.getOrCreate(identityUser);
        try {
            organization = organizationRepository.save(organization);

            UserOrgPermissions userOrgPermissions = new UserOrgPermissions();
            userOrgPermissions.setOrganization(organization);
            userOrgPermissions.setUser(user);
            userOrgPermissions.setPermissionSet(Set.of(ALL_READ.value(), ALL_WRITE.value(), ORG_MANAGE.value()));
            userOrgPermissionsRepository.save(userOrgPermissions);

            return organization;

        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.ORGANIZATION, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public Set<Organization> retrieveForUser(IdentityUser identityUser) {
        User user = userService.getOrCreate(identityUser);
        return userOrgPermissionsRepository.findForUser(user).stream().map(UserOrgPermissions::getOrganization).collect(Collectors.toSet());
    }

    public Optional<Organization> getByName(String name, IdentityUser identityUser) {
        User user = userService.getOrCreate(identityUser);
        return getByName(name, user);
    }

    public Organization getDefaultOrganizationForUser(User user) {
        return organizationRepository.getByName(user.getUserId(), user.getTenant());
    }

    public Optional<Organization> getByName(String name, User user) {
        return Optional.ofNullable(organizationRepository.getByName(name, user.getTenant()));
    }

    public Optional<Organization> getByNameForCurrentUser(String name) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        User user = userService.getOrCreate(identityUser);
        return Optional.ofNullable(organizationRepository.getByName(name, user.getTenant()));
    }

    public Organization get(Long id) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        User user = userService.getOrCreate(identityUser);
        Organization organization = organizationRepository.findById(id).orElseThrow(notFound("Organization", id));
        if (!user.getTenant().equals(organization.getTenant())) {
            throw new NotFoundException("Organization not found.");
        }
        // TODO: check permissions (does the user have the right to do this in this org?)
        return organization;
    }

    public Set<User> removeUsers(String orgName, Set<String> userIds) {
        try {
            return transactionService.required(() -> {
                Organization organization = getOrganizationOrThrowNotFound(orgName);
                Set<User> users = userService.getByUsersIds(userIds);
                Set<UserOrgPermissions> toBeDeleted = validateAllUsersAreInTheOrganization(organization, users);

                // TODO: check permissions (does the user have the right to do this in this org?)
                userOrgPermissionsRepository.deleteAll(toBeDeleted);

                return toBeDeleted.stream()
                        .map(UserOrgPermissions::getUser)
                        .collect(Collectors.toSet());
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<User> addUsers(String orgName, Set<ChangeOrganizationUsersJson> changeOrganizationUsersJsons) {
        try {
            return transactionService.required(() -> {
                Organization organization = getOrganizationOrThrowNotFound(orgName);
                Map<User, Set<String>> usersToAddWithPermissions = orgUserPermissionJsonSetToMap(changeOrganizationUsersJsons);
                validateNoUserIsInTheOrganization(organization, usersToAddWithPermissions.keySet());

                Set<UserOrgPermissions> userOrgPermsToAdd = usersToAddWithPermissions.entrySet().stream()
                        .map(userWithPermissions -> {
                            UserOrgPermissions newUserPermission = new UserOrgPermissions();
                            newUserPermission.setPermissionSet(userWithPermissions.getValue());
                            newUserPermission.setUser(userWithPermissions.getKey());
                            newUserPermission.setOrganization(organization);
                            return newUserPermission;
                        })
                        .collect(Collectors.toSet());

                // TODO: check permissions (does the user have the right to do this in this org?)
                userOrgPermissionsRepository.saveAll(userOrgPermsToAdd);
                return userOrgPermsToAdd.stream().map(UserOrgPermissions::getUser)
                        .collect(Collectors.toSet());
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<User> updateUsers(String orgName, Set<ChangeOrganizationUsersJson> updateOrganizationUsersJsons) {
        try {
            return transactionService.required(() -> {
                Organization organization = getOrganizationOrThrowNotFound(orgName);
                Map<User, Set<String>> usersToUpdateWithPermissions = orgUserPermissionJsonSetToMap(updateOrganizationUsersJsons);
                Map<User, UserOrgPermissions> toBeUpdated = validateAllUsersAreInTheOrganization(organization, usersToUpdateWithPermissions.keySet())
                        .stream()
                        .collect(Collectors.toMap(UserOrgPermissions::getUser, uop -> uop));

                Set<UserOrgPermissions> userOrgPermissions = toBeUpdated.entrySet().stream()
                        .map(userPermission -> {
                            userPermission.getValue().setPermissionSet(usersToUpdateWithPermissions.get(userPermission.getKey()));
                            return userPermission.getValue();
                        })
                        .collect(Collectors.toSet());

                // TODO: check permissions (does the user have the right to do this in this org?)
                userOrgPermissionsRepository.saveAll(userOrgPermissions);
                return toBeUpdated.keySet();
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Organization getOrganizationOrThrowNotFound(String orgName) {
        Optional<Organization> organization = getByNameForCurrentUser(orgName);
        return organization.orElseThrow(() -> new NotFoundException("Cannot find organization with name: " + orgName));
    }

    private Map<User, Set<String>> orgUserPermissionJsonSetToMap(Set<ChangeOrganizationUsersJson> updateOrganizationUsersJsons) {
        return updateOrganizationUsersJsons.stream()
                .collect(Collectors.toMap(json -> userService.getByUserId(json.getUserId()), json -> json.getPermissions()));
    }

    private void validateAllUsersAreInTheTenant(Organization organization, Set<User> users) {
        boolean anyUserNotInTenantOfOrg = users.stream()
                .anyMatch(user -> !user.getTenant().equals(organization.getTenant()));

        if (anyUserNotInTenantOfOrg) {
            throw new NotFoundException("User not found in tenant.");
        }
    }

    private Set<UserOrgPermissions> validateAllUsersAreInTheOrganization(Organization organization, Set<User> users) {
        validateAllUsersAreInTheTenant(organization, users);
        Set<String> usersNotInTheOrganization = new TreeSet<>();

        Set<UserOrgPermissions> userOrgPermissionsSet = users.stream()
                .map(user -> {
                    UserOrgPermissions userOrgPermissions = userOrgPermissionsRepository.findForUserAndOrganization(user, organization);
                    if (userOrgPermissions == null) {
                        usersNotInTheOrganization.add(user.getUserId());
                    }
                    return userOrgPermissions;
                })
                .collect(Collectors.toSet());

        if (!usersNotInTheOrganization.isEmpty()) {
            String usersCommaSeparated = usersNotInTheOrganization.stream().collect(Collectors.joining(", "));
            throw new BadRequestException("The following users are not in the organization: " + usersCommaSeparated);
        }

        return userOrgPermissionsSet;
    }

    private void validateNoUserIsInTheOrganization(Organization organization, Set<User> users) {
        validateAllUsersAreInTheTenant(organization, users);

        Set<String> usersInOrganization = users.stream()
                .filter(user -> userOrgPermissionsRepository.findForUserAndOrganization(user, organization) != null)
                .map(User::getUserId)
                .collect(Collectors.toSet());

        if (!usersInOrganization.isEmpty()) {
            String usersCommaSeparated = usersInOrganization.stream().collect(Collectors.joining(", "));
            throw new BadRequestException("The following users are already in the organization: " + usersCommaSeparated);
        }
    }

    public Set<User> changeUsers(String orgName, Map<String, Set<String>> userPermissions) {
        try {
            return transactionService.required(() -> {
                Organization organization = getOrganizationOrThrowNotFound(orgName);
                Set<UserOrgPermissions> oldPermissions = userOrgPermissionsRepository.findForOrganization(organization);
                userOrgPermissionsRepository.deleteAll(oldPermissions);

                Map<String, User> usersToAdd = userService.getByUsersIds(userPermissions.keySet()).stream()
                        .collect(Collectors.toMap(User::getUserId, user -> user));

                userPermissions.entrySet().stream()
                        .map(userPermission -> {
                            User user = usersToAdd.get(userPermission.getKey());
                            UserOrgPermissions newUserPermission = new UserOrgPermissions();
                            newUserPermission.setPermissionSet(userPermission.getValue());
                            newUserPermission.setUser(user);
                            newUserPermission.setOrganization(organization);
                            return newUserPermission;
                        })
                        // TODO: check permissions (does the user have the right to do this in this org?)
                        .forEach(userOrgPermissionsRepository::save);

                return new HashSet<>(usersToAdd.values());
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Organization deleteByName(String orgName, IdentityUser identityUser) {
        // TODO: check permissions (does the user have the right to delete this org?)
        try {
            return transactionService.required(() -> {
                Organization organizationForDelete = getOrganizationOrThrowNotFound(orgName);
                User userWhoRequestTheDeletion = userService.getOrCreate(identityUser);
                Organization defaultOrganizationOfUserWhoRequestTheDeletion = getDefaultOrganizationForUser(userWhoRequestTheDeletion);
                organizationDeleteVerifierService.checkThatOrganizationIsDeletable(userWhoRequestTheDeletion, organizationForDelete,
                        defaultOrganizationOfUserWhoRequestTheDeletion);
                Long deleted = userOrgPermissionsRepository.deleteByOrganization(organizationForDelete);
                setupDeletionDateAndFlag(organizationForDelete);
                organizationRepository.save(organizationForDelete);
                LOGGER.info("Deleted organisation: {}, related permissions: {}", orgName, deleted);
                return organizationForDelete;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public void setupDeletionDateAndFlag(Organization organizationForDelete) {
        organizationForDelete.setStatus(DELETED);
        organizationForDelete.setDeletionTimestamp(Calendar.getInstance().getTimeInMillis());
    }
}
