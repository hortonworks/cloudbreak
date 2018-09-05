package com.sequenceiq.cloudbreak.service.filesystem;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.repository.FileSystemRepository;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Service
public class FileSystemConfigService extends AbstractOrganizationAwareResourceService<FileSystem> {

    @Inject
    private FileSystemRepository fileSystemRepository;

    @Inject
    private AuthorizationService authService;

    @Override
    public OrganizationResourceRepository<FileSystem, Long> repository() {
        return fileSystemRepository;
    }

    @Override
    public OrganizationResource resource() {
        return OrganizationResource.FILESYSTEM;
    }

    public FileSystem getByIdFromAnyAvailableOrganization(Long id) {
        return fileSystemRepository.findById(id).orElseThrow(notFound("File system", id));
    }

    @Override
    protected void prepareDeletion(FileSystem resource) {
    }

    public FileSystem deleteByIdFromAnyAvailableOrganization(Long id) {
        return delete(getByIdFromAnyAvailableOrganization(id));
    }

    @Override
    protected void prepareCreation(FileSystem resource) {
    }
}
