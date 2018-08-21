package com.sequenceiq.cloudbreak.service;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.organization.OrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

public abstract class AbstractOrganizationAwareResourceService<T extends OrganizationAwareResource> implements OrganizationAwareResourceService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrganizationAwareResourceService.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private OrganizationService organizationService;

    @Override
    public T create(T resource, @Nonnull Long organizationId, User user) {
        Organization organization = organizationService.get(organizationId, user);
        return create(resource, organization, user);
    }

    @Override
    public T create(T resource, Organization organization, User user) {
        try {
            prepareCreation(resource);
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
    public Set<T> findAllByOrganization(Organization organization) {
        return repository().findAllByOrganization(organization);
    }

    @Override
    public Set<T> findAllByOrganizationId(Long organizationId) {
        return repository().findAllByOrganizationId(organizationId);
    }

    @Override
    public T delete(T resource) {
        LOGGER.info("Deleting resource with name: {}", resource.getName());
        prepareDeletion(resource);
        repository().delete(resource);
        return resource;
    }

    @Override
    public T deleteByNameFromOrganization(String name, Long organizationId) {
        T toBeDeleted = getByNameForOrganizationId(name, organizationId);
        return delete(toBeDeleted);
    }

    protected void setOrganization(T resource, User user, Organization organization) {
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

    protected abstract OrganizationResourceRepository<T, Long> repository();

    protected abstract OrganizationResource resource();

    protected abstract void prepareDeletion(T resource);

    protected abstract void prepareCreation(T resource);
}
