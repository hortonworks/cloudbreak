package com.sequenceiq.it.cloudbreak;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalog;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class ImageCatalogTests extends CloudbreakTest {
    private static final String VALID_IMAGECATALOG_NAME = "valid-imagecat";

    private static final String INVALID_IMAGECATALOG_NAME_SHORT = "test";

    private static final String SPECIAL_IMAGECATALOG_NAME = "@#$%|:&*;";

    private static final String VALID_IMAGECATALOG_URL = "https://cloudbreak-imagecatalog.s3.amazonaws.com/v2-dev-cb-image-catalog.json";

    private static final String INVALID_IMAGECATALOG_URL = "google.com";

    private static final String INVALID_IMAGECATALOG_JSON = "https://gist.githubusercontent.com/mhalmy/a206484148d0cb02085bfbd8a58af97f/raw/f9c0a0fea59a67e1e7a"
    + "80f5e7b6defd97a69f26f/imagecatalog_invalid.json";

    private static final String DEFAULT_IMAGECATALOG_NAME = "cloudbreak-default";

    private static final String[] IMAGECATALOG_NAMES = {VALID_IMAGECATALOG_NAME, VALID_IMAGECATALOG_NAME + "-default", VALID_IMAGECATALOG_NAME + "-old",
            VALID_IMAGECATALOG_NAME + "-new"};

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @Test
    public void testCreateValidImageCatalog() throws Exception {
        given(CloudbreakClient.isCreated());
        given(ImageCatalog.request()
                .withName(VALID_IMAGECATALOG_NAME)
                .withUrl(VALID_IMAGECATALOG_URL),  "an imagecatalog request"
        );
        when(ImageCatalog.post(), "post the imagecatalog request");
        then(ImageCatalog.assertThis(
                (imageCatalog, t) -> {
                    Assert.assertEquals(imageCatalog.getResponse().getName(), VALID_IMAGECATALOG_NAME);
                }), "check imagecatalog is created");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateInvalidImageCatalogShortName() throws Exception {
        given(CloudbreakClient.isCreated());
        given(ImageCatalog.request()
                .withName(INVALID_IMAGECATALOG_NAME_SHORT)
                .withUrl(VALID_IMAGECATALOG_URL), "an imagecatalog request with short name"
        );
        checkNameNotAssertEquals(INVALID_IMAGECATALOG_NAME_SHORT,  "short name");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateInvalidImageCatalogLongName() throws Exception {
        given(CloudbreakClient.isCreated());
        given(ImageCatalog.request()
                .withName(longStringGeneratorUtil.stringGenerator(101))
                .withUrl(VALID_IMAGECATALOG_URL), "an imagecatalog request with long name"
        );
        checkNameNotAssertEquals(longStringGeneratorUtil.stringGenerator(101),  "long name");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateInvalidImageCatalogSpecName() throws Exception {
        given(CloudbreakClient.isCreated());
        given(ImageCatalog.request()
                .withName(SPECIAL_IMAGECATALOG_NAME)
                .withUrl(VALID_IMAGECATALOG_URL), "an imagecatalog request with special name"
        );
        checkNameNotAssertEquals(SPECIAL_IMAGECATALOG_NAME,  "special name");
    }

    // BUG-97072
    @Test(expectedExceptions = BadRequestException.class, enabled = false)
    public void testCreateInvalidImageCatalogUrl() throws Exception {
        given(CloudbreakClient.isCreated());
        given(ImageCatalog.request()
                .withName(VALID_IMAGECATALOG_NAME + "-url")
                .withUrl("https://" + INVALID_IMAGECATALOG_URL), "an imagecatalog request with invalid url"
        );
        checkNameNotAssertEquals(VALID_IMAGECATALOG_NAME + "-url",  "invalid url");
    }

    // BUG-97072
    @Test(expectedExceptions = BadRequestException.class, enabled = false)
    public void testCreateInvalidImageCatalogUrlInvalidJson() throws Exception {
        given(CloudbreakClient.isCreated());
        given(ImageCatalog.request()
                .withName(VALID_IMAGECATALOG_NAME + "-url-invalid-json")
                .withUrl(INVALID_IMAGECATALOG_JSON), "an imagecatalog request with url invalid json"
        );
        checkNameNotAssertEquals(VALID_IMAGECATALOG_NAME + "-url-invalid-json",  "url invalid json");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateInvalidImageCatalogUrlNoProtocol() throws Exception {
        given(CloudbreakClient.isCreated());
        given(ImageCatalog.request()
                .withName(VALID_IMAGECATALOG_NAME + "-url-np")
                .withUrl(INVALID_IMAGECATALOG_URL), "an imagecatalog request with url no protocol"
        );
        checkNameNotAssertEquals(VALID_IMAGECATALOG_NAME + "-url-np",  "with url no protocol");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateDeleteValidCreateAgain() throws Exception {
        given(CloudbreakClient.isCreated());
        given(ImageCatalog.isCreatedDeleted()
                .withName(VALID_IMAGECATALOG_NAME + "-delete-create")
                .withUrl(VALID_IMAGECATALOG_URL), "an imagecatalog request then delete"
        );
        given(ImageCatalog.request()
                .withName(VALID_IMAGECATALOG_NAME + "-delete-create")
                .withUrl(INVALID_IMAGECATALOG_URL), "same imagecatalog request again"
        );
        when(ImageCatalog.post(), "post the imagecatalog request");
        then(ImageCatalog.assertThis(
                (imageCatalog, t) -> {
                    Assert.assertEquals(imageCatalog.getResponse().getName(), VALID_IMAGECATALOG_NAME + "-delete-create");
                }),  "check imagecatalog is created when name was used and deleted before");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testDeleteCbDefaultImageCatalog() throws Exception {
        given(CloudbreakClient.isCreated());
        given(ImageCatalog.request()
                .withName(DEFAULT_IMAGECATALOG_NAME)
        );
        when(ImageCatalog.delete(), "try to delete cb default image catalog");
    }

    @Test
    public void testSetNewDefaultImageCatalog() throws Exception {
        given(CloudbreakClient.isCreated());
        given(ImageCatalog.request()
                .withName(VALID_IMAGECATALOG_NAME + "-default")
                .withUrl(VALID_IMAGECATALOG_URL), "an imagecatalog request and set as default"

        );
        when(ImageCatalog.post());
        when(ImageCatalog.setDefault());
        then(ImageCatalog.assertThis(
                (imageCatalog, t) -> {
                    Assert.assertEquals(imageCatalog.getResponse().getName(), VALID_IMAGECATALOG_NAME + "-default");
                    Assert.assertEquals(imageCatalog.getResponse().isUsedAsDefault(), true);
                }),  "check imagecatalog is created and set as default");
    }

    @Test
    public void testSetImageCatalogBackAsNotDefault() throws  Exception {
        given(CloudbreakClient.isCreated());

        given(ImageCatalog.isCreatedAsDefault()
                .withName(VALID_IMAGECATALOG_NAME + "-old")
                .withUrl(VALID_IMAGECATALOG_URL)
        );

        given(ImageCatalog.request()
                .withName(VALID_IMAGECATALOG_NAME + "-new")
                .withUrl(VALID_IMAGECATALOG_URL)
        );
        when(ImageCatalog.post());
        when(ImageCatalog.setDefault());
        when(ImageCatalog.getAll());
        then(ImageCatalog.assertThis(
                (imageCatalog, t) -> {
                    Set<ImageCatalogResponse> imageCatalogResponses = imageCatalog.getResponses();
                    for (ImageCatalogResponse response : imageCatalogResponses) {
                        if (response.getName().equals(VALID_IMAGECATALOG_NAME + "-old")) {
                            Assert.assertEquals(response.isUsedAsDefault(), false);
                        }
                        if (response.getName().equals(VALID_IMAGECATALOG_NAME + "new")) {
                            Assert.assertEquals(response.isUsedAsDefault(), true);
                        }
                    }
                })
        );
    }

    @AfterSuite
    public void cleanAll() throws Exception {
        for (String name : IMAGECATALOG_NAMES) {
            given(CloudbreakClient.isCreated());
            given(ImageCatalog.request()
                    .withName(name)
            );
            when(ImageCatalog.delete());
        }
    }

    @AfterSuite
    public void setDefaults() throws Exception {
        given(CloudbreakClient.isCreated());
        given(ImageCatalog.request()
                .withName(DEFAULT_IMAGECATALOG_NAME)
        );
        when(ImageCatalog.setDefault());
        then(ImageCatalog.assertThis(
                (imageCatalog, t) -> {
                    Assert.assertEquals(imageCatalog.getResponse().getName(), DEFAULT_IMAGECATALOG_NAME);
                    Assert.assertEquals(imageCatalog.getResponse().isUsedAsDefault(), true);

                }),  "check default imagecatalog is set back");
    }

    private void checkNameNotAssertEquals(String a, String b) throws Exception {
        when(ImageCatalog.post(), "post the imagecatalog request");
        then(ImageCatalog.assertThis(
                (imageCatalog, t) -> {
                    Assert.assertNotEquals(imageCatalog.getResponse().getName(), a);
                }),  "check imagecatalog is not created with " + b);
    }
}