package com.sequenceiq.cloudbreak.service;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;
import com.sequenceiq.cloudbreak.domain.organization.User;
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
    public T createInDefaultOrganization(T resource) {
        User user = userService.getCurrentUser();
        Organization organization = organizationService.getDefaultOrganizationForUser(user);
        return create(resource, organization);
    }

    @Override
    public T create(T resource, @Nonnull Long organizationId) {
        Organization organization = organizationService.get(organizationId);
        return create(resource, organization);
    }

    private T create(T resource, Organization organization) {
        try {
            prepareCreation(resource);
            User user = userService.getCurrentUser();
            return transactionService.required(() -> {
                setOrganization(resource, user, organization);
                return repository().save(resource);
            });
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof DataIntegrityViolationException) {
                String message = String.format("%s already exists with name '%s' in organization %s",
                        resource().getShortName(), resource.getName(), resource.getOrganization().getName());
                throw new BadRequestException(message, e);
            }
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @Override
    public T getByNameForOrganization(String name, Organization organization) {
        T object = repository().findByNameAndOrganization(name, organization);
        if (object == null) {
            throw new NotFoundException(String.format("No %s found with name '%s'", resource().getShortName(), name));
        }
        return object;
    }

    @Override
    public T getByNameForOrganizationId(String name, Long organizationId) {
        T object = repository().findByNameAndOrganizationId(name, organizationId);
        if (object == null) {
            throw new NotFoundException(String.format("No %s found with name '%s'", resource().getShortName(), name));
        }
        return object;
    }

    @Override
    public T getByNameFromUsersDefaultOrganization(String name) {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        return getByNameForOrganization(name, organization);
    }

    @Override
    public Set<T> listByOrganizationId(Long organizationId) {
        return repository().findAllByOrganizationId(organizationId);
    }

    @Override
    public Set<T> listByOrganization(Organization organization) {
        return repository().findAllByOrganization(organization);
    }

    @Override
    public Set<T> listForUsersDefaultOrganization() {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        return listByOrganization(organization);
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
        T toBeDeleted = getByNameForOrganizationId(name, organizationId);
        return delete(toBeDeleted);
    }

    @Override
    public T deleteByNameFromDefaultOrganization(String name) {
        T toBeDeleted = getByNameFromUsersDefaultOrganization(name);
        return delete(toBeDeleted);
    }

    private void setOrganization(T resource, User user, Organization organization) {
        Set<Organization> usersOrganizations = organizationService.retrieveForUser(user);
        if (!usersOrganizations.contains(organization)) {
            throw new NotFoundException("Organization not found for user.");
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

    protected abstract OrganizationResource resource();

    protected abstract boolean canDelete(T resource);

    protected abstract void prepareCreation(T resource);
}
