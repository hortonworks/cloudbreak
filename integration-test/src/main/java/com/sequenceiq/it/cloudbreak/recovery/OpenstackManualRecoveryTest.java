package com.sequenceiq.it.cloudbreak.recovery;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;


public class OpenstackManualRecoveryTest extends AbstractManualRecoveryTest {
    @Value("${integrationtest.openstackcredential.tenantName}")
    private String defaultTenantName;

    @Value("${integrationtest.openstackcredential.userName}")
    private String defaultUserName;

    @Value("${integrationtest.openstackcredential.password}")
    private String defaultPassword;

    @Value("${integrationtest.openstackcredential.endpoint}")
    private String defaultEndpoint;

    @BeforeMethod
    @Parameters({ "tenantName", "userName", "password", "endpoint" })
    public void setCloudProviderParameters(@Optional("") String tenantName,
            @Optional("") String userName, @Optional("") String password, @Optional("") String endpoint) {
        tenantName = StringUtils.hasLength(tenantName) ? tenantName : defaultTenantName;
        userName = StringUtils.hasLength(userName) ? userName : defaultUserName;
        password = StringUtils.hasLength(password) ? password : defaultPassword;
        endpoint = StringUtils.hasLength(endpoint) ? endpoint : defaultEndpoint;

        Map<String, String> cpd = getCloudProviderParams();
        cpd.put("cloudProvider", "OPENSTACK");
        cpd.put("tenantName", tenantName);
        cpd.put("userName", userName);
        cpd.put("password", password);
        cpd.put("endpoint", endpoint);
    }
}
