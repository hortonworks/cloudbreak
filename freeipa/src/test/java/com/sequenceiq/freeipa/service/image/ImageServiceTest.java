package com.sequenceiq.freeipa.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;
import com.sequenceiq.freeipa.converter.image.ImageToImageEntityConverter;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.image.change.action.ImageRevisionReaderService;
import com.sequenceiq.freeipa.repository.ImageRepository;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    private static final String DEFAULT_PLATFORM = "aws";

    private static final String REGION = "eu-west-1";

    private static final String DEFAULT_REGION = "default";

    private static final String EXISTING_ID = "ami-09fea90f257c85513";

    private static final String DEFAULT_REGION_EXISTING_ID = "ami-09fea90f257c85514";

    private static final String FAKE_ID = "fake-ami-0a6931aea1415eb0e";

    private static final String IMAGE_CATALOG = "image catalog";

    private static final String IMAGE_CATALOG_URL = "image catalog url";

    private static final String DEFAULT_OS = "redhat7";

    private static final String IMAGE_UUID = "UUID";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ACCOUNT_ID = "cloudera";

    private static final LocalDateTime MOCK_NOW = LocalDateTime.of(1969, 4, 1, 4, 20);

    private static final String LDAP_AGENT_VERSION = "1.2.3";

    private static final String SOURCE_IMAGE = "source-image";

    private static final String IMDS_VERSION = "v2";

    private static final String SALT_VERSION = "3001.8";

    private static final String GATEWAY_USERDATA = "gateway userdata";

    @Mock
    private ImageProviderFactory imageProviderFactory;

    @Mock
    private ImageProvider imageProvider;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ImageRevisionReaderService imageRevisionReaderService;

    @Mock
    private Clock clock;

    @InjectMocks
    private ImageService underTest;

    @Mock
    private Image image;

    @Mock
    private ImageToImageEntityConverter imageConverter;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private FreeipaPlatformStringTransformer platformStringTransformer;

    @Mock
    private PreferredOsService preferredOsService;

    @Captor
    private ArgumentCaptor<FreeIpaImageFilterSettings> imageFilterSettingsCaptor;

    @Test
    void testDetermineImageNameFound() {
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(DEFAULT_PLATFORM, Collections.singletonMap(REGION, EXISTING_ID)));

        String imageName = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.determineImageName(DEFAULT_PLATFORM, REGION, image));
        assertEquals("ami-09fea90f257c85513", imageName);
    }

    @Test
    void testDetermineImageNameFoundDefaultPreferred() {
        when(entitlementService.azureMarketplaceImagesEnabled(any())).thenReturn(true);
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap("azure",
                Map.of(
                        REGION, EXISTING_ID,
                        DEFAULT_REGION, DEFAULT_REGION_EXISTING_ID)));

        String imageName = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.determineImageName("azure", REGION, image));
        assertEquals("ami-09fea90f257c85514", imageName);
    }

    @Test
    void testDetermineImageNameFoundDefaultMock() {
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap("mock",
                Map.of(DEFAULT_REGION, "mockimage")));

        String imageName = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.determineImageName("mock", "London", image));
        assertEquals("mockimage", imageName);
    }

    @Test
    void testDetermineImageNameFoundNoMpEntitlement() {
        when(entitlementService.azureMarketplaceImagesEnabled(any())).thenReturn(false);
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap("azure",
                Map.of(
                        REGION, EXISTING_ID,
                        DEFAULT_REGION, DEFAULT_REGION_EXISTING_ID)));

        String imageName = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.determineImageName("azure", REGION, image));
        assertEquals("ami-09fea90f257c85513", imageName);
    }

    @Test
    void testDetermineImageNameNotFound() {
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(DEFAULT_PLATFORM, Collections.singletonMap(REGION, EXISTING_ID)));

        Exception exception = assertThrows(RuntimeException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.determineImageName(DEFAULT_PLATFORM, "fake-region", image)));
        String exceptionMessage = "Virtual machine image couldn't be found in image";
        MatcherAssert.assertThat(exception.getMessage(), CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    void testGetImageGivenIdInputNotFound() {
        FreeIpaImageFilterSettings imageSettings = new FreeIpaImageFilterSettings(FAKE_ID, IMAGE_CATALOG, DEFAULT_OS, DEFAULT_OS, REGION, DEFAULT_PLATFORM,
                false, Architecture.X86_64);

        when(imageProviderFactory.getImageProvider(IMAGE_CATALOG)).thenReturn(imageProvider);
        when(imageProvider.getImage(imageSettings)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () ->
                underTest.getImage(imageSettings));
        String exceptionMessage = "Could not find any image with id: 'fake-ami-0a6931aea1415eb0e' in region 'eu-west-1' with OS 'redhat7'.";
        assertEquals(exceptionMessage, exception.getMessage());
    }

    @Test
    void testImageChange() {
        Stack stack = new Stack();
        stack.setCloudPlatform(DEFAULT_PLATFORM);
        stack.setRegion(REGION);
        stack.setAccountId(ACCOUNT_ID);
        ImageSettingsRequest imageRequest = new ImageSettingsRequest();
        when(imageProviderFactory.getImageProvider(any())).thenReturn(imageProvider);
        when(imageProvider.getImage(any())).thenReturn(Optional.of(ImageWrapper.ofFreeipaImage(image, IMAGE_CATALOG_URL)));
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(DEFAULT_PLATFORM, Collections.singletonMap(REGION, EXISTING_ID)));
        when(imageRepository.getByStack(stack)).thenReturn(new ImageEntity());
        when(image.getUuid()).thenReturn(IMAGE_UUID);
        when(image.getOs()).thenReturn("rh8");
        when(image.getOsType()).thenReturn("rhel8");
        when(image.getUuid()).thenReturn(IMAGE_UUID);
        when(imageRepository.save(any(ImageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0, ImageEntity.class));
        when(imageConverter.extractLdapAgentVersion(image)).thenReturn(LDAP_AGENT_VERSION);
        when(imageConverter.extractSourceImage(image)).thenReturn(SOURCE_IMAGE);
        when(imageConverter.extractImdsVersion(image)).thenReturn(IMDS_VERSION);
        when(imageConverter.extractSaltVersion(image)).thenReturn(SALT_VERSION);
        when(platformStringTransformer.getPlatformString(stack)).thenReturn("aws");

        ImageEntity imageEntity = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.changeImage(stack, imageRequest));

        assertEquals(EXISTING_ID, imageEntity.getImageName());
        assertEquals(IMAGE_CATALOG_URL, imageEntity.getImageCatalogUrl());
        assertNull(imageEntity.getImageCatalogName());
        assertEquals(IMAGE_UUID, imageEntity.getImageId());
        assertEquals(LDAP_AGENT_VERSION, imageEntity.getLdapAgentVersion());
        assertEquals(SOURCE_IMAGE, imageEntity.getSourceImage());
        assertEquals("rh8", imageEntity.getOs());
        assertEquals("rhel8", imageEntity.getOsType());
        assertEquals(IMDS_VERSION, imageEntity.getImdsVersion());
        assertEquals(SALT_VERSION, imageEntity.getSaltVersion());
    }

    @Test
    void testImageCreate() {
        Stack stack = new Stack();
        stack.setCloudPlatform(DEFAULT_PLATFORM);
        stack.setRegion(DEFAULT_REGION);
        stack.setAccountId(ACCOUNT_ID);
        ImageSettingsRequest imageRequest = new ImageSettingsRequest();
        when(imageRepository.save(any(ImageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0, ImageEntity.class));
        when(imageConverter.convert(any(), any())).thenReturn(new ImageEntity());

        ImageEntity imageEntity = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.create(stack, Pair.of(ImageWrapper.ofCoreImage(image, IMAGE_CATALOG), EXISTING_ID)));

        assertEquals(stack, imageEntity.getStack());
        assertEquals(EXISTING_ID, imageEntity.getImageName());
        assertNull(imageEntity.getImageCatalogUrl());
        assertEquals(IMAGE_CATALOG, imageEntity.getImageCatalogName());
    }

    @Test
    void testImageFetch() {
        Stack stack = new Stack();
        stack.setCloudPlatform(DEFAULT_PLATFORM);
        stack.setRegion(DEFAULT_REGION);
        stack.setAccountId(ACCOUNT_ID);
        ImageSettingsRequest imageRequest = new ImageSettingsRequest();
        when(imageProviderFactory.getImageProvider(any())).thenReturn(imageProvider);
        when(imageProvider.getImage(any())).thenReturn(Optional.of(ImageWrapper.ofCoreImage(image, IMAGE_CATALOG)));
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(DEFAULT_PLATFORM, Collections.singletonMap(DEFAULT_REGION, EXISTING_ID)));
        when(platformStringTransformer.getPlatformString(stack)).thenReturn("aws");

        Pair<ImageWrapper, String> imageWrapperWithName = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.fetchImageWrapperAndName(stack, imageRequest));

        assertEquals(EXISTING_ID, imageWrapperWithName.getValue());
    }

    @Test
    void testRevert() {
        ImageEntity originalImage = new ImageEntity();
        originalImage.setImageName(EXISTING_ID);
        originalImage.setImageId(IMAGE_UUID);
        originalImage.setImageCatalogName(IMAGE_CATALOG);
        originalImage.setImageCatalogUrl(IMAGE_CATALOG_URL);
        originalImage.setOs(OsType.CENTOS7.getOs());
        originalImage.setOsType(OsType.CENTOS7.getOsType());
        originalImage.setLdapAgentVersion(LDAP_AGENT_VERSION);
        originalImage.setSourceImage(SOURCE_IMAGE);
        originalImage.setImdsVersion(IMDS_VERSION);
        originalImage.setSaltVersion(SALT_VERSION);
        originalImage.setGatewayUserdata(GATEWAY_USERDATA);

        when(imageRevisionReaderService.find(2L, 3L)).thenReturn(originalImage);
        ImageEntity currentImage = new ImageEntity();
        currentImage.setId(2L);
        when(imageRepository.findById(2L)).thenReturn(Optional.of(currentImage));

        underTest.revertImageToRevision("accountId", 2L, 3L);

        ArgumentCaptor<ImageEntity> captor = ArgumentCaptor.forClass(ImageEntity.class);
        verify(imageRepository).save(captor.capture());
        ImageEntity revertedImage = captor.getValue();
        assertEquals(2L, revertedImage.getId());
        assertEquals(IMAGE_UUID, revertedImage.getImageId());
        assertEquals(IMAGE_CATALOG, revertedImage.getImageCatalogName());
        assertEquals(IMAGE_CATALOG_URL, revertedImage.getImageCatalogUrl());
        assertEquals(EXISTING_ID, revertedImage.getImageName());
        assertEquals(OsType.CENTOS7.getOs(), revertedImage.getOs());
        assertEquals(OsType.CENTOS7.getOsType(), revertedImage.getOsType());
        assertEquals(LDAP_AGENT_VERSION, revertedImage.getLdapAgentVersion());
        assertEquals(SOURCE_IMAGE, revertedImage.getSourceImage());
        assertEquals(IMDS_VERSION, revertedImage.getImdsVersion());
        assertEquals(SALT_VERSION, revertedImage.getSaltVersion());
        assertEquals(GATEWAY_USERDATA, revertedImage.getGatewayUserdata());
    }

    @Test
    void testGenerateForStack() {
        Stack stack = new Stack();
        stack.setRegion(REGION);
        stack.setCloudPlatform(DEFAULT_PLATFORM);
        stack.setAccountId("account");
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setImageId(IMAGE_UUID);
        imageEntity.setOs(DEFAULT_OS);
        imageEntity.setImageCatalogName(IMAGE_CATALOG);
        imageEntity.setImageCatalogUrl(IMAGE_CATALOG_URL);
        when(imageRepository.getByStack(stack)).thenReturn(imageEntity);

        when(imageProviderFactory.getImageProvider(IMAGE_CATALOG)).thenReturn(imageProvider);
        Image image = new Image(123L, "now", "desc", DEFAULT_OS, IMAGE_UUID, Map.of(), "os", Map.of(), true, "x86_64", Map.of());
        ImageWrapper imageWrapper = ImageWrapper.ofCoreImage(image, IMAGE_CATALOG);
        when(imageProvider.getImage(any())).thenReturn(Optional.of(imageWrapper));
        when(platformStringTransformer.getPlatformString(stack)).thenReturn("aws");

        ImageCatalog result = underTest.generateImageCatalogForStack(stack);

        verify(imageProvider).getImage(imageFilterSettingsCaptor.capture());
        assertThat(imageFilterSettingsCaptor.getValue())
                .returns(IMAGE_CATALOG, FreeIpaImageFilterSettings::catalog)
                .returns(IMAGE_UUID, FreeIpaImageFilterSettings::currentImageId)
                .returns(REGION, FreeIpaImageFilterSettings::region)
                .returns(DEFAULT_PLATFORM, FreeIpaImageFilterSettings::platform)
                .returns(DEFAULT_OS, FreeIpaImageFilterSettings::currentOs);

        assertThat(result.getImages().getFreeipaImages())
                .containsExactly(image);
        assertThat(result.getVersions()).isNull();
    }

    @Test
    void getImagesOfAliveStacksWithNoThresholdShouldCallRepositoryWithCurrentTimestamp() {
        when(clock.getCurrentLocalDateTime()).thenReturn(MOCK_NOW);

        underTest.getImagesOfAliveStacks(null);

        verify(imageRepository).findImagesOfAliveStacks(Timestamp.valueOf(MOCK_NOW).getTime());
    }

    @Test
    void getImagesOfAliveStacksWithThresholdShouldCallRepositoryWithModifiedTimestamp() {
        final int thresholdInDays = 180;
        final LocalDateTime thresholdTime = MOCK_NOW.minusDays(thresholdInDays);
        when(clock.getCurrentLocalDateTime()).thenReturn(MOCK_NOW);

        underTest.getImagesOfAliveStacks(thresholdInDays);

        verify(imageRepository).findImagesOfAliveStacks(Timestamp.valueOf(thresholdTime).getTime());
    }
}
