package com.sequenceiq.cloudbreak.service.image;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ImageCatalogServiceTest {

    public static final String USER_ID = "userId";

    public static final String USERNAME = "username";

    public static final String ACCOUNT = "account";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private ImageCatalogRepository imageCatalogRepository;

    @Mock
    private AccountPreferencesService accountPreferencesService;

    @InjectMocks
    private ImageCatalogService underTest;

    @Spy
    private List<CloudConstant> constants = new ArrayList<>();

    @Before
    public void beforeTest() throws Exception {
        String catalogJson = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/service/image/cb-image-catalog-v2.json");
        CloudbreakImageCatalogV2 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV2.class);
        when(imageCatalogProvider.getImageCatalogV2("")).thenReturn(catalog);

        IdentityUser user = getIdentityUser();
        when(authenticatedUserService.getCbUser()).thenReturn(user);

        constants.addAll(Collections.singletonList(new CloudConstant() {
            @Override
            public Platform platform() {
                return Platform.platform("AWS");
            }

            @Override
            public Variant variant() {
                return Variant.variant("AWS");
            }
        }));

        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "defaultCatalogUrl", "http://localhost/imagecatalog-url", null);
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "cbVersion", "unspecified", null);
    }

    private IdentityUser getIdentityUser() {
        return new IdentityUser(USER_ID, USERNAME, ACCOUNT,
                Collections.emptyList(), "givenName", "familyName", new Date());
    }

    @Test
    public void testGetImagesWhenExactVersionExistsInCatalog() throws Exception {
        String cbVersion = "1.16.4";
        StatedImages images = underTest.getImages("", "default", "aws", cbVersion);

        boolean exactImageIdMatch = images.getImages().getHdpImages().stream()
                .anyMatch(img -> img.getUuid().equals("2.5.1.9-4-ccbb32dc-6c9f-43f1-8a09-64b598fda733-2.6.1.4-2"));
        Assert.assertTrue("Result doesn't contain the required Ambari image with id.", exactImageIdMatch);
    }

    @Test
    public void testGetImagesWhenExactVersionExistsInCatalogAndMorePlatformRequested() throws Exception {
        String cbVersion = "1.12.0";
        StatedImages images = underTest.getImages("", "default", ImmutableSet.of("aws", "azure"), cbVersion);
        boolean awsAndAzureWerePresentedInTheTest = false;
        Assert.assertEquals(2, images.getImages().getHdpImages().size());
        for (Image image : images.getImages().getHdpImages()) {
            boolean containsAws = images.getImages().getHdpImages().stream()
                    .anyMatch(img -> img.getImageSetsByProvider().entrySet().stream().anyMatch(
                            platformImages -> platformImages.getKey().equals("aws")));
            boolean containsAzure = images.getImages().getHdpImages().stream()
                    .anyMatch(img -> img.getImageSetsByProvider().entrySet().stream().anyMatch(
                            platformImages -> platformImages.getKey().equals("azure_rm")));
            if (image.getImageSetsByProvider().size() == 2) {
                awsAndAzureWerePresentedInTheTest = true;
                Assert.assertTrue("Result doesn't contain the required Ambari image with id.", containsAws && containsAzure);
            } else if (image.getImageSetsByProvider().size() == 1) {
                Assert.assertTrue("Result doesn't contain the required Ambari image with id.", containsAws || containsAzure);

            }
        }
        Assert.assertTrue(awsAndAzureWerePresentedInTheTest);
    }

    @Test
    public void testGetImagesWhenSimilarDevVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        StatedImages images = underTest.getImages("", "default", "aws", "1.16.4-dev.132");

        boolean match = images.getImages().getHdpImages().stream()
                .anyMatch(img -> img.getUuid().equals("2.5.1.9-4-ccbb32dc-6c9f-43f1-8a09-64b598fda733-2.6.1.4-2"));
        Assert.assertTrue("Result doesn't contain the required Ambari image with id.", match);
    }

    @Test
    public void testGetImagesWhenSimilarRcVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        StatedImages images = underTest.getImages("", "default", "aws", "1.16.4-rc.13");

        boolean match = images.getImages().getHdpImages().stream()
                .anyMatch(img -> img.getUuid().equals("2.5.1.9-4-ccbb32dc-6c9f-43f1-8a09-64b598fda733-2.6.1.4-2"));
        Assert.assertTrue("Result doesn't contain the required Ambari image with id.", match);
    }

    @Test
    public void testGetImagesWhenSimilarDevVersionExistsInCatalog() throws Exception {
        StatedImages images = underTest.getImages("", "default", "aws", "2.1.0-dev.4000");

        boolean hdfImgMatch = images.getImages().getHdfImages().stream()
                .anyMatch(ambariImage -> ambariImage.getUuid().equals("9958938a-1261-48e2-aff9-dbcb2cebf6cd"));
        boolean hdpImgMatch = images.getImages().getHdpImages().stream()
                .anyMatch(ambariImage -> ambariImage.getUuid().equals("2.5.0.2-65-5288855d-d7b9-4b90-b326-ab4b168cf581-2.6.0.1-145"));
        boolean baseImgMatch = images.getImages().getBaseImages().stream()
                .anyMatch(ambariImage -> ambariImage.getUuid().equals("f6e778fc-7f17-4535-9021-515351df3691"));
        Assert.assertTrue("Result doesn't contain the required Ambari image with id.", hdfImgMatch && hdpImgMatch && baseImgMatch);
    }

    @Test
    public void testGetImagesWhenSimilarRcVersionExistsInCatalog() throws Exception {
        StatedImages images = underTest.getImages("", "default", "aws", "2.0.0-rc.4");

        boolean allMatch = images.getImages().getHdpImages().stream()
                .allMatch(img -> img.getUuid().equals("2.4.2.2-1-9e3ccdca-fa64-42eb-ab29-b1450767bbd8-2.5.0.1-265")
                        || img.getUuid().equals("2.5.1.9-4-ccbb32dc-6c9f-43f1-8a09-64b598fda733-2.6.1.4-2"));
        Assert.assertTrue("Result doesn't contain the required Ambari image with id.", allMatch);
    }

    @Test
    public void testGetImagesWhenExactVersionExistsInCatalogForPlatform() throws Exception {
        StatedImages images = underTest.getImages("", "default", "AWS", "1.16.4");
        boolean exactImageIdMatch = images.getImages().getHdpImages().stream()
                .anyMatch(img -> img.getUuid().equals("2.5.1.9-4-ccbb32dc-6c9f-43f1-8a09-64b598fda733-2.6.1.4-2"));
        Assert.assertTrue("Result doesn't contain the required Ambari image with id for the platform.", exactImageIdMatch);
    }

    @Test
    public void testGetImagesWhenExactVersionDoesnotExistInCatalogForPlatform() throws Exception {
        StatedImages images = underTest.getImages("", "default", "owncloud", "1.16.4");

        boolean noMatch = images.getImages().getBaseImages().isEmpty()
                && images.getImages().getHdpImages().isEmpty()
                && images.getImages().getHdfImages().isEmpty();
        Assert.assertTrue("Result contains no Ambari Image for the version and platform.", noMatch);
    }

    @Test
    public void testGetImagesWhenCustomImageCatalogExists() throws Exception {
        ImageCatalog ret = new ImageCatalog();
        ret.setImageCatalogUrl("");
        when(imageCatalogRepository.findByName("name", "userId", "account")).thenReturn(ret);
        when(imageCatalogProvider.getImageCatalogV2("")).thenReturn(null);
        underTest.getImages("name", "aws");

        verify(imageCatalogProvider, times(1)).getImageCatalogV2("");

    }

    @Test
    public void testGetImagesWhenCustomImageCatalogDoesNotExists() throws Exception {
        when(imageCatalogRepository.findByName("name", "userId", "account")).thenReturn(null);
        Images images = underTest.getImages("name", "aws").getImages();

        verify(imageCatalogProvider, times(0)).getImageCatalogV2("");

        Assert.assertTrue("Base images should be empty!", images.getBaseImages().isEmpty());
        Assert.assertTrue("HDF images should be empty!", images.getHdfImages().isEmpty());
        Assert.assertTrue("HDP images should be empty!", images.getHdpImages().isEmpty());

    }

    @Test
    public void testDeleteImageCatalog() {
        String name = "img-name";
        IdentityUser user = getIdentityUser();
        UserProfile userProfile = new UserProfile();
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogName(name);
        imageCatalog.setArchived(false);
        when(authenticatedUserService.getCbUser()).thenReturn(user);
        when(imageCatalogRepository.findByName(name, user.getUserId(), user.getAccount())).thenReturn(imageCatalog);
        when(userProfileService.get(user.getAccount(), user.getUserId(), user.getUsername())).thenReturn(userProfile);
        underTest.delete(name);

        verify(imageCatalogRepository, times(1)).save(imageCatalog);

        Assert.assertTrue(imageCatalog.isArchived());
        Assert.assertTrue(imageCatalog.getImageCatalogName().startsWith(name) && imageCatalog.getImageCatalogName().indexOf("_") == name.length());
    }

    @Test
    public void testDeleteImageCatalogWhenEnvDefault() {
        String name = "cloudbreak-default";

        thrown.expectMessage("cloudbreak-default cannot be deleted because it is an environment default image catalog.");
        thrown.expect(BadRequestException.class);

        underTest.delete(name);
    }

    @Test
    public void testGet() {
        String name = "img-name";
        ImageCatalog imageCatalog = new ImageCatalog();
        IdentityUser user = getIdentityUser();
        when(authenticatedUserService.getCbUser()).thenReturn(user);
        when(imageCatalogRepository.findByName(name, user.getUserId(), user.getAccount())).thenReturn(imageCatalog);
        ImageCatalog actual = underTest.get(name);

        Assert.assertEquals(actual, imageCatalog);
    }

    @Test
    public void testGetWhenEnvDefault() {
        String name = "cloudbreak-default";
        ImageCatalog actual = underTest.get(name);

        verify(imageCatalogRepository, times(0)).findByName(name, USER_ID, ACCOUNT);

        Assert.assertEquals(actual.getImageCatalogName(), name);
        Assert.assertNull(actual.getId());
    }
}