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

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.security.CrnUser;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalUserModifier;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.tenant.TenantService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceStatus;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.UserRepository;

@Service
public class UserService extends InternalUserModifier {

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
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

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

    public Optional<User> getByUserIdAndTenant(String userId, String tenant) {
        return userRepository.findByTenantNameAndUserId(tenant, userId);
    }

    public Set<User> getByUsersIds(Set<String> userIds) {
        return userRepository.findByUserIdIn(userIds);
    }

    public Set<User> getAll(CloudbreakUser cloudbreakUser) {
        Optional<Tenant> tenant = tenantService.findByName(cloudbreakUser.getTenant());
        if (tenant.isPresent()) {
            return userRepository.findAllByTenant(tenant.get());
        }
        return Sets.newHashSet();
    }

    public String evictCurrentUserDetailsForLoggedInUser() {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        cachedUserService.evictByIdentityUser(cloudbreakUser);
        return cloudbreakUser.getUsername();
    }

    @Override
    public void persistModifiedInternalUser(CrnUser newUser) {
        getOrCreate(newUser);
        restRequestThreadLocalService.setCloudbreakUser(newUser);
    }

    private User findUserAndSetCrnIfExists(CloudbreakUser cloudbreakUser) {
        try {
            return transactionService.requiresNew(() -> {
                Optional<User> userByIdAndTenantName = userRepository.findByTenantNameAndUserId(cloudbreakUser.getTenant(), cloudbreakUser.getUserId());
                if (userByIdAndTenantName.isPresent()) {
                    User user = userByIdAndTenantName.get();
                    if (user.getUserCrn() == null) {
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
                user.setUserCrn(cloudbreakUser.getUserCrn());

                Tenant tenant = tenantService.findByName(cloudbreakUser.getTenant()).orElse(null);
                if (tenant == null) {
                    tenant = new Tenant();
                    tenant.setName(cloudbreakUser.getTenant());
                    tenant = tenantService.save(tenant);
                    createTenantDefaultWorkspace(tenant);
                }
                user.setTenant(tenant);

                return userRepository.save(user);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private void createTenantDefaultWorkspace(Tenant tenant) {
        Workspace workspace = new Workspace();
        workspace.setTenant(tenant);
        workspace.setName(tenant.getName());
        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspaceService.create(workspace);
    }

}
