package com.sequenceiq.cloudbreak.converter.v2;

import static com.sequenceiq.cloudbreak.common.type.APIResourceType.FILESYSTEM;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemParameters;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemResolver;
import com.sequenceiq.cloudbreak.api.model.v2.FileSystemV2Request;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class FileSystemV2RequestToFileSystemConverter extends AbstractConversionServiceAwareConverter<FileSystemV2Request, FileSystem> {

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private MissingResourceNameGenerator nameGenerator;

    @Override
    public FileSystem convert(FileSystemV2Request source) {
        FileSystem fileSystem = new FileSystem();
        fileSystem.setName(nameGenerator.generateName(FILESYSTEM));
        fileSystem.setDefaultFs(false);
        IdentityUser user = authenticatedUserService.getCbUser();
        fileSystem.setOwner(user.getUserId());
        fileSystem.setAccount(user.getAccount());
        fileSystem.setDescription(source.getDescription());
        FileSystemParameters fileSystemProperties = FileSystemResolver.decideFileSystemFromFileSystemV2Request(source);
        fileSystem.setType(fileSystemProperties.getType().name());
        fileSystem.setProperties(fileSystemProperties.getAsMap());
        return fileSystem;
    }
}
