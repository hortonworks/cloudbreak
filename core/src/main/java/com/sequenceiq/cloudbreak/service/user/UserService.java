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

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.TenantRepository;
import com.sequenceiq.cloudbreak.repository.workspace.UserRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Service
public class UserService {

    private static final Map<IdentityUser, Semaphore> UNDER_OPERATION = new ConcurrentHashMap<>();

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

    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 500))
    public User getOrCreate(IdentityUser identityUser) {
        try {
            return getCached(identityUser, this::createUser);
        } catch (Exception e) {
            throw new RetryException(e.getMessage());
        }
    }

    private User getCached(IdentityUser identityUser, Function<IdentityUser, User> createUser) throws InterruptedException {
        Semaphore semaphore = UNDER_OPERATION.computeIfAbsent(identityUser, iu -> new Semaphore(1));
        semaphore.acquire();
        try {
            return cachedUserService.getUser(identityUser, userRepository::findByUserId, this::createUser);
        } finally {
            semaphore.release();
            UNDER_OPERATION.remove(identityUser);
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

    public Set<User> getAll(IdentityUser identityUser) {
        User user = userRepository.findByUserId(identityUser.getUsername());
        return userRepository.findAllByTenant(user.getTenant());
    }

    private User createUser(IdentityUser identityUser) {
        try {
            return transactionService.requiresNew(() -> {
                User user = new User();
                user.setUserId(identityUser.getUsername());

                Tenant tenant = tenantRepository.findByName("DEFAULT");
                user.setTenant(tenant);
                user.setTenantPermissionSet(Collections.emptySet());
                user = userRepository.save(user);

                //create workspace
                Workspace workspace = new Workspace();
                workspace.setTenant(tenant);
                workspace.setName(identityUser.getUsername());
                workspace.setStatus(ACTIVE);
                workspace.setDescription("Default workspace for the user.");
                workspaceService.create(user, workspace);
                return user;
            });
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof ConstraintViolationException) {
                try {
                    return transactionService.requiresNew(() -> userRepository.findByUserId(identityUser.getUsername()));
                } catch (TransactionExecutionException e2) {
                    throw new TransactionRuntimeExecutionException(e2);
                }
            }
            throw new TransactionRuntimeExecutionException(e);
        }
    }
}
