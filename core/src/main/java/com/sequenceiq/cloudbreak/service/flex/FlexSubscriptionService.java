package com.sequenceiq.cloudbreak.service.flex;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.repository.FlexSubscriptionRepository;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class FlexSubscriptionService extends AbstractOrganizationAwareResourceService<FlexSubscription> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlexSubscriptionService.class);

    @Inject
    private FlexSubscriptionRepository flexSubscriptionRepository;

    @Inject
    private StackService stackService;

    @Inject
    private TransactionService transactionService;

    @Override
    public FlexSubscription create(FlexSubscription subscription, @Nonnull Organization organization) {
        prepareCreation(subscription, organization);

        try {
            User user = getUserService().getCurrentUser();
            return transactionService.required(() -> {
                setOrganization(subscription, user, organization);
                FlexSubscription updated = flexSubscriptionRepository.save(subscription);
                Set<FlexSubscription> allInAccount = flexSubscriptionRepository.findAllByOrganization(organization);
                setSubscriptionAsDefaultIfNeeded(updated, allInAccount);
                updateSubscriptionsDefaultFlagsIfNeeded(updated, allInAccount);
                LOGGER.info("Flex subscription has been created: {}", updated);
                return updated;
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    @Override
    protected OrganizationResourceRepository<FlexSubscription, Long> repository() {
        return flexSubscriptionRepository;
    }

    @Override
    protected OrganizationResource resource() {
        return OrganizationResource.FLEXSUBSCRIPTION;
    }

    @Override
    protected void prepareDeletion(FlexSubscription resource) {
        if (stackService.countByFlexSubscription(resource) != 0L) {
            throw new BadRequestException("The given Flex subscription cannot be deleted, there are associated clusters");
        }
    }

    private void prepareCreation(FlexSubscription subscription, Organization organization) {
        if (!flexSubscriptionRepository.countByNameAndOrganization(subscription.getName(), organization).equals(0L)) {
            throw new BadRequestException(String.format("The name: '%s' has already taken by an other FlexSubscription.", subscription.getName()));
        } else if (!flexSubscriptionRepository.countBySubscriptionIdAndOrganization(subscription.getSubscriptionId(), organization).equals(0L)) {
            throw new BadRequestException(String.format("The subscriptionId: '%s' has already taken by an other FlexSubscription.",
                    subscription.getSubscriptionId()));
        }
    }

    @Override
    protected void prepareCreation(FlexSubscription resource) {

    }

    public void delete(Long id) {
        FlexSubscription subscription = get(id);
        delete(subscription);
    }

    public FlexSubscription findOneByName(String name, IdentityUser user) {
        Organization organization = getOrganizationService().getDefaultOrganizationForUser(user);
        return getFlexSubscription(name, organization);
    }

    public FlexSubscription findOneByNameAndOrganization(String name, Long organizationId) {
        Organization organization = getOrganizationService().get(organizationId);
        return getFlexSubscription(name, organization);
    }

    private FlexSubscription getFlexSubscription(String name, Organization organization) {
        LOGGER.info("Looking for Flex subscription name: {}", name);
        return flexSubscriptionRepository.findByNameAndOrganization(name, organization);
    }

    public Set<FlexSubscription> findAllForUser(IdentityUser user) {
        Organization organization = getOrganizationService().getDefaultOrganizationForUser(user);
        return findAllForUserAndOrganization(user, organization.getId());
    }

    public Set<FlexSubscription> findAllForUserAndOrganization(IdentityUser user, Long organizationId) {
        LOGGER.info("Looking for public Flex subscriptions for user: {}", user.getUsername());
        return flexSubscriptionRepository.findAllByOrganizationId(organizationId);
    }

    public void setDefaultFlexSubscription(String name, IdentityUser identityUser) {
        setFlexSubscriptionFlag(name, identityUser, FlexSubscription::setDefault);
    }

    public void setUsedForControllerFlexSubscription(String name, IdentityUser identityUser) {
        setFlexSubscriptionFlag(name, identityUser, FlexSubscription::setUsedForController);
    }

    public FlexSubscription get(Long id) {
        return flexSubscriptionRepository.findById(id).orElseThrow(notFound("Flex Subscription", id));
    }

    private void setSubscriptionAsDefaultIfNeeded(FlexSubscription subscription, Collection<FlexSubscription> allInAccount) {
        if (allInAccount.stream().allMatch(subscription::equals)) {
            subscription.setDefault(true);
            subscription.setUsedForController(true);
        }
    }

    private void updateSubscriptionsDefaultFlagsIfNeeded(FlexSubscription subscription, Collection<FlexSubscription> allInAccount) {
        if (subscription.isDefault() || subscription.isUsedForController()) {
            if (subscription.isDefault()) {
                setFlagOnFlexSubscriptionCollection(subscription.getName(), FlexSubscription::setDefault, allInAccount);
            }

            if (subscription.isUsedForController()) {
                setFlagOnFlexSubscriptionCollection(subscription.getName(), FlexSubscription::setUsedForController, allInAccount);
            }
            flexSubscriptionRepository.saveAll(allInAccount);
        }
    }

    private void setFlexSubscriptionFlag(String name, IdentityUser identityUser, BiConsumer<FlexSubscription, Boolean> setter) {
        Organization organization = getOrganizationService().getDefaultOrganizationForUser(identityUser);
        Set<FlexSubscription> allInAccount = flexSubscriptionRepository.findAllByOrganization(organization);
        setFlagOnFlexSubscriptionCollection(name, setter, allInAccount);
        flexSubscriptionRepository.saveAll(allInAccount);
    }

    private void setFlagOnFlexSubscriptionCollection(String name, BiConsumer<FlexSubscription, Boolean> setter, Collection<FlexSubscription> allInAccount) {
        if (allInAccount.stream().noneMatch(f -> name.equals(f.getName()))) {
            throw new BadRequestException("Given subscription not found with name: " + name);
        }
        for (FlexSubscription flex : allInAccount) {
            setter.accept(flex, name.equals(flex.getName()));
        }
    }

    public FlexSubscription findFirstByUsedForController(boolean usedForController) {
        return flexSubscriptionRepository.findFirstByUsedForController(usedForController);
    }

    public FlexSubscription findFirstByIsDefault(boolean byDefault) {
        return flexSubscriptionRepository.findFirstByIsDefault(byDefault);
    }
}
