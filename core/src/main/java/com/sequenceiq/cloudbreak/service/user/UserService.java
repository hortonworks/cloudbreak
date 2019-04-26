package com.sequenceiq.cloudbreak.service.user;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserPreferences;
import com.sequenceiq.cloudbreak.repository.workspace.UserRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.tenant.TenantService;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private static final Map<CloudbreakUser, Semaphore> UNDER_OPERATION = new ConcurrentHashMap<>();

    @Inject
    private CachedUserService cachedUserService;

    @Inject
    private UserRepository userRepository;

    @Inject
    private TenantService tenantService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private UserPreferencesService userPreferencesService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Retryable(value = RetryException.class, maxAttempts = 5, backoff = @Backoff(delay = 500))
    public User getOrCreate(CloudbreakUser cloudbreakUser) {
        if (cloudbreakUser != null) {
            try {
                return getCached(cloudbreakUser);
            } catch (TransactionRuntimeExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new RetryException(e.getMessage(), e);
            }
        } else {
            throw new AccessDeniedException("cloudbreakUser is empty");
        }
    }

    private User getCached(CloudbreakUser cloudbreakUser) throws InterruptedException {
        Semaphore semaphore = UNDER_OPERATION.computeIfAbsent(cloudbreakUser, iu -> new Semaphore(1));
        semaphore.acquire();
        try {
            return cachedUserService.getUser(
                    cloudbreakUser,
                    this::findUserAndSetCrnIfExists,
                    this::createUser);
        } finally {
            semaphore.release();
            UNDER_OPERATION.remove(cloudbreakUser);
        }
    }

    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    public Set<User> getByUsersIds(Set<String> userIds) {
        return userRepository.findByUserIdIn(userIds);
    }

    public Set<User> getAll(CloudbreakUser cloudbreakUser) {
        User user = userRepository.findByUserId(cloudbreakUser.getUserId())
                .orElseThrow(NotFoundException.notFound("User", cloudbreakUser.getUserId()));
        return userRepository.findAllByTenant(user.getTenant());
    }

    public String evictCurrentUserDetailsForLoggedInUser() {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        cachedUserService.evictByIdentityUser(cloudbreakUser);
        return cloudbreakUser.getUsername();
    }

    private User findUserAndSetCrnIfExists(CloudbreakUser cloudbreakUser) {
        try {
            return transactionService.requiresNew(() -> {
                Optional<User> userByIdAndTenantName = userRepository.findByTenantNameAndUserId(cloudbreakUser.getTenant(), cloudbreakUser.getUserId());
                if (userByIdAndTenantName.isPresent()) {
                    User user = userByIdAndTenantName.get();
                    if (user.getUserCrn() == null && !StringUtils.isEmpty(cloudbreakUser.getUserCrn())) {
                        user.setUserCrn(cloudbreakUser.getUserCrn());
                        user = userRepository.save(user);
                    }
                    return user;
                }
                return null;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private User createUser(CloudbreakUser cloudbreakUser) {
        try {
            return transactionService.requiresNew(() -> {
                User user = new User();
                user.setUserId(cloudbreakUser.getUserId());
                user.setUserName(cloudbreakUser.getUsername());

                Tenant tenant = tenantService.findByName(cloudbreakUser.getTenant()).orElse(null);
                if (tenant == null) {
                    tenant = new Tenant();
                    tenant.setName(cloudbreakUser.getTenant());
                    tenant = tenantService.save(tenant);
                }
                user.setTenant(tenant);
                if (!StringUtils.isEmpty(cloudbreakUser.getUserCrn())) {
                    user.setUserCrn(cloudbreakUser.getUserCrn());
                }
                user = userRepository.save(user);

                UserPreferences userPreferences = new UserPreferences(null, user);
                userPreferences = userPreferencesService.save(userPreferences);
                user.setUserPreferences(userPreferences);
                user = userRepository.save(user);
                return user;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

}
