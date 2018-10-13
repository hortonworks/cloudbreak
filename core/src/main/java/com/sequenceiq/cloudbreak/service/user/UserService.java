package com.sequenceiq.cloudbreak.service.user;

import static com.sequenceiq.cloudbreak.api.model.v2.WorkspaceStatus.ACTIVE;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import javax.inject.Inject;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.retry.RetryException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserPreferences;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.TenantRepository;
import com.sequenceiq.cloudbreak.repository.workspace.UserPreferencesRepository;
import com.sequenceiq.cloudbreak.repository.workspace.UserRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Service
public class UserService {

    private static final Map<CloudbreakUser, Semaphore> UNDER_OPERATION = new ConcurrentHashMap<>();

    @Inject
    private CachedUserService cachedUserService;

    @Inject
    private UserRepository userRepository;

    @Inject
    private TenantRepository tenantRepository;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private UserPreferencesRepository userPreferencesRepository;

    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 500))
    public User getOrCreate(CloudbreakUser cloudbreakUser) {
        try {
            return getCached(cloudbreakUser, this::createUser);
        } catch (Exception e) {
            throw new RetryException(e.getMessage());
        }
    }

    private User getCached(CloudbreakUser cloudbreakUser, Function<CloudbreakUser, User> createUser) throws InterruptedException {
        Semaphore semaphore = UNDER_OPERATION.computeIfAbsent(cloudbreakUser, iu -> new Semaphore(1));
        semaphore.acquire();
        try {
            return cachedUserService.getUser(cloudbreakUser, userRepository::findByTenantNameAndUserName, this::createUser);
        } finally {
            semaphore.release();
            UNDER_OPERATION.remove(cloudbreakUser);
        }
    }

    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    public User getByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    public Set<User> getByUsersIds(Set<String> userIds) {
        return userRepository.findByUserIdIn(userIds);
    }

    public Set<User> getAll(CloudbreakUser cloudbreakUser) {
        User user = userRepository.findByUserId(cloudbreakUser.getUsername());
        return userRepository.findAllByTenant(user.getTenant());
    }

    private User createUser(CloudbreakUser cloudbreakUser) {
        try {
            return transactionService.requiresNew(() -> {
                User user = new User();
                user.setUserId(cloudbreakUser.getUsername());
                user.setUserName(cloudbreakUser.getUsername());

                Tenant tenant = tenantRepository.findByName(cloudbreakUser.getTenant());
                if (tenant == null) {
                    tenant = new Tenant();
                    tenant.setName(cloudbreakUser.getTenant());
                    tenant = tenantRepository.save(tenant);
                }
                user.setTenant(tenant);
                user.setTenantPermissionSet(Collections.emptySet());
                user = userRepository.save(user);

                //create workspace
                Workspace workspace = new Workspace();
                workspace.setTenant(tenant);
                workspace.setName(cloudbreakUser.getUsername());
                workspace.setStatus(ACTIVE);
                workspace.setDescription("Default workspace for the user.");
                workspaceService.create(user, workspace);

                UserPreferences userPreferences = new UserPreferences(null, user);
                userPreferences = userPreferencesRepository.save(userPreferences);
                user.setUserPreferences(userPreferences);
                user = userRepository.save(user);
                return user;
            });
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof ConstraintViolationException) {
                try {
                    return transactionService.requiresNew(() -> userRepository.findByUserId(cloudbreakUser.getUsername()));
                } catch (TransactionExecutionException e2) {
                    throw new TransactionRuntimeExecutionException(e2);
                }
            }
            throw new TransactionRuntimeExecutionException(e);
        }
    }
}
