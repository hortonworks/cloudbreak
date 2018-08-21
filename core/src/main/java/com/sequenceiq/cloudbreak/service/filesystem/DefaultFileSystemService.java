package com.sequenceiq.cloudbreak.service.filesystem;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.repository.FileSystemRepository;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

@Service
public class DefaultFileSystemService extends AbstractOrganizationAwareResourceService<FileSystem> implements FileSystemService {

    @Inject
    private FileSystemRepository fileSystemRepository;

    @Inject
    private AuthorizationService authService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Override
    protected OrganizationResourceRepository<FileSystem, Long> repository() {
        return fileSystemRepository;
    }

    @Override
    protected OrganizationResource resource() {
        return OrganizationResource.FILESYSTEM;
    }

    @Override
    protected void prepareDeletion(FileSystem resource) {

    }

    @Override
    protected void prepareCreation(FileSystem resource) {
        IdentityUser user = authenticatedUserService.getCbUser();
        resource.setAccount(user.getAccount());
        resource.setOwner(user.getUserId());
    }

    private FileSystem getFileSystem(Long id) {
        return fileSystemRepository.findById(id).orElseThrow(notFound("Record", id));
    }
}
