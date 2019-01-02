package com.sequenceiq.it.cloudbreak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseTestV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
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
            @Optional("") String rdsConnectionUrl, @Optional("POSTGRES") String rdsDbType, @Optional("2.5") String hdpVersion) {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        rdsUser = StringUtils.hasLength(rdsUser) ? rdsUser : defaultRdsUser;
        rdsPassword = StringUtils.hasLength(rdsPassword) ? rdsPassword : defaultRdsPassword;
        rdsConnectionUrl = StringUtils.hasLength(rdsConnectionUrl) ? rdsConnectionUrl : defaultRdsConnectionUrl;

        DatabaseV4Request rdsCreateRequest = new DatabaseV4Request();
        rdsCreateRequest.setName(rdsName);
        rdsCreateRequest.setConnectionUserName(rdsUser);
        rdsCreateRequest.setConnectionPassword(rdsPassword);
        rdsCreateRequest.setConnectionURL(rdsConnectionUrl);
        DatabaseTestV4Request testRequest = new DatabaseTestV4Request();
        testRequest.setDatabase(rdsCreateRequest);
        // WHEN
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        String rdsConnectionResult = getCloudbreakClient().databaseV4Endpoint().test(workspaceId, testRequest).getResult();
        Assert.assertEquals(rdsConnectionResult, "connected", "RDS connection test failed. Set the RDS configuration parameters properly.");
        String rdsConfigId = getCloudbreakClient().databaseV4Endpoint().create(workspaceId, rdsCreateRequest).getId().toString();
        itContext.putContextParam(CloudbreakITContextConstants.RDS_CONFIG_ID, rdsConfigId);
        itContext.putCleanUpParam(CloudbreakITContextConstants.RDS_CONFIG_ID, rdsConfigId);
        //THEN
        String listedRDSByName = getCloudbreakClient().databaseV4Endpoint().get(workspaceId, rdsName).getName();
        Assert.assertEquals(listedRDSByName, rdsName);
    }

}



