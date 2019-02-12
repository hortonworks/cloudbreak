package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.entity.blueprint.Blueprint;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.util.LongStringGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class BlueprintTests extends CloudbreakTest {

    private static final String VALID_BP_NAME = "valid-blueprint";

    private static final String AGAIN_BP_NAME = "again-blueprint";

    private static final String LONG_DC_BP_NAME = "long-description-blueprint";

    private static final String DELETE_BP_NAME = "delete-blueprint";

    private static final String SPECIAL_BP_NAME = "@#$|:&* ABC";

    private static final String ILLEGAL_BP_NAME = "Illegal blueprint name %s";

    private static final String INVALID_SHORT_BP_NAME = "";

    private static final String EMPTY_BP_NAME = "temp-empty-blueprint";

    private static final String INVALIDURL_BP_NAME = "temp-url-blueprint";

    private static final String BP_DESCRIPTION = "temporary blueprint for API E2E tests";

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintTests.class);

    private String errorMessage = "";

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @Test(priority = 1, groups = "blueprints")
    public void testCreateValidBlueprint() throws Exception {
        given(CloudbreakClient.created());
        given(Blueprint.request()
                .withName(VALID_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()), VALID_BP_NAME + " blueprint request.");
        when(Blueprint.post(), VALID_BP_NAME + " blueprint request has been posted.");
        then(Blueprint.assertThis(
                (blueprint, t) -> Assert.assertEquals(blueprint.getResponse().getName(), VALID_BP_NAME)),
                VALID_BP_NAME + " should be the name of the new blueprint."
        );
    }

    //BUG-95607
    @Test(priority = 2, groups = "blueprints")
    public void testCreateSpecialCharacterBlueprint() throws Exception {
        given(CloudbreakClient.created());
        given(Blueprint.request()
                .withName(SPECIAL_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()), SPECIAL_BP_NAME + " blueprint name request.");
        when(Blueprint.post(), SPECIAL_BP_NAME + " blueprint name request has been posted.");
        then(Blueprint.assertThis(
                (blueprint, t) -> Assert.assertEquals(blueprint.getResponse().getName(), SPECIAL_BP_NAME)),
                SPECIAL_BP_NAME + " should be the name of the new blueprint."
        );
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 3, groups = "blueprints")
    public void testCreateBlueprintWithIllegalSemicolonCharacterInNameShouldThrowBadRequestException() throws Exception {
        given(CloudbreakClient.created());
        String blueprintName = String.format(ILLEGAL_BP_NAME, ";");
        given(Blueprint.request()
                .withName(blueprintName)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()), blueprintName + " blueprint name request.");
        when(Blueprint.post(), blueprintName + " blueprint name request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 4, groups = "blueprints")
    public void testCreateBlueprintWithIllegalPercentageCharacterInNameShouldThrowBadRequestException() throws Exception {
        given(CloudbreakClient.created());
        String blueprintName = String.format(ILLEGAL_BP_NAME, "%");
        given(Blueprint.request()
                .withName(blueprintName)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()), blueprintName + " blueprint name request.");
        when(Blueprint.post(), blueprintName + " blueprint name request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 5, groups = "blueprints")
    public void testCreateBlueprintWithIllegalSlashCharacterInNameShouldThrowBadRequestException() throws Exception {
        given(CloudbreakClient.created());
        String blueprintName = String.format(ILLEGAL_BP_NAME, "/");
        given(Blueprint.request()
                .withName(blueprintName)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()), blueprintName + " blueprint name request.");
        when(Blueprint.post(), blueprintName + " blueprint name request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 6, groups = "blueprints")
    public void testCreateAgainBlueprint() throws Exception {
        given(CloudbreakClient.created());
        given(Blueprint.isCreated()
                .withName(AGAIN_BP_NAME), AGAIN_BP_NAME + " blueprint is created.");
        given(Blueprint.request()
                .withName(AGAIN_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()), AGAIN_BP_NAME + " blueprint request.");
        when(Blueprint.post(), AGAIN_BP_NAME + " blueprint request has been posted.");
    }

    //"The length of the blueprint's name has to be in range of 1 to 100"
    @Test(expectedExceptions = BadRequestException.class, priority = 7, groups = "blueprints")
    public void testCreateInvalidShortBlueprint() throws Exception {
        given(CloudbreakClient.created());
        given(Blueprint.request()
                .withName(INVALID_SHORT_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()), "0 char blueprint name request.");
        when(Blueprint.post(), "0 char blueprint name request has been posted.");
    }

    //"The length of the blueprint's name has to be in range of 1 to 100"
    @Test(expectedExceptions = BadRequestException.class, priority = 8, groups = "blueprints")
    public void testCreateInvalidLongBlueprint() throws Exception {
        String invalidLongName = longStringGeneratorUtil.stringGenerator(101);

        given(CloudbreakClient.created());
        given(Blueprint.request()
                .withName(invalidLongName)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()), invalidLongName + " blueprint name request.");
        when(Blueprint.post(), invalidLongName + " blueprint name request has been posted.");
    }

    //BUG-95609 - Won't fix issue
    //"The length of the blueprint's description has to be in range of 1 to 1000"
    @Test(expectedExceptions = BadRequestException.class, priority = 9, groups = "blueprints")
    public void testCreateLongDescriptionBlueprint() throws Exception {
        String invalidLongDescripton = longStringGeneratorUtil.stringGenerator(1001);

        given(CloudbreakClient.created());
        given(Blueprint.request()
                .withName(LONG_DC_BP_NAME)
                .withDescription(invalidLongDescripton)
                .withAmbariBlueprint(getBlueprintFile()), LONG_DC_BP_NAME + " blueprint description request.");
        when(Blueprint.post(), LONG_DC_BP_NAME + " blueprint description request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 10, groups = "blueprints")
    public void testCreateEmptyJSONBlueprintException() throws Exception {
        given(CloudbreakClient.created());
        given(Blueprint.request()
                .withName(EMPTY_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getEmptyBlueprintFile()), EMPTY_BP_NAME + " blueprint with no content request.");
        when(Blueprint.post(), EMPTY_BP_NAME + " blueprint with no content request has been posted.");
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 11, groups = "blueprints")
    public void testCreateEmptyFileBlueprintException() throws Exception {
        given(CloudbreakClient.created());
        given(Blueprint.request()
                .withName(EMPTY_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintUrl()), EMPTY_BP_NAME + " blueprint file with no content on URL request.");
        when(Blueprint.post(), EMPTY_BP_NAME + " blueprint file with no content on URL request has been posted.");
    }

    @Test(priority = 12, groups = "blueprints")
    public void testCreateEmptyFileBlueprintMessage() throws Exception {
        given(CloudbreakClient.created());
        given(Blueprint.request()
                .withName(EMPTY_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintUrl()), EMPTY_BP_NAME + " blueprint file with no content on URL request.");
        try {
            when(Blueprint.post(), EMPTY_BP_NAME + " blueprint file with no content on URL request has been posted.");
        } catch (BadRequestException e) {
            try (Response response = e.getResponse()) {
                String exceptionMessage = response.readEntity(String.class);
                errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                LOGGER.info("BadRequestException message ::: " + errorMessage);
            }
        }
        then(Blueprint.assertThis(
                (blueprint, t) -> Assert.assertEquals(errorMessage, " Failed to parse JSON.\"}")),
                "Error message [Failed to parse JSON.] should be present in response."
        );
    }

    @Test(expectedExceptions = BadRequestException.class, priority = 13, groups = "blueprints")
    public void testCreateInvalidURLBlueprintException() throws Exception {
        given(CloudbreakClient.created());
        given(Blueprint.request()
                .withName(INVALIDURL_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintInvalidUrl()), INVALIDURL_BP_NAME + " blueprint with invalid URL request.");
        when(Blueprint.post(), INVALIDURL_BP_NAME + " blueprint with invalid URL request has been posted.");
    }

    @Test(priority = 14, groups = "blueprints")
    public void testCreateInvalidURLBlueprintMessage() throws Exception {
        given(CloudbreakClient.created());
        given(Blueprint.request()
                .withName(INVALIDURL_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintInvalidUrl()), INVALIDURL_BP_NAME + " blueprint with invalid URL request.");
        try {
            when(Blueprint.post(), INVALIDURL_BP_NAME + " blueprint with invalid URL request has been posted.");
        } catch (BadRequestException e) {
            try (Response response = e.getResponse()) {
                String exceptionMessage = response.readEntity(String.class);
                errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                LOGGER.info("BadRequestException message ::: " + errorMessage);
            }
        }
        then(Blueprint.assertThis(
                (blueprint, t) -> Assert.assertEquals(errorMessage, " Failed to parse JSON.\"}")),
                "Error message [Failed to parse JSON.] should be present in response."
        );
    }

    @Test(priority = 15, groups = "blueprints")
    public void testDeleteValidBlueprint() throws Exception {
        given(CloudbreakClient.created());
        given(Blueprint.request()
                .withName(DELETE_BP_NAME), DELETE_BP_NAME + " blueprint delete request.");
        when(Blueprint.delete(), DELETE_BP_NAME + " blueprint delete request has been posted.");
        then(Blueprint.assertThis(
                (blueprint, t) -> Assert.assertNull(blueprint.getResponse())),
                DELETE_BP_NAME + " blueprint should not be present in response."
        );
    }

    @BeforeTest
    public void testCreateDeleteBlueprint() throws Exception {
        given(CloudbreakClient.created());
        given(Blueprint.request()
                .withName(DELETE_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()));
        when(Blueprint.post());
    }

    @AfterTest
    public void cleanUp() throws Exception {
        String[] nameArray = {VALID_BP_NAME, SPECIAL_BP_NAME};

        for (String aNameArray : nameArray) {
            LOGGER.info("Delete blueprint: \'{}\'", aNameArray.toLowerCase().trim());
            try {
                given(CloudbreakClient.created());
                given(Blueprint.request()
                        .withName(aNameArray));
                when(Blueprint.delete());
            } catch (ForbiddenException | BadRequestException e) {
                try (Response response = e.getResponse()) {
                    String exceptionMessage = response.readEntity(String.class);
                    errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                    LOGGER.info("Clean Up Exception message ::: " + errorMessage);
                }
            }
        }
    }

    private String getBlueprintFile() throws IOException {
        return new String(
                StreamUtils.copyToByteArray(
                        applicationContext
                                .getResource("classpath:/blueprint/hdp-multinode-default.bp")
                                .getInputStream()));
    }

    private String getEmptyBlueprintFile() throws IOException {
        return new String(
                StreamUtils.copyToByteArray(
                        applicationContext
                                .getResource("classpath:/blueprint/empty.bp")
                                .getInputStream()));
    }

    private String getBlueprintUrl() {
        return "https://rawgit.com/hortonworks/cloudbreak/master/integration-test/src/main/resources/"
                + "blueprint/multi-node-hdfs-yarn.bp";
    }

    private String getBlueprintInvalidUrl() {
        return "https://index.hu";
    }
}
