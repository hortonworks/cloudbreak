package com.sequenceiq.cloudbreak.api.endpoint.v4.util;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class DatabaseVendorConverter extends DefaultEnumConverter<DatabaseVendor> {

    @Override
    public DatabaseVendor getDefault() {
        return DatabaseVendor.EMBEDDED;
    }
}
