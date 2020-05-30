package com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import org.junit.Test;

import java.util.EnumMap;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.StreamingAppRdsRoleConfigProviderUtil.dataBaseTypeForCM;
import static org.assertj.core.api.Assertions.assertThat;

public class StreamingAppRdsRoleConfigProviderUtilTest {

    @Test
    public void testDataBaseTypeForCM() {
        EnumMap<DatabaseVendor, String> expected = new EnumMap<>(DatabaseVendor.class);
        for (DatabaseVendor vendor: DatabaseVendor.values()) {
            expected.put(vendor, vendor.databaseType());
        }
        expected.put(DatabaseVendor.POSTGRES, "postgresql");

        for (DatabaseVendor vendor: DatabaseVendor.values()) {
            assertThat(dataBaseTypeForCM(vendor)).isEqualTo(expected.get(vendor));
        }
    }

}