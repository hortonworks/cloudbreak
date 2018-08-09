package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.repository.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.organization.OrganizationResourceService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;

public abstract class AbstractOrganizationResourceService<T extends OrganizationResource> implements OrganizationResourceService<T> {

    @Override
    public T create(IdentityUser identityUser, T resource) {
        return create(identityUser, resource, null);
    }

    @Override
    public T create(IdentityUser identityUser, T resource, Long organizationId) {
        try {
            prepareCreation(resource);
            return transactionService().required(() -> {
                try {
                    setOrganization(resource, identityUser, organizationId);
                    return repository().save(resource);
                } catch (DataIntegrityViolationException ex) {
                    String msg = String.format("Error with resource [%s], %s", apiResourceType(), getProperSqlErrorMessage(ex));
                    throw new BadRequestException(msg);
                }
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @Override
    public T getByNameForOrganization(String name, Organization organization) {
        T object = repository().findByNameAndOrganization(name, organization);
        if (object == null) {
            throw new NotFoundException(String.format("No %s found with name '%s'", resourceName(), name));
        }
        return object;
    }

    @Override
    public T getByNameForOrganization(String name, Long organizationId) {
        Organization organization = organizationService().get(organizationId);
        return getByNameForOrganization(name, organization);
    }

    @Override
    public T getByNameFromUsersDefaultOrganization(String name) {
        Organization organization = organizationService().getDefaultOrganizationForCurrentUser();
        return getByNameForOrganization(name, organization);
    }

    @Override
    public Set<T> listByOrganization(Long organizationId) {
        Organization organization = organizationService().get(organizationId);
        return repository().findByOrganization(organization);
    }

    @Override
    public Set<T> listByOrganization(Organization organization) {
        return repository().findByOrganization(organization);
    }

    @Override
    public Set<T> listForUsersDefaultOrganization() {
        Organization organization = organizationService().getDefaultOrganizationForCurrentUser();
        return repository().findByOrganization(organization);
    }

    @Override
    public T delete(T resource) {
        if (canDelete(resource)) {
            repository().delete(resource);
        }
        return resource;
    }

    @Override
    public T deleteByNameFromOrganization(String name, Long organizationId) {
        T toBeDeleted = organizationId == null ? getByNameFromUsersDefaultOrganization(name) : getByNameForOrganization(name, organizationId);
        return delete(toBeDeleted);
    }

    @Override
    public T deleteByNameFromDefaultOrganization(String name) {
        return deleteByNameFromOrganization(name, null);
    }

    private void setOrganization(T resource, IdentityUser identityUser, Long organizationId) {
        User user = userService().getOrCreate(identityUser);
        Organization organization;
        if (organizationId != null) {
            Set<Organization> usersOrganizations = organizationService().retrieveForUser(user);
            organization = organizationService().get(organizationId);
            if (!usersOrganizations.contains(organization)) {
                throw new NotFoundException("Organization not found for user.");
            }

        } else {
            organization = organizationService().getDefaultOrganizationForUser(user);
        }
        resource.setOrganization(organization);
    }

    protected abstract OrganizationResourceRepository<T, Long> repository();

    protected abstract TransactionService transactionService();

    protected abstract OrganizationService organizationService();

    protected abstract UserService userService();

    protected abstract APIResourceType apiResourceType();

    protected abstract String resourceName();

    protected abstract boolean canDelete(T resource);

    protected abstract void prepareCreation(T resource);
}
