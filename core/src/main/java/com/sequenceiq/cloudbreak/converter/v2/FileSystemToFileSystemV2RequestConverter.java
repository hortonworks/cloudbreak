package com.sequenceiq.cloudbreak.converter.v2;

import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemResolver;
import com.sequenceiq.cloudbreak.api.model.v2.FileSystemV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.FileSystem;

public class FileSystemToFileSystemV2RequestConverter extends AbstractConversionServiceAwareConverter<FileSystem, FileSystemV2Request> {

    @Override
    public FileSystemV2Request convert(FileSystem source) {
        FileSystemV2Request request = new FileSystemV2Request();
        request.setDescription(source.getDescription());
        FileSystemResolver.setFSV2RequestFileSystemParamsByNameAndProperties(source.getType(), source.getProperties(), request);
        return request;
    }

}
