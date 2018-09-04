package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.filesystem.AbfsFileSystem;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AbfsCloudStorageParametersToAbfsFileSystemConverter
        extends AbstractConversionServiceAwareConverter<AbfsCloudStorageParameters, AbfsFileSystem> {

    @Override
    public AbfsFileSystem convert(AbfsCloudStorageParameters source) {
        AbfsFileSystem abfsFileSystem = new AbfsFileSystem();
        abfsFileSystem.setAccountName(source.getAccountName());
        abfsFileSystem.setAccountKey(source.getAccountKey());
        return abfsFileSystem;
    }
}
