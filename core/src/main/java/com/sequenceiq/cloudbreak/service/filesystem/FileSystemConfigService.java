package com.sequenceiq.cloudbreak.service.filesystem;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.repository.FileSystemRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Service
public class FileSystemConfigService {

    @Inject
    private FileSystemRepository fileSystemRepository;

    @Inject
    private AuthorizationService authService;

    public Set<FileSystem> retrievePrivateFileSystems(@Nonnull IdentityUser user) {
        return fileSystemRepository.findByOwner(user.getUserId());
    }

    public FileSystem create(@Nonnull IdentityUser user, @Nonnull FileSystem fileSystem) {
        setUserDataRelatedFields(user, fileSystem);
        return fileSystemRepository.save(fileSystem);
    }

    public FileSystem getPrivateFileSystem(String name, @Nonnull IdentityUser user) {
        return fileSystemRepository.findByNameAndOwner(name, user.getUserId());
    }

    public FileSystem get(Long id) {
        return getFileSystem(id);
    }

    public Set<FileSystem> retrieveAccountFileSystems(@Nonnull IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN)
                ? fileSystemRepository.findByAccount(user.getAccount())
                : fileSystemRepository.findByAccountAndOwner(user.getAccount(), user.getUserId());
    }

    public void delete(Long id, IdentityUser user) {
        FileSystem fileSystem = getFileSystem(id);
        setUserDataRelatedFields(user, fileSystem);
        fileSystemRepository.delete(fileSystem);
    }

    public void delete(String name, @Nonnull IdentityUser user) {
        FileSystem fileSystem = Optional.ofNullable(fileSystemRepository.findByNameAndAccountAndOwner(name, user.getAccount(), user.getUserId()))
                .orElseThrow(notFound("Record", name));
        setUserDataRelatedFields(user, fileSystem);
        fileSystemRepository.delete(fileSystem);
    }

    private FileSystem getFileSystem(Long id) {
        return fileSystemRepository.findById(id).orElseThrow(notFound("Record", id));
    }

    private void setUserDataRelatedFields(IdentityUser user, FileSystem fileSystem) {
        fileSystem.setAccount(user.getAccount());
        fileSystem.setOwner(user.getUserId());
    }

}
