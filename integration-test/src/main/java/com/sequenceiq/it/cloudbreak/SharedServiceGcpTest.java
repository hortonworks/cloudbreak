package com.sequenceiq.it.cloudbreak;

import static com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider.GCP;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Gcp.Database.Hive;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Gcp.Database.Ranger;

public class SharedServiceGcpTest extends SharedServiceTestRoot {

    public SharedServiceGcpTest() {
        this(LoggerFactory.getLogger(SharedServiceGcpTest.class), GCP, Hive.CONFIG_NAME, Ranger.CONFIG_NAME);
    }

    private SharedServiceGcpTest(@Nonnull Logger logger, String implementation, String hiveConfigKey, String rangerConfigKey) {
        super(logger, implementation, hiveConfigKey, rangerConfigKey);
    }

}
