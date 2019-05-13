package com.sequenceiq.cloudbreak.workspace.service;

import static com.sequenceiq.cloudbreak.workspace.model.WorkspaceStatus.DELETED;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.authorization.UmsAuthorizationService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.user.UserService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.ResourceAction;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceRole;

@Service
public class WorkspaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceService.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private WorkspaceRepository workspaceRepository;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceModificationVerifierService verifierService;

    @Inject
    private Clock clock;

    @Inject
    private UmsAuthorizationService umsAuthorizationService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    public Workspace create(User user, Workspace workspace) {
        try {
            return transactionService.required(() -> {
                workspace.setResourceCrnByUser(user);
                umsAuthorizationService.assignResourceRoleToUserInWorkspace(user, workspace, WorkspaceRole.WORKSPACEMANAGER);
                return workspaceRepository.save(workspace);
            });
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof DataIntegrityViolationException) {
                throw new BadRequestException(String.format("Workspace with name '%s' in your tenant already exists.",
                        workspace.getName()), e.getCause());
            }
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Workspace create(Workspace workspace) {
        try {
            return transactionService.required(() -> workspaceRepository.save(workspace));
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof DataIntegrityViolationException) {
                throw new BadRequestException(String.format("Workspace with name '%s' in your tenant already exists.",
                        workspace.getName()), e.getCause());
            }
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public Set<Workspace> retrieveForUser(User user) {
        //return umsAuthorizationService.getWorkspacesOfCurrentUser(user);
        return Sets.newHashSet(getByName(getAccountWorkspaceName(user), user).get());
    }

    public Workspace getDefaultWorkspace() {
        CloudbreakUser user = authenticatedUserService.getCbUser();
        return workspaceRepository.getByName(getAccountWorkspaceName(user), user.getTenant());
    }

    public Workspace getDefaultWorkspaceForUser(User user) {
        return workspaceRepository.getByName(getAccountWorkspaceName(user), user.getTenant());
    }

    public Optional<Workspace> getByName(String name, User user) {
        return Optional.ofNullable(workspaceRepository.getByName(getAccountWorkspaceName(user), user.getTenant()));
    }

    public Optional<Workspace> getByNameForUser(String name, User user) {
        return Optional.ofNullable(workspaceRepository.getByName(getAccountWorkspaceName(user), user.getTenant()));
    }

    /**
     * Use this method with caution, since it is not authorized! Don!t use it in REST context!
     *
     * @param id id of Workspace
     * @return Workspace
     */
    public Workspace getByIdWithoutAuth(Long id) {
        Optional<Workspace> workspace = workspaceRepository.findById(id);
        if (workspace.isPresent()) {
            return workspace.get();
        }
        throw new IllegalArgumentException(String.format("No Workspace found with id: %s", id));
    }

    public Workspace getByIdForCurrentUser(Long id) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        return get(id, user);
    }

    public Workspace get(Long id, User user) {
        return getDefaultWorkspaceForUser(user);
    }

}
