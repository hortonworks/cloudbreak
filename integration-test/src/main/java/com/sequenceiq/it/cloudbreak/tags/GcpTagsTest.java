package com.sequenceiq.it.cloudbreak.tags;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

public class GcpTagsTest extends AbstractTagTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.sequenceiq.it.cloudbreak.tags.GcpTagsTest.class);

    @Value("${integrationtest.gcpcredential.name}")
    private String defaultName;

    @Value("${integrationtest.gcpcredential.projectId}")
    private String defaultProjectId;

    @Value("${integrationtest.gcpcredential.serviceAccountId}")
    private String defaultServiceAccountId;

    @Value("${integrationtest.gcpcredential.p12File}")
    private String defaultP12File;

    @BeforeMethod
    @Parameters({ "region", "name", "projectId", "serviceAccountId", "p12File" })
    public void checkGcpTags(@Optional ("europe-west1-b") String region, @Optional ("") String name, @Optional ("") String projectId,
    @Optional ("") String serviceAccountId, @Optional ("") String p12File) throws Exception {
        name = StringUtils.hasLength(name) ? name : defaultName;
        projectId = StringUtils.hasLength(projectId) ? projectId : defaultProjectId;
        serviceAccountId = StringUtils.hasLength(serviceAccountId) ? serviceAccountId : defaultServiceAccountId;
        p12File = StringUtils.hasLength(p12File) ? p12File : defaultP12File;

        Map<String, String> cpd = getCloudProviderParams();
        cpd.put("cloudProvider", "GCP");
        cpd.put("region", region);
        cpd.put("applicationName", name);
        cpd.put("projectId", projectId);
        cpd.put("serviceAccountId", serviceAccountId);
        cpd.put("p12File", p12File);
    }
}
