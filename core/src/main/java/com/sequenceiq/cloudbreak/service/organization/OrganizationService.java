package com.sequenceiq.cloudbreak.service.organization;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.validation.Permissions.ALL_READ;
import static com.sequenceiq.cloudbreak.validation.Permissions.ALL_WRITE;
import static com.sequenceiq.cloudbreak.validation.Permissions.ORG_MANAGE;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.domain.security.UserOrgPermissions;
import com.sequenceiq.cloudbreak.repository.security.OrganizationRepository;
import com.sequenceiq.cloudbreak.repository.security.UserOrgPermissionsRepository;
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

    public Organization getByName(String name, IdentityUser identityUser) {
        User user = userService.getOrCreate(identityUser);
        return organizationRepository.getByName(name, user.getTenant());
    }

    public Organization get(Long id) {
        return organizationRepository.findById(id).orElseThrow(notFound("Organization", id));
    }

    public void changeUsers(Long id, Map<String, Set<String>> userPermissions) {
        try {
            transactionService.required(() -> {
                Organization organization = get(id);
                Set<UserOrgPermissions> oldPermissions = userOrgPermissionsRepository.findForOrganization(organization);
                userOrgPermissionsRepository.deleteAll(oldPermissions);
                for (Map.Entry<String, Set<String>> userPermission : userPermissions.entrySet()) {
                    User user = userService.getByEmail(userPermission.getKey());
                    UserOrgPermissions newUserPermission = new UserOrgPermissions();
                    newUserPermission.setPermissionSet(userPermission.getValue());
                    newUserPermission.setUser(user);
                    newUserPermission.setOrganization(organization);
                    userOrgPermissionsRepository.save(newUserPermission);
                }
                return organization;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public void delete(Long id) {
        deleteImpl(get(id));
    }

    private void deleteImpl(Organization organization) {
    }
}
