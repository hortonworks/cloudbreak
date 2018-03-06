package com.sequenceiq.it.cloudbreak;

import java.io.IOException;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Blueprint;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;

public class BlueprintTests extends CloudbreakTest {

    public static final String VALID_BP_NAME = "valid-blueprint";

    public static final String AGAIN_BP_NAME = "again-blueprint";

    public static final String LONG_DC_BP_NAME = "long-description-blueprint";

    public static final String DELETE_BP_NAME = "delete-blueprint";

    public static final String SPECIAL_BP_NAME = "@#$%|:&*;";

    public static final String INVALID_SHORT_BP_NAME = "";

    public static final String INVALID_LONG_BP_NAME = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    public static final String EMPTY_BP_NAME = "temp-empty-blueprint";

    public static final String INVALIDURL_BP_NAME = "temp-url-blueprint";

    public static final String BP_DESCRIPTION = "temporary blueprint for API E2E tests";

    public static final String INVALID_LONG_DESCRIPTION = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintTests.class);

    private String errorMessage = "";

    @Test
    public void testCreateValidBlueprint() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(VALID_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()));
        when(Blueprint.post());
        then(Blueprint.assertThis(
                (blueprint, t) -> {
                    Assert.assertEquals(blueprint.getResponse().getName(), VALID_BP_NAME);
                })
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateAgainBlueprint() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.isCreated()
                .withName(AGAIN_BP_NAME));
        given(Blueprint.request()
                .withName(AGAIN_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()));
        when(Blueprint.post());
    }

    //"The length of the blueprint's name has to be in range of 1 to 100"
    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateInvalidShortBlueprint() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(INVALID_SHORT_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()));
        when(Blueprint.post());
    }

    //"The length of the blueprint's name has to be in range of 1 to 100"
    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateInvalidLongBlueprint() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(INVALID_LONG_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()));
        when(Blueprint.post());
    }

    //"The length of the blueprint's name has to be in range of 1 to 100"
    //BUG-95607
    @Test(expectedExceptions = BadRequestException.class, enabled = false)
    public void testCreateSpecialCharacterBlueprint() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(SPECIAL_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()));
        when(Blueprint.post());
    }

    //BUG-95609 - Won't fix issue
    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateLongDescriptionBlueprint() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(LONG_DC_BP_NAME)
                .withDescription(INVALID_LONG_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()));
        when(Blueprint.post());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateEmptyJSONBlueprintException() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(EMPTY_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getEmptyBlueprintFile()));
        when(Blueprint.post());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateEmptyFileBlueprintException() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(EMPTY_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintUrl()));
        when(Blueprint.post());
    }

    @Test
    public void testCreateEmptyJSONBlueprintMessage() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(EMPTY_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintUrl()));
        try {
            when(Blueprint.post());
        } catch (BadRequestException e) {
            String exceptionMessage = e.getResponse().readEntity(String.class);
            this.errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(":") + 1);
            LOGGER.info("BadRequestException message ::: " + this.errorMessage);
        }
        then(Blueprint.assertThis(
                (blueprint, t) -> {
                    Assert.assertEquals(this.errorMessage, " Failed to parse JSON.\"}");
                })
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateInvalidURLBlueprintException() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(INVALIDURL_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintInvalidUrl()));
        when(Blueprint.post());
    }

    @Test
    public void testCreateInvalidURLBlueprintMessage() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(INVALIDURL_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintInvalidUrl()));
        try {
            when(Blueprint.post());
        } catch (BadRequestException e) {
            String exceptionMessage = e.getResponse().readEntity(String.class);
            this.errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(":") + 1);
            LOGGER.info("BadRequestException message ::: " + this.errorMessage);
        }
        then(Blueprint.assertThis(
                (blueprint, t) -> {
                    Assert.assertEquals(this.errorMessage, " Failed to parse JSON.\"}");
                })
        );
    }

    @Test
    public void testDeleteValidBlueprint() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(DELETE_BP_NAME));
        when(Blueprint.delete());
        then(Blueprint.assertThis(
                (blueprint, t) -> {
                    Assert.assertNull(blueprint.getResponse());
                })
        );
    }

    @BeforeTest
    public void testCreateDeleteBlueprint() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(DELETE_BP_NAME)
                .withDescription(BP_DESCRIPTION)
                .withAmbariBlueprint(getBlueprintFile()));
        when(Blueprint.post());
    }

    @AfterSuite
    public void cleanUp() throws Exception {
        given(CloudbreakClient.isCreated());
        given(Blueprint.request()
                .withName(VALID_BP_NAME));
        when(Blueprint.delete());
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
        return "https://gist.githubusercontent.com/aszegedi/852bc5cea158e3cd0eec88415580321b/"
                + "raw/717daa56694ed544b58ff7ba8ca426144db4e518/empty.bp";
    }

    private String getBlueprintInvalidUrl() {
        return "https://index.hu";
    }
}
