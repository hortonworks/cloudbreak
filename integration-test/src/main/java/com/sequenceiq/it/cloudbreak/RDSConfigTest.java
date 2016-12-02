package com.sequenceiq.it.cloudbreak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.RDSDatabase;
import com.sequenceiq.it.IntegrationTestContext;

public class RDSConfigTest extends AbstractCloudbreakIntegrationTest {

    @Value("${integrationtest.rdsconfig.rdsUser}")
    private String defaultRdsUser;

    @Value("${integrationtest.rdsconfig.rdsPassword}")
    private String defaultRdsPassword;

    @Value("${integrationtest.rdsconfig.rdsConnectionUrl}")
    private String defaultRdsConnectionUrl;

    @Test
    @Parameters({ "rdsName", "rdsUser", "rdsPassword", "rdsConnectionUrl", "rdsDbType", "hdpVersion" })
    public void testRDSConfig(String rdsName, @Optional("") String rdsUser, @Optional("") String rdsPassword,
            @Optional("") String rdsConnectionUrl, @Optional("POSTGRES") String rdsDbType, @Optional("2.5") String hdpVersion) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        rdsUser = StringUtils.hasLength(rdsUser) ? rdsUser : defaultRdsUser;
        rdsPassword = StringUtils.hasLength(rdsPassword) ? rdsPassword : defaultRdsPassword;
        rdsConnectionUrl = StringUtils.hasLength(rdsConnectionUrl) ? rdsConnectionUrl : defaultRdsConnectionUrl;

        RDSConfigJson rdsCreateRequest = new RDSConfigJson();
        rdsCreateRequest.setName(rdsName);
        rdsCreateRequest.setConnectionUserName(rdsUser);
        rdsCreateRequest.setConnectionPassword(rdsPassword);
        rdsCreateRequest.setConnectionURL(rdsConnectionUrl);
        rdsCreateRequest.setDatabaseType(RDSDatabase.valueOf(rdsDbType));
        rdsCreateRequest.setHdpVersion(hdpVersion);
        // WHEN
        String rdsConnectionResult = getCloudbreakClient().utilEndpoint().testRdsConnection(rdsCreateRequest).getConnectionResult();
        Assert.assertEquals(rdsConnectionResult, "connected", "RDS connection test failed. Set the RDS configuration parameters properly.");
        String rdsConfigId = getCloudbreakClient().rdsConfigEndpoint().postPrivate(rdsCreateRequest).getId().toString();
        itContext.putContextParam(CloudbreakITContextConstants.RDS_CONFIG_ID, rdsConfigId);
        itContext.putCleanUpParam(CloudbreakITContextConstants.RDS_CONFIG_ID, rdsConfigId);
        //THEN
        String listedRDSByName = getCloudbreakClient().rdsConfigEndpoint().getPrivate(rdsName).getName();
        Assert.assertEquals(listedRDSByName, rdsName);
    }

}



