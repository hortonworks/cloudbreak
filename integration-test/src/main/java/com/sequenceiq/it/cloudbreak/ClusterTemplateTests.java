package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.ClusterTemplate;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.util.LongStringGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Objects;

public class ClusterTemplateTests extends CloudbreakTest {

    private static final String VALID_CT_NAME = "valid-template";

    private static final String AGAIN_CT_NAME = "again-template";

    private static final String LONG_DC_CT_NAME = "long-description-template";

    private static final String DELETE_CT_NAME = "delete-template";

    private static final String SPECIAL_CT_NAME = "@#$|:&* ABC";

    private static final String ILLEGAL_CT_NAME = "Illegal template name %s";

    private static final String INVALID_SHORT_CT_NAME = "";

    private static final String EMPTY_CT_NAME = "temp-empty-template";

    private static final String INVALIDURL_CT_NAME = "temp-url-template";

    private static final String CT_DESCRIPTION = "temporary template for API E2E tests";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateTests.class);

    private String errorMessage = "";

    private CloudProvider cloudProvider;

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    public ClusterTemplateTests() {
    }

    public ClusterTemplateTests(CloudProvider cp, TestParameter tp) {
        cloudProvider = cp;
        setTestParameter(tp);
    }

    @Test(priority = 1, groups = "templates")
    public void testCreateValidClusterTemplate() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterTemplate.request()
                .withName(VALID_CT_NAME)
                .withDescription(CT_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform())
                .withClusterTemplateJSON(getClusterTemplateFile()), VALID_CT_NAME + " cluster template request.");
        when(ClusterTemplate.post(), VALID_CT_NAME + " cluster template request has been posted.");
        then(ClusterTemplate.assertThis(
                (clusterTemplate, t) -> Assert.assertEquals(clusterTemplate.getResponse().getName(), VALID_CT_NAME)),
                VALID_CT_NAME + " should be the name of the new cluster template."
        );
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 2, dataProvider = "templatename", groups = "templates")
    public void testCreateInvalidNameClusterTemplate(TestContext testContext, String templateName) throws Exception {
        LOGGER.info("testing cluster template execution for: {}", templateName);

        given(CloudbreakClient.created());
        given(ClusterTemplate.request()
                .withName(templateName)
                .withDescription(CT_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform())
                .withClusterTemplateJSON(getClusterTemplateFile()), templateName + " cluster template request.");
        when(ClusterTemplate.post(), templateName + " cluster template name request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 3, groups = "templates")
    public void testCreateAgainClusterTemplate() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterTemplate.created()
                .withName(AGAIN_CT_NAME), AGAIN_CT_NAME + " cluster template is created.");
        given(ClusterTemplate.request()
                .withName(AGAIN_CT_NAME)
                .withDescription(CT_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform())
                .withClusterTemplateJSON(getClusterTemplateFile()), AGAIN_CT_NAME + " cluster template request.");
        when(ClusterTemplate.post(), AGAIN_CT_NAME + " cluster template request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 4, groups = "templates")
    public void testCreateLongDescriptionClusterTemplate() throws Exception {
        String invalidLongDescripton = longStringGeneratorUtil.stringGenerator(1001);

        given(CloudbreakClient.created());
        given(ClusterTemplate.request()
                .withName(LONG_DC_CT_NAME)
                .withDescription(invalidLongDescripton)
                .withCloudPlatform(cloudProvider.getPlatform())
                .withClusterTemplateJSON(getClusterTemplateFile()), LONG_DC_CT_NAME + " cluster template request.");
        when(ClusterTemplate.post(), LONG_DC_CT_NAME + " cluster template description request has been posted.");
    }

    //BUG-113364 [API] Empty Cluster Template validation is missing
    @Test(expectedExceptions = BadRequestException.class, priority = 5, groups = "templates", enabled = false)
    public void testCreateEmptyJSONClusterTemplateException() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterTemplate.request()
                .withName(EMPTY_CT_NAME)
                .withDescription(CT_DESCRIPTION)
                .withCloudPlatform(cloudProvider.getPlatform())
                .withClusterTemplateJSON(getEmptyClusterTemplateFile()), EMPTY_CT_NAME + " cluster template with no content request.");
        when(ClusterTemplate.post(), EMPTY_CT_NAME + " cluster template with no content request has been posted.");
    }

    @Test(expectedExceptions = ForbiddenException.class, priority = 6, groups = "templates")
    public void testDeleteValidClusterTemplate() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterTemplate.request()
                .withName(DELETE_CT_NAME), DELETE_CT_NAME + " cluster template delete request.");
        when(ClusterTemplate.delete(), DELETE_CT_NAME + " cluster template delete request has been posted.");
        when(ClusterTemplate.get(), DELETE_CT_NAME + " cluster template should not be present in response.");
    }

    @BeforeTest
    @Parameters("provider")
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("before cluster template test set provider: " + provider);
        if (cloudProvider == null) {
            cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        } else {
            LOGGER.info("cloud provider already set - running from factory test");
        }
    }

    @AfterTest
    public void cleanUp() throws Exception {
        String[] nameArray = {VALID_CT_NAME};

        for (String aNameArray : nameArray) {
            LOGGER.info("Delete cluster template: \'{}\'", aNameArray.toLowerCase().trim());
            try {
                given(CloudbreakClient.created());
                given(ClusterTemplate.request()
                        .withName(aNameArray));
                when(ClusterTemplate.delete());
            } catch (ForbiddenException | BadRequestException e) {
                try (Response response = e.getResponse()) {
                    String exceptionMessage = response.readEntity(String.class);
                    errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                    LOGGER.info("Clean Up Exception message ::: " + errorMessage);
                }
            }
        }
    }

    private String getClusterTemplateFile() throws IOException {
        return new String(StreamUtils.copyToByteArray(Objects.requireNonNull(applicationContext)
                .getResource("classpath:/clustertemplate/openstack.json")
                .getInputStream()));
    }

    private String getEmptyClusterTemplateFile() throws IOException {
        return new String(StreamUtils.copyToByteArray(Objects.requireNonNull(applicationContext)
                .getResource("classpath:/clustertemplate/empty.json")
                .getInputStream()));
    }

    @DataProvider(name = "templatename")
    public Object[][] providerClusterTemplateName() {
        return new Object[][]{
                {Objects.requireNonNull(applicationContext).getBean(TestContext.class), SPECIAL_CT_NAME},
                {Objects.requireNonNull(applicationContext).getBean(TestContext.class), String.format(ILLEGAL_CT_NAME, ";")},
                {Objects.requireNonNull(applicationContext).getBean(TestContext.class), String.format(ILLEGAL_CT_NAME, "%")},
                {Objects.requireNonNull(applicationContext).getBean(TestContext.class), String.format(ILLEGAL_CT_NAME, "/")},
                {Objects.requireNonNull(applicationContext).getBean(TestContext.class), INVALID_SHORT_CT_NAME},
                {Objects.requireNonNull(applicationContext).getBean(TestContext.class), longStringGeneratorUtil.stringGenerator(101)}
        };
    }

}
