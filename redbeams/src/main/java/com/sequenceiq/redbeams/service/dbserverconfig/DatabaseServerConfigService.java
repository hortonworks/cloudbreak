package com.sequenceiq.redbeams.service.dbserverconfig;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.repository.DatabaseServerConfigRepository;

@Service
public class DatabaseServerConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerConfigService.class);

    @Inject
    private DatabaseServerConfigRepository repository;

    public Set<DatabaseServerConfig> findAllInWorkspaceAndEnvironment(Long workspaceId, String environmentId, Boolean attachGlobal) {
        return repository.findAllByWorkspaceIdAndEnvironmentId(workspaceId, environmentId);
    }

    public DatabaseServerConfig create(DatabaseServerConfig resource, Long workspaceId) {
        // FIXME? Currently no checks if logged-in user has access to workspace
        // Compare with AbstractWorkspaceAwareResourceService
        try {
            MDCBuilder.buildMdcContext(resource);
            // prepareCreation(resource);
            resource.setWorkspaceId(workspaceId);
            return repository.save(resource);
        } catch (AccessDeniedException e) {
            ConstraintViolationException cve = null;
            for (Throwable t = e.getCause(); t != null; t = t.getCause()) {
                if (t instanceof ConstraintViolationException) {
                    cve = (ConstraintViolationException) t;
                    break;
                }
            }
            if (cve != null) {
                String message = String.format("%s already exists with name '%s' in workspace %d",
                        resource().getShortName(), resource.getName(), resource.getWorkspaceId());
                throw new BadRequestException(message, e);
            }
            throw e;
        }
    }

    public DatabaseServerConfig getByNameInWorkspace(Long workspaceId, String name) {
        Optional<DatabaseServerConfig> resourceOpt = repository.findByNameAndWorkspaceId(name, workspaceId);
        if (resourceOpt.isEmpty()) {
            throw new NotFoundException(String.format("No %s found with name '%s'", resource().getShortName(), name));
        }
        MDCBuilder.buildMdcContext(resourceOpt.get());
        return resourceOpt.get();
    }

    public DatabaseServerConfig deleteByNameInWorkspace(Long workspaceId, String name) {
        DatabaseServerConfig resource = getByNameInWorkspace(workspaceId, name);
        return delete(resource);
    }

    DatabaseServerConfig delete(DatabaseServerConfig resource) {
        LOGGER.debug("Deleting {} with name: {}", resource().getReadableName(), resource.getName());
        MDCBuilder.buildMdcContext(resource);
        // prepareDeletion(resource);
        repository.delete(resource);
        return resource;
    }

    public Set<DatabaseServerConfig> deleteMultipleByNameInWorkspace(Long workspaceId, Set<String> names) {
        Set<DatabaseServerConfig> resources = getByNamesInWorkspace(workspaceId, names);
        return resources.stream()
            .map(r -> delete(r))
            .collect(Collectors.toSet());
    }

    Set<DatabaseServerConfig> getByNamesInWorkspace(Long workspaceId, Set<String> names) {
        Set<DatabaseServerConfig> resources = repository.findByNameInAndWorkspaceId(names, workspaceId);
        Set<String> notFound = Sets.difference(names,
                resources.stream().map(DatabaseServerConfig::getName).collect(Collectors.toSet()));

        if (!notFound.isEmpty()) {
            throw new NotFoundException(String.format("No %s(s) found with name(s) %s", resource().getShortName(),
                    notFound.stream().map(name -> '\'' + name + '\'').collect(Collectors.joining(", "))));
        }

        return resources;
    }

    public WorkspaceResource resource() {
        return WorkspaceResource.DATABASE_SERVER;
    }
}
