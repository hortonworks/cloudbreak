package com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;

public abstract class StreamingAppRdsRoleConfigProviderUtil {

    public static String dataBaseTypeForCM(DatabaseVendor vendor) {
        return DatabaseVendor.POSTGRES == vendor ? "postgresql" : vendor.databaseType();
    }

}
