package com.sequenceiq.cloudbreak.service.organization;

import static com.sequenceiq.cloudbreak.api.model.v2.OrganizationStatus.DELETED;
import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.ALL_READ;
import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.ALL_WRITE;
import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.ORG_MANAGE;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.users.ChangeOrganizationUsersJson;
import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions;
import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.organization.UserOrgPermissions;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.user.UserOrgPermissionsService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Service
public class OrganizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationService.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private OrganizationRepository organizationRepository;

    @Inject
    private UserOrgPermissionsService userOrgPermissionsService;

    @Inject
    private UserService userService;

    @Inject
    private OrganizationModificationVerifierService organizationModificationVerifierService;

    public Organization create(User user, Organization organization) {
        try {
            return transactionService.required(() -> {
                Organization createdOrg = organizationRepository.save(organization);
                UserOrgPermissions userOrgPermissions = new UserOrgPermissions();
                userOrgPermissions.setOrganization(createdOrg);
                userOrgPermissions.setUser(user);
                userOrgPermissions.setPermissionSet(Set.of(ALL_READ.value(), ALL_WRITE.value(), ORG_MANAGE.value()));
                userOrgPermissionsService.save(userOrgPermissions);
                return createdOrg;
            });
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof DataIntegrityViolationException) {
                throw new BadRequestException(String.format("Organization with name '%s' in your tenant already exists.",
                        organization.getName()), e.getCause());
            }
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<Organization> retrieveForUser(User user) {
        return userOrgPermissionsService.findForUser(user).stream()
                .map(UserOrgPermissions::getOrganization).collect(Collectors.toSet());
    }

    public Organization getDefaultOrganizationForUser(User user) {
        return organizationRepository.getByName(user.getUserId(), user.getTenant());
    }

    public Optional<Organization> getByName(String name, User user) {
        return Optional.ofNullable(organizationRepository.getByName(name, user.getTenant()));
    }

    public Optional<Organization> getByNameForUser(String name, User user) {
        return Optional.ofNullable(organizationRepository.getByName(name, user.getTenant()));
    }

    public Organization getById(Long id) {
        Optional<Organization> organization = organizationRepository.findById(id);
        if (organization.isPresent()) {
            return organization.get();
        }
        throw new IllegalArgumentException(String.format("No Organization found with id: %s", id));
    }

    public Organization get(Long id, User user) {
        UserOrgPermissions userOrgPermissions = userOrgPermissionsService.findForUserByOrganizationId(user, id);
        if (userOrgPermissions == null) {
            throw new NotFoundException("Cannot find organization by user.");
        }
        return userOrgPermissions.getOrganization();
    }

    public Set<User> removeUsers(String orgName, Set<String> userIds, User user) {
        try {
            return transactionService.required(() -> {
                Organization organization = getByNameForUserOrThrowNotFound(orgName, user);
                authorizeOrgManipulation(user, organization, Action.MANAGE);

                Set<User> users = userService.getByUsersIds(userIds);
                Set<UserOrgPermissions> toBeRemoved = validateAllUsersAreAlreadyInTheOrganization(organization, users);
                Set<User> usersToBeRemoved = toBeRemoved.stream().map(UserOrgPermissions::getUser).collect(Collectors.toSet());
                organizationModificationVerifierService.verifyRemovalFromDefaultOrg(user, organization, usersToBeRemoved);

                userOrgPermissionsService.deleteAll(toBeRemoved);
                return toBeRemoved.stream()
                        .map(UserOrgPermissions::getUser)
                        .collect(Collectors.toSet());
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<User> addUsers(String orgName, Set<ChangeOrganizationUsersJson> changeOrganizationUsersJsons, User currentUser) {
        try {
            return transactionService.required(() -> {
                Organization organization = getByNameForUserOrThrowNotFound(orgName, currentUser);
                authorizeOrgManipulation(currentUser, organization, Action.INVITE);

                Map<User, Set<String>> usersToAddWithPermissions = orgUserPermissionJsonSetToMap(changeOrganizationUsersJsons);
                validateUsersAreNotInTheOrganizationYet(organization, usersToAddWithPermissions.keySet());

                Set<UserOrgPermissions> userOrgPermsToAdd = usersToAddWithPermissions.entrySet().stream()
                        .map(userWithPermissions -> {
                            UserOrgPermissions newUserPermission = new UserOrgPermissions();
                            newUserPermission.setPermissionSet(userWithPermissions.getValue());
                            newUserPermission.setUser(userWithPermissions.getKey());
                            newUserPermission.setOrganization(organization);
                            return newUserPermission;
                        })
                        .collect(Collectors.toSet());

                userOrgPermissionsService.saveAll(userOrgPermsToAdd);
                return userOrgPermsToAdd.stream().map(UserOrgPermissions::getUser)
                        .collect(Collectors.toSet());
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<User> updateUsers(String orgName, Set<ChangeOrganizationUsersJson> updateOrganizationUsersJsons, User currentUser) {
        try {
            return transactionService.required(() -> {
                Organization organization = getByNameForUserOrThrowNotFound(orgName, currentUser);
                authorizeOrgManipulation(currentUser, organization, Action.MANAGE);

                Map<User, Set<String>> usersToUpdateWithPermissions = orgUserPermissionJsonSetToMap(updateOrganizationUsersJsons);
                Map<User, UserOrgPermissions> toBeUpdated = validateAllUsersAreAlreadyInTheOrganization(
                        organization, usersToUpdateWithPermissions.keySet()).stream()
                        .collect(Collectors.toMap(UserOrgPermissions::getUser, uop -> uop));

                Set<UserOrgPermissions> userOrgPermissions = toBeUpdated.entrySet().stream()
                        .map(userPermission -> {
                            userPermission.getValue().setPermissionSet(usersToUpdateWithPermissions.get(userPermission.getKey()));
                            return userPermission.getValue();
                        })
                        .collect(Collectors.toSet());

                Set<User> usersToBeUpdated = userOrgPermissions.stream().map(UserOrgPermissions::getUser).collect(Collectors.toSet());
                organizationModificationVerifierService.verifyUserUpdates(currentUser, organization, usersToBeUpdated);

                userOrgPermissionsService.saveAll(userOrgPermissions);
                return toBeUpdated.keySet();
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Organization getByNameForUserOrThrowNotFound(String orgName, User currentUser) {
        Optional<Organization> organization = getByNameForUser(orgName, currentUser);
        return organization.orElseThrow(() -> new NotFoundException("Cannot find organization with name: " + orgName));
    }

    public Set<User> changeUsers(String orgName, Map<String, Set<String>> userPermissions, User currentUser) {
        try {
            return transactionService.required(() -> {
                Organization organization = getByNameForUserOrThrowNotFound(orgName, currentUser);
                authorizeOrgManipulation(currentUser, organization, Action.MANAGE);

                Set<UserOrgPermissions> oldPermissions = userOrgPermissionsService.findForOrganization(organization);
                Set<User> oldUsers = oldPermissions.stream().map(UserOrgPermissions::getUser).collect(Collectors.toSet());

                organizationModificationVerifierService.verifyUserUpdates(currentUser, organization, oldUsers);
                userOrgPermissionsService.deleteAll(oldPermissions);

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
                        .forEach(userOrgPermissionsService::save);

                return new HashSet<>(usersToAdd.values());
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Organization deleteByNameForUser(String orgName, User currentUser, Organization defaultOrg) {
        try {
            return transactionService.required(() -> {
                Organization organizationForDelete = getByNameForUserOrThrowNotFound(orgName, currentUser);
                authorizeOrgManipulation(currentUser, organizationForDelete, Action.MANAGE);

                organizationModificationVerifierService.checkThatOrganizationIsDeletable(currentUser, organizationForDelete, defaultOrg);
                Long deleted = userOrgPermissionsService.deleteByOrganization(organizationForDelete);
                setupDeletionDateAndFlag(organizationForDelete);
                organizationRepository.save(organizationForDelete);
                LOGGER.info("Deleted organisation: {}, related permissions: {}", orgName, deleted);
                return organizationForDelete;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private Map<User, Set<String>> orgUserPermissionJsonSetToMap(Set<ChangeOrganizationUsersJson> updateOrganizationUsersJsons) {
        return updateOrganizationUsersJsons.stream()
                .collect(Collectors.toMap(json -> userService.getByUserId(json.getUserId()), json -> json.getPermissions()));
    }

    private Set<UserOrgPermissions> validateAllUsersAreAlreadyInTheOrganization(Organization organization, Set<User> users) {
        validateAllUsersAreInTheTenant(organization, users);
        Set<String> usersNotInTheOrganization = new TreeSet<>();

        Set<UserOrgPermissions> userOrgPermissionsSet = users.stream()
                .map(user -> {
                    UserOrgPermissions userOrgPermissions = userOrgPermissionsService.findForUserAndOrganization(user, organization);
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

    private void validateUsersAreNotInTheOrganizationYet(Organization organization, Set<User> users) {
        validateAllUsersAreInTheTenant(organization, users);

        Set<String> usersInOrganization = users.stream()
                .filter(user -> userOrgPermissionsService.findForUserAndOrganization(user, organization) != null)
                .map(User::getUserId)
                .collect(Collectors.toSet());

        if (!usersInOrganization.isEmpty()) {
            String usersCommaSeparated = usersInOrganization.stream().collect(Collectors.joining(", "));
            throw new BadRequestException("The following users are already in the organization: " + usersCommaSeparated);
        }
    }

    private void validateAllUsersAreInTheTenant(Organization organization, Set<User> users) {
        boolean anyUserNotInTenantOfOrg = users.stream()
                .anyMatch(user -> !user.getTenant().equals(organization.getTenant()));

        if (anyUserNotInTenantOfOrg) {
            throw new NotFoundException("User not found in tenant.");
        }
    }

    private void authorizeOrgManipulation(User currentUser, Organization organizationToManipulate, Action action) {
        UserOrgPermissions userOrgPermissions = userOrgPermissionsService.findForUserAndOrganization(currentUser, organizationToManipulate);
        if (userOrgPermissions == null) {
            throw new AccessDeniedException("You have no access for this resource.");
        }
        boolean hasPermission = OrganizationPermissions.hasPermission(userOrgPermissions.getPermissionSet(), OrganizationResource.ORG, action);
        if (!hasPermission) {
            throw new AccessDeniedException("You cannot delete this organization.");
        }
    }

    private void setupDeletionDateAndFlag(Organization organizationForDelete) {
        organizationForDelete.setStatus(DELETED);
        organizationForDelete.setDeletionTimestamp(Calendar.getInstance().getTimeInMillis());
    }
}
