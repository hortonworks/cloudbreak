package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.filesystem.AbfsFileSystem;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AbfsFileSystemToAbfsCloudStorageParametersConverter
        extends AbstractConversionServiceAwareConverter<AbfsFileSystem, AbfsCloudStorageParameters> {

    @Override
    public AbfsCloudStorageParameters convert(AbfsFileSystem source) {
        AbfsCloudStorageParameters abfsCloudStorageParameters = new AbfsCloudStorageParameters();
        abfsCloudStorageParameters.setAccountName(source.getAccountName());
        abfsCloudStorageParameters.setAccountKey(source.getAccountKey());
        return abfsCloudStorageParameters;
    }
}
