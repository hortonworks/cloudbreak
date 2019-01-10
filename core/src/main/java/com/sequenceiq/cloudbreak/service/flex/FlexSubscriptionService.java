package com.sequenceiq.cloudbreak.service.flex;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.repository.FlexSubscriptionRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class FlexSubscriptionService extends AbstractWorkspaceAwareResourceService<FlexSubscription> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlexSubscriptionService.class);

    @Inject
    private FlexSubscriptionRepository flexSubscriptionRepository;

    @Inject
    private StackService stackService;

    @Inject
    private TransactionService transactionService;

    @Override
    public FlexSubscription create(FlexSubscription subscription, @Nonnull Workspace workspace, User user) {
        prepareCreation(subscription, workspace);

        try {
            return transactionService.required(() -> {
                setWorkspace(subscription, user, workspace);
                FlexSubscription updated = flexSubscriptionRepository.save(subscription);
                Set<FlexSubscription> allInAccount = flexSubscriptionRepository.findAllByWorkspace(workspace);
                setSubscriptionAsDefaultIfNeeded(updated, allInAccount);
                updateSubscriptionsDefaultFlagsIfNeeded(updated, allInAccount);
                LOGGER.debug("Flex subscription has been created: {}", updated);
                return updated;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @Override
    public WorkspaceResourceRepository<FlexSubscription, Long> repository() {
        return flexSubscriptionRepository;
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.FLEXSUBSCRIPTION;
    }

    @Override
    protected void prepareDeletion(FlexSubscription resource) {
        if (stackService.countByFlexSubscription(resource) != 0L) {
            throw new BadRequestException("The given Flex subscription cannot be deleted, there are associated clusters");
        }
    }

    private void prepareCreation(FlexSubscription subscription, Workspace workspace) {
        if (!flexSubscriptionRepository.countByNameAndWorkspace(subscription.getName(), workspace).equals(0L)) {
            throw new BadRequestException(String.format("The name: '%s' has already taken by an other FlexSubscription.", subscription.getName()));
        } else if (!flexSubscriptionRepository.countBySubscriptionIdAndWorkspace(subscription.getSubscriptionId(), workspace).equals(0L)) {
            throw new BadRequestException(String.format("The subscriptionId: '%s' has already taken by an other FlexSubscription.",
                    subscription.getSubscriptionId()));
        }
    }

    @Override
    protected void prepareCreation(FlexSubscription resource) {

    }

    public FlexSubscription delete(Long id) {
        FlexSubscription subscription = get(id);
        return delete(subscription);
    }

    public FlexSubscription findOneByName(String name, User user, Workspace workspace) {
        return getFlexSubscription(name, workspace);
    }

    public FlexSubscription findOneByNameAndWorkspace(String name, Long workspaceId, User user) {
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        return getFlexSubscription(name, workspace);
    }

    private FlexSubscription getFlexSubscription(String name, Workspace workspace) {
        LOGGER.debug("Looking for Flex subscription name: {}", name);
        return flexSubscriptionRepository.findByNameAndWorkspace(name, workspace);
    }

    public Set<FlexSubscription> findAllForUserAndWorkspace(User user, Long workspaceId) {
        LOGGER.debug("Looking for public Flex subscriptions for user: {}", user.getUserId());
        return flexSubscriptionRepository.findAllByWorkspaceId(workspaceId);
    }

    public Optional<FlexSubscription> setDefaultFlexSubscription(String name, User user, Workspace workspace) {
        LOGGER.debug("Set Flex subscription '{}' as default in workspace '{}'", name, workspace.getName());
        return setFlexSubscriptionFlag(name, user, workspace, FlexSubscription::setDefault);
    }

    public Optional<FlexSubscription> setUsedForControllerFlexSubscription(String name, User user, Workspace workspace) {
        LOGGER.debug("Set Flex subscription '{}' as used for controller in workspace '{}'", name, workspace.getName());
        return setFlexSubscriptionFlag(name, user, workspace, FlexSubscription::setUsedForController);
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

    private Optional<FlexSubscription> setFlexSubscriptionFlag(String name, User user, Workspace workspace, BiConsumer<FlexSubscription, Boolean> setter) {
        Set<FlexSubscription> allInAccount = flexSubscriptionRepository.findAllByWorkspace(workspace);
        Optional<FlexSubscription> flexSubscription = setFlagOnFlexSubscriptionCollection(name, setter, allInAccount);
        flexSubscriptionRepository.saveAll(allInAccount);
        return flexSubscription;
    }

    private Optional<FlexSubscription> setFlagOnFlexSubscriptionCollection(String name, BiConsumer<FlexSubscription, Boolean> setter, Collection<FlexSubscription> allInAccount) {
        if (allInAccount.stream().noneMatch(f -> name.equals(f.getName()))) {
            throw new BadRequestException("Given subscription not found with name: " + name);
        }
        for (FlexSubscription flex : allInAccount) {
            setter.accept(flex, name.equals(flex.getName()));
        }
        return allInAccount.stream()
                .filter(flex -> name.equals(flex.getName()))
                .findFirst();
    }

    public FlexSubscription findFirstByUsedForController(boolean usedForController) {
        return flexSubscriptionRepository.findFirstByUsedForController(usedForController);
    }

    public FlexSubscription findFirstByIsDefault(boolean byDefault) {
        return flexSubscriptionRepository.findFirstByIsDefault(byDefault);
    }
}
