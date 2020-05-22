package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;

public class DirectoryTypeConverter extends DefaultEnumConverter<DirectoryType> {

    @Override
    public DirectoryType getDefault() {
        return DirectoryType.ACTIVE_DIRECTORY;
    }
}
