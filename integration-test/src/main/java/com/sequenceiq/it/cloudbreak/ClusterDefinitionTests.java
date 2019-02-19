package com.sequenceiq.it.cloudbreak;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinition;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class ClusterDefinitionTests extends CloudbreakTest {

    private static final String VALID_CD_NAME = "valid-br";

    private static final String AGAIN_CD_NAME = "again-blueprint";

    private static final String LONG_DC_CD_NAME = "long-description-blueprint";

    private static final String DELETE_CD_NAME = "delete-blueprint";

    private static final String SPECIAL_CD_NAME = "@#$|:&* ABC";

    private static final String ILLEGAL_CD_NAME = "Illegal cluster definition name %s";

    private static final String INVALID_SHORT_CD_NAME = "";

    private static final String EMPTY_CD_NAME = "temp-empty-blueprint";

    private static final String INVALIDURL_CD_NAME = "temp-url-blueprint";

    private static final String CD_DESCRIPTION = "temporary cluster definition for API E2E tests";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDefinitionTests.class);

    private String errorMessage = "";

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @Test(priority = 1, groups = "clusterdefinitions")
    public void testCreateValidClusterDefinition() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterDefinition.request()
                .withName(VALID_CD_NAME)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getClusterDefinitionFile()), VALID_CD_NAME + " cluster definition request.");
        when(ClusterDefinition.post(), VALID_CD_NAME + " cluster definition request has been posted.");
        then(ClusterDefinition.assertThis(
                (clusterDefinition, t) -> Assert.assertEquals(clusterDefinition.getResponse().getName(), VALID_CD_NAME)),
                VALID_CD_NAME + " should be the name of the new cluster definition."
        );
    }

    //BUG-95607
    @Test(priority = 2, groups = "clusterdefinitions")
    public void testCreateSpecialCharacterClusterDefinition() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterDefinition.request()
                .withName(SPECIAL_CD_NAME)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getClusterDefinitionFile()), SPECIAL_CD_NAME + " cluster definition name request.");
        when(ClusterDefinition.post(), SPECIAL_CD_NAME + " cluster definition name request has been posted.");
        then(ClusterDefinition.assertThis(
                (clusterDefinition, t) -> Assert.assertEquals(clusterDefinition.getResponse().getName(), SPECIAL_CD_NAME)),
                SPECIAL_CD_NAME + " should be the name of the new cluster definition."
        );
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 3, groups = "clusterdefinitions")
    public void testCreateClusterDefinitionWithIllegalSemicolonCharacterInNameShouldThrowBadRequestException() throws Exception {
        given(CloudbreakClient.created());
        String clusterDefinitionName = String.format(ILLEGAL_CD_NAME, ";");
        given(ClusterDefinition.request()
                .withName(clusterDefinitionName)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getClusterDefinitionFile()), clusterDefinitionName + " cluster definition name request.");
        when(ClusterDefinition.post(), clusterDefinitionName + " cluster definition name request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 4, groups = "clusterdefinitions")
    public void testCreateClusterDefinitionWithIllegalPercentageCharacterInNameShouldThrowBadRequestException() throws Exception {
        given(CloudbreakClient.created());
        String clusterDefinitionName = String.format(ILLEGAL_CD_NAME, "%");
        given(ClusterDefinition.request()
                .withName(clusterDefinitionName)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getClusterDefinitionFile()), clusterDefinitionName + " cluster definition name request.");
        when(ClusterDefinition.post(), clusterDefinitionName + " cluster definition name request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 5, groups = "clusterdefinitions")
    public void testCreateClusterDefinitionWithIllegalSlashCharacterInNameShouldThrowBadRequestException() throws Exception {
        given(CloudbreakClient.created());
        String clusterDefinitionName = String.format(ILLEGAL_CD_NAME, "/");
        given(ClusterDefinition.request()
                .withName(clusterDefinitionName)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getClusterDefinitionFile()), clusterDefinitionName + " cluster definition name request.");
        when(ClusterDefinition.post(), clusterDefinitionName + " cluster definition name request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 6, groups = "clusterdefinitions")
    public void testCreateAgainClusterDefinition() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterDefinition.isCreated()
                .withName(AGAIN_CD_NAME), AGAIN_CD_NAME + " cluster definition is created.");
        given(ClusterDefinition.request()
                .withName(AGAIN_CD_NAME)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getClusterDefinitionFile()), AGAIN_CD_NAME + " cluster definition request.");
        when(ClusterDefinition.post(), AGAIN_CD_NAME + " cluster definition request has been posted.");
    }

    //"The length of the cluster definition's name has to be in range of 1 to 100"
    @Test(expectedExceptions = BadRequestException.class, priority = 7, groups = "clusterdefinitions")
    public void testCreateInvalidShortClusterDefinition() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterDefinition.request()
                .withName(INVALID_SHORT_CD_NAME)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getClusterDefinitionFile()), "0 char cluster definition name request.");
        when(ClusterDefinition.post(), "0 char cluster definition name request has been posted.");
    }

    //"The length of the cluster definition's name has to be in range of 1 to 100"
    @Test(expectedExceptions = BadRequestException.class, priority = 8, groups = "clusterdefinitions")
    public void testCreateInvalidLongClusterDefinition() throws Exception {
        String invalidLongName = longStringGeneratorUtil.stringGenerator(101);

        given(CloudbreakClient.created());
        given(ClusterDefinition.request()
                .withName(invalidLongName)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getClusterDefinitionFile()), invalidLongName + " cluster definition name request.");
        when(ClusterDefinition.post(), invalidLongName + " cluster definition name request has been posted.");
    }

    //BUG-95609 - Won't fix issue
    //"The length of the cluster definition's description has to be in range of 1 to 1000"
    @Test(expectedExceptions = BadRequestException.class, priority = 9, groups = "clusterdefinitions")
    public void testCreateLongDescriptionClusterDefinition() throws Exception {
        String invalidLongDescripton = longStringGeneratorUtil.stringGenerator(1001);

        given(CloudbreakClient.created());
        given(ClusterDefinition.request()
                .withName(LONG_DC_CD_NAME)
                .withDescription(invalidLongDescripton)
                .withClusterDefinition(getClusterDefinitionFile()), LONG_DC_CD_NAME + " cluster definition description request.");
        when(ClusterDefinition.post(), LONG_DC_CD_NAME + " cluster definition description request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 10, groups = "clusterdefinitions")
    public void testCreateEmptyJSONClusterDefinitionException() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterDefinition.request()
                .withName(EMPTY_CD_NAME)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getEmptyClusterDefinitionFile()), EMPTY_CD_NAME + " cluster definition with no content request.");
        when(ClusterDefinition.post(), EMPTY_CD_NAME + " cluster definition with no content request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 11, groups = "clusterdefinitions")
    public void testCreateEmptyFileClusterDefinitionException() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterDefinition.request()
                .withName(EMPTY_CD_NAME)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getClusterDefinitionUrl()), EMPTY_CD_NAME + " cluster definition file with no content on URL request.");
        when(ClusterDefinition.post(), EMPTY_CD_NAME + " cluster definition file with no content on URL request has been posted.");
    }

    @Test(priority = 12, groups = "clusterdefinitions")
    public void testCreateEmptyFileClusterDefinitionMessage() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterDefinition.request()
                .withName(EMPTY_CD_NAME)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getClusterDefinitionUrl()), EMPTY_CD_NAME + " cluster definition file with no content on URL request.");
        try {
            when(ClusterDefinition.post(), EMPTY_CD_NAME + " cluster definition file with no content on URL request has been posted.");
        } catch (BadRequestException e) {
            try (Response response = e.getResponse()) {
                String exceptionMessage = response.readEntity(String.class);
                errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                LOGGER.info("BadRequestException message ::: " + errorMessage);
            }
        }
        then(ClusterDefinition.assertThis(
                (clusterDefinition, t) -> Assert.assertEquals(errorMessage, " Failed to parse JSON.\"}")),
                "Error message [Failed to parse JSON.] should be present in response."
        );
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 13, groups = "clusterdefinitions")
    public void testCreateInvalidURLClusterDefinitionException() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterDefinition.request()
                .withName(INVALIDURL_CD_NAME)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getClusterDefinitionInvalidUrl()), INVALIDURL_CD_NAME + " cluster definition with invalid URL request.");
        when(ClusterDefinition.post(), INVALIDURL_CD_NAME + " cluster definition with invalid URL request has been posted.");
    }

    @Test(priority = 14, groups = "clusterdefinitions")
    public void testCreateInvalidURLClusterDefinitionMessage() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterDefinition.request()
                .withName(INVALIDURL_CD_NAME)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getClusterDefinitionInvalidUrl()), INVALIDURL_CD_NAME + " cluster definition with invalid URL request.");
        try {
            when(ClusterDefinition.post(), INVALIDURL_CD_NAME + " cluster definition with invalid URL request has been posted.");
        } catch (BadRequestException e) {
            try (Response response = e.getResponse()) {
                String exceptionMessage = response.readEntity(String.class);
                errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                LOGGER.info("BadRequestException message ::: " + errorMessage);
            }
        }
        then(ClusterDefinition.assertThis(
                (clusterDefinition, t) -> Assert.assertEquals(errorMessage, " Failed to parse JSON.\"}")),
                "Error message [Failed to parse JSON.] should be present in response."
        );
    }

    @Test(priority = 15, groups = "clusterdefinitions")
    public void testDeleteValidClusterDefinition() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterDefinition.request()
                .withName(DELETE_CD_NAME), DELETE_CD_NAME + " cluster definition delete request.");
        when(ClusterDefinition.delete(), DELETE_CD_NAME + " cluster definition delete request has been posted.");
        then(ClusterDefinition.assertThis(
                (clusterDefinition, t) -> Assert.assertNull(clusterDefinition.getResponse())),
                DELETE_CD_NAME + " cluster definition should not be present in response."
        );
    }

    @BeforeTest
    public void testCreateDeleteClusterDefinition() throws Exception {
        given(CloudbreakClient.created());
        given(ClusterDefinition.request()
                .withName(DELETE_CD_NAME)
                .withDescription(CD_DESCRIPTION)
                .withClusterDefinition(getClusterDefinitionFile()));
        when(ClusterDefinition.post());
    }

    @AfterTest
    public void cleanUp() throws Exception {
        String[] nameArray = {VALID_CD_NAME, SPECIAL_CD_NAME};

        for (String aNameArray : nameArray) {
            LOGGER.info("Delete cluster definition: \'{}\'", aNameArray.toLowerCase().trim());
            try {
                given(CloudbreakClient.created());
                given(ClusterDefinition.request()
                        .withName(aNameArray));
                when(ClusterDefinition.delete());
            } catch (ForbiddenException | BadRequestException e) {
                try (Response response = e.getResponse()) {
                    String exceptionMessage = response.readEntity(String.class);
                    errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                    LOGGER.info("Clean Up Exception message ::: " + errorMessage);
                }
            }
        }
    }

    private String getClusterDefinitionFile() throws IOException {
        return new String(
                StreamUtils.copyToByteArray(
                        applicationContext
                                .getResource("classpath:/blueprint/hdp-multinode-default.bp")
                                .getInputStream()));
    }

    private String getEmptyClusterDefinitionFile() throws IOException {
        return new String(
                StreamUtils.copyToByteArray(
                        applicationContext
                                .getResource("classpath:/blueprint/empty.bp")
                                .getInputStream()));
    }

    private String getClusterDefinitionUrl() {
        return "https://rawgit.com/hortonworks/cloudbreak/master/integration-test/src/main/resources/"
                + "blueprint/multi-node-hdfs-yarn.bp";
    }

    private String getClusterDefinitionInvalidUrl() {
        return "https://index.hu";
    }
}
