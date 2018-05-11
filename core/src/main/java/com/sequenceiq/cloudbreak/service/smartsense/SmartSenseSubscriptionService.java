package com.sequenceiq.cloudbreak.service.smartsense;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Iterator;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.SmartSenseConfigurationNotFoundException;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.repository.FlexSubscriptionRepository;
import com.sequenceiq.cloudbreak.repository.SmartSenseSubscriptionRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Service
public class SmartSenseSubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartSenseSubscriptionService.class);

    @Inject
    private SmartSenseSubscriptionRepository repository;

    @Inject
    private FlexSubscriptionRepository flexSubscriptionRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Value("${cb.smartsense.id:}")
    private String defaultSmartsenseId;

    @PostConstruct
    public void init() {
        if (!defaultSmartsenseId.isEmpty() && !Pattern.matches(SmartSenseSubscriptionJson.ID_PATTERN, defaultSmartsenseId)) {
            throw new IllegalArgumentException(SmartSenseSubscriptionJson.ID_FORMAT);
        }
    }

    public SmartSenseSubscription create(SmartSenseSubscription subscription) {
        long count = repository.count();
        if (count != 0L) {
            throw new BadRequestException("Only one SmartSense subscription is allowed by deployment.");
        }
        try {
            subscription = repository.save(subscription);
            LOGGER.info("SmartSense subscription has been created: {}", subscription);
            return subscription;
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], error: [%s]", APIResourceType.SMARTSENSE_SUBSCRIPTION, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public SmartSenseSubscription update(SmartSenseSubscription subscription) {
        return repository.save(subscription);
    }

    public void delete(SmartSenseSubscription subscription) {
        authorizationService.hasWritePermission(subscription);
        if (!flexSubscriptionRepository.countBySmartSenseSubscription(subscription).equals(0L)) {
            throw new BadRequestException("Subscription could not be deleted, because it is assigned to Flex subscription(s).");
        }
        repository.delete(subscription);
        LOGGER.info("SmartSense subscription has been deleted: {}", subscription);
    }

    public void delete(Long id) {
        SmartSenseSubscription subscription = repository.findOneById(id);
        delete(subscription);
    }

    public void delete(String subscriptionId, IdentityUser cbUser) {
        SmartSenseSubscription subscription = repository.findBySubscriptionIdAndAccount(subscriptionId, cbUser.getAccount());
        delete(subscription);
    }

    public SmartSenseSubscription findById(Long id) {
        LOGGER.info("Looking for SmartSense subscription with id: {}", id);
        return repository.findOne(id);
    }

    public SmartSenseSubscription findOneById(Long id) {
        LOGGER.info("Looking for one SmartSense subscription with id: {}", id);
        return repository.findOneById(id);
    }

    public Optional<SmartSenseSubscription> getDefault() {
        LOGGER.info("Get the SmartSense subscription");
        Iterator<SmartSenseSubscription> subscriptions = repository.findAll().iterator();
        return subscriptions.hasNext() ? Optional.of(subscriptions.next()) : Optional.empty();
    }

    public SmartSenseSubscription getDefaultForUser(@Nonnull IdentityUser cbUser) {
        Optional<SmartSenseSubscription> subscription = obtainSmartSenseSubscription(cbUser);
        checkSmartSenseSubscriptionAuthorization(subscription);
        return subscription.get();
    }

    private Optional<SmartSenseSubscription> obtainSmartSenseSubscription(IdentityUser cbUser) {
        Optional<SmartSenseSubscription> subscription = Optional.ofNullable(repository.findByAccountAndOwner(cbUser.getAccount(), cbUser.getUserId()));
        if (subscription.isPresent()) {
            upgradeDefaultSmartSenseSubscription(subscription.get());
        } else {
            LOGGER.info("Unable to find subscription for user.");
            subscription = Optional.ofNullable(createSubscriptionFromIdentityUser(cbUser));
        }
        return subscription;
    }

    private void upgradeDefaultSmartSenseSubscription(SmartSenseSubscription subscription) {
        if (!StringUtils.isEmpty(defaultSmartsenseId) && !defaultSmartsenseId.equals(subscription.getSubscriptionId())) {
            LOGGER.info("Upgrading default SmartSense subscription");
            subscription.setSubscriptionId(defaultSmartsenseId);
            repository.save(subscription);
        }
    }

    private void checkSmartSenseSubscriptionAuthorization(Optional<SmartSenseSubscription> subscription) {
        try {
            authorizationService.hasReadPermission(subscription.orElseThrow(() -> new SmartSenseConfigurationNotFoundException("Not Found")));
        } catch (AccessDeniedException | SmartSenseConfigurationNotFoundException issue) {
            String message = "Unable to identify SmartSense subscription for the user.";
            LOGGER.warn(message);
            throw new SmartSenseSubscriptionAccessDeniedException(message, issue);
        }
    }

    @Nullable
    private SmartSenseSubscription createSubscriptionFromIdentityUser(IdentityUser cbUser) {
        SmartSenseSubscription newSubscription = null;
        if (!StringUtils.isEmpty(defaultSmartsenseId)) {
            LOGGER.info("Generating default SmartSense subscription");
            newSubscription = new SmartSenseSubscription();
            newSubscription.setSubscriptionId(defaultSmartsenseId);
            newSubscription.setAccount(cbUser.getAccount());
            newSubscription.setOwner(cbUser.getUserId());
            newSubscription.setPublicInAccount(true);
            repository.save(newSubscription);
        }
        return newSubscription;
    }

}
