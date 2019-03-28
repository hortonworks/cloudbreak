package com.sequenceiq.cloudbreak.service.filesystem;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.repository.FileSystemRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;

@Service
public class FileSystemConfigService extends AbstractWorkspaceAwareResourceService<FileSystem> {

    @Inject
    private FileSystemRepository fileSystemRepository;

    @Override
    public WorkspaceResourceRepository<FileSystem, Long> repository() {
        return fileSystemRepository;
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.STACK;
    }

    public FileSystem getByIdFromAnyAvailableWorkspace(Long id) {
        return fileSystemRepository.findById(id).orElseThrow(notFound("File system", id));
    }

    @Override
    protected void prepareDeletion(FileSystem resource) {
    }

    public FileSystem deleteByIdFromAnyAvailableWorkspace(Long id) {
        return delete(getByIdFromAnyAvailableWorkspace(id));
    }

    @Override
    protected void prepareCreation(FileSystem resource) {
    }
}
