package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Azure.Database.Hive;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Azure.Database.Ranger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeTest;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.sequenceiq.it.cloudbreak.newway.cloud.AzureCloudProvider.AZURE;

public class SharedServiceAzureTest extends SharedServiceTestRoot {

    public SharedServiceAzureTest() {
        this(LoggerFactory.getLogger(SharedServiceAzureTest.class), AZURE, Hive.CONFIG_NAME, Ranger.CONFIG_NAME);
    }

    private SharedServiceAzureTest(@Nonnull Logger logger, @Nonnull String implementation, String hiveConfigKey, String rangerConfigKey) {
        super(logger, implementation, hiveConfigKey, rangerConfigKey);
    }

    @BeforeTest
    public void initialize() {
        setCloudProvider(CloudProviderHelper.providerFactory(AZURE, getTestParameter()));
        setResourceHelper(getCloudProvider().getResourceHelper());
        setOptionalClusterPostfix(Optional.ofNullable(getTestParameter().get("cloudStorageType"))
                .orElseThrow(() -> new SkipException("Unable to find cloudStorageType value which is necessary to choose the right implementation!"))
                .toLowerCase());
    }
}
