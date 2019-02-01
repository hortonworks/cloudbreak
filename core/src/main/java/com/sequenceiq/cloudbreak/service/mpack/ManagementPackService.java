package com.sequenceiq.cloudbreak.service.mpack;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.repository.ManagementPackRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;

@Service
public class ManagementPackService extends AbstractWorkspaceAwareResourceService<ManagementPack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementPackService.class);

    @Inject
    private ManagementPackRepository mpackRepository;

    @Inject
    private ManagementPackCreationValidator managementPackCreationValidator;

    /**
     * @param id id of mpack
     * @return the mpack
     * @deprecated the queries for id are not supported on V3 API, this is only for backward compability. Should be remove with V1 API.
     */
    @Deprecated(since = "2.8.0", forRemoval = true)
    public ManagementPack getById(Long id) {
        return mpackRepository.findById(id).orElseThrow(notFound("Mpack", id));
    }

    @Override
    public WorkspaceResourceRepository<ManagementPack, Long> repository() {
        return mpackRepository;
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.MPACK;
    }

    @Override
    protected void prepareDeletion(ManagementPack resource) {

    }

    @Override
    protected void prepareCreation(ManagementPack resource) {
        ValidationResult validationResult = managementPackCreationValidator.validate(resource);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }
}
