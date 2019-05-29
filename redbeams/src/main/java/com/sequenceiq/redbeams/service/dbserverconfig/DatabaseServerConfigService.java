package com.sequenceiq.redbeams.service.dbserverconfig;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.FieldError;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.archive.AbstractArchivistService;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.repository.DatabaseServerConfigRepository;
import com.sequenceiq.redbeams.service.validation.DatabaseServerConnectionValidator;

@Service
public class DatabaseServerConfigService extends AbstractArchivistService<DatabaseServerConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerConfigService.class);

    @Inject
    private DatabaseServerConfigRepository repository;

    @Inject
    private DatabaseServerConnectionValidator connectionValidator;

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
        } catch (AccessDeniedException | DataIntegrityViolationException e) {
            Optional<Throwable> cve = Throwables.getCausalChain(e).stream()
                .filter(c -> c instanceof ConstraintViolationException)
                .findFirst();
            if (cve.isPresent()) {
                String message = String.format("%s already exists with name '%s' in workspace %d",
                        resource().getShortName(), resource.getName(), resource.getWorkspaceId());
                throw new BadRequestException(message, cve.get());
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

    @Override
    public JpaRepository repository() {
        return repository;
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

    public String testConnection(Long workspaceId, String name) {
        return testConnection(getByNameInWorkspace(workspaceId, name));
    }

    public String testConnection(DatabaseServerConfig resource) {
        MapBindingResult errors = new MapBindingResult(new HashMap(), "databaseServer");
        connectionValidator.validate(resource, errors);
        if (!errors.hasErrors()) {
            return "success";
        }
        return errors.getAllErrors().stream()
            .map(e -> (e instanceof FieldError ? ((FieldError) e).getField() + ": " : "") + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
    }

    public WorkspaceResource resource() {
        return WorkspaceResource.DATABASE_SERVER;
    }
}
