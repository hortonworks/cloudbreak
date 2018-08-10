package com.sequenceiq.cloudbreak.service;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.OrganizationAwareResource;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.repository.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.organization.OrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;

public abstract class AbstractOrganizationAwareResourceService<T extends OrganizationAwareResource> implements OrganizationAwareResourceService<T> {

    @Inject
    private TransactionService transactionService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private UserService userService;

    @Override
    public T create(T resource) {
        return create(resource, null);
    }

    @Override
    public T create(T resource, Long organizationId) {
        try {
            prepareCreation(resource);
            User user = userService.getCurrentUser();
            return transactionService.required(() -> {
                setOrganization(resource, user, organizationId);
                return repository().save(resource);
            });
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof DataIntegrityViolationException) {
                String message = String.format("%s already exists with name '%s' in organization %s",
                        resourceName(), resource.getName(), resource.getOrganization().getName());
                throw new BadRequestException(message, e);
            }
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
        Organization organization = organizationService.get(organizationId);
        return getByNameForOrganization(name, organization);
    }

    @Override
    public T getByNameFromUsersDefaultOrganization(String name) {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        return getByNameForOrganization(name, organization);
    }

    @Override
    public Set<T> listByOrganization(Long organizationId) {
        Organization organization = organizationService.get(organizationId);
        return repository().findByOrganization(organization);
    }

    @Override
    public Set<T> listByOrganization(Organization organization) {
        return repository().findByOrganization(organization);
    }

    @Override
    public Set<T> listForUsersDefaultOrganization() {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
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

    private void setOrganization(T resource, User user, Long organizationId) {
        Organization organization;
        if (organizationId != null) {
            Set<Organization> usersOrganizations = organizationService.retrieveForUser(user);
            organization = organizationService.get(organizationId);
            if (!usersOrganizations.contains(organization)) {
                throw new NotFoundException("Organization not found for user.");
            }
        } else {
            organization = organizationService.getDefaultOrganizationForUser(user);
        }
        resource.setOrganization(organization);
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public OrganizationService getOrganizationService() {
        return organizationService;
    }

    public UserService getUserService() {
        return userService;
    }

    protected abstract OrganizationResourceRepository<T, Long> repository();

    protected abstract APIResourceType apiResourceType();

    protected abstract String resourceName();

    protected abstract boolean canDelete(T resource);

    protected abstract void prepareCreation(T resource);
}
