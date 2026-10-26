package com.sequenceiq.cloudbreak.controller.v4;


import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageRecommendationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.RuntimeVersionsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.controller.validation.RecommendedImageValidator;
import com.sequenceiq.cloudbreak.converter.v4.imagecatalog.ImageToImageV4ResponseConverter;
import com.sequenceiq.cloudbreak.service.image.DefaultImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@ExtendWith(MockitoExtension.class)
public class ImageCatalogV4ControllerTest {

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private RecommendedImageValidator recommendedImageValidator;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private DefaultImageCatalogService defaultImageCatalogService;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private ImageToImageV4ResponseConverter imageToImageV4ResponseConverter;

    @InjectMocks
    private ImageCatalogV4Controller victim;

    @Test
    public void testGetRuntimeVersionsFromDefault() throws Exception {
        List<String> expected = List.of("7.2.1", "7.2.2");
        when(imageCatalogService.getRuntimeVersionsFromDefault()).thenReturn(expected);

        RuntimeVersionsV4Response actual = victim.getRuntimeVersionsFromDefault(WORKSPACE_ID);

        assertEquals(expected, actual.getRuntimeVersions());
    }

    @Test
    void testGetFreeIpaImagesFromDefault() throws Exception {
        ImageCatalogPlatform aws = imageCatalogPlatform("AWS");
        Image image = mock(Image.class);
        ImageV4Response imageResponse = new ImageV4Response();
        when(platformStringTransformer.getPlatformStringForImageCatalog("aws", false)).thenReturn(aws);
        when(defaultImageCatalogService.getFreeIpaImagesFromDefaultCatalog(Optional.of(aws), Optional.of(Architecture.ARM64)))
                .thenReturn(List.of(image));
        when(imageToImageV4ResponseConverter.convert(image)).thenReturn(imageResponse);

        ImagesV4Response response = victim.getFreeIpaImagesFromDefault(WORKSPACE_ID, "aws", "arm64");

        assertEquals(List.of(imageResponse), response.getFreeipaImages());
        assertTrue(response.getBaseImages().isEmpty());
        assertTrue(response.getCdhImages().isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void testGetFreeIpaImagesFromDefaultWithoutFilters(String filter) throws Exception {
        when(defaultImageCatalogService.getFreeIpaImagesFromDefaultCatalog(Optional.empty(), Optional.empty())).thenReturn(List.of());

        ImagesV4Response response = victim.getFreeIpaImagesFromDefault(WORKSPACE_ID, filter, filter);

        assertTrue(response.getFreeipaImages().isEmpty());
        verifyNoInteractions(platformStringTransformer);
    }

    @Test
    void testValidateRecommendedImageWithProvider() {
        Long workspaceId = 1L;
        ImageRecommendationV4Request request = new ImageRecommendationV4Request();
        when(recommendedImageValidator.validateRecommendedImage(anyLong(), any(), any()))
                .thenReturn(new RecommendedImageValidator.ValidationResult());

        ImageRecommendationV4Response response = victim.validateRecommendedImageWithProvider(workspaceId, request);

        verify(recommendedImageValidator).validateRecommendedImage(eq(workspaceId), any(), any());
        verify(restRequestThreadLocalService).getCloudbreakUser();

        assertNotNull(response);
        assertFalse(response.hasValidationError());
        assertNull(response.getValidationMessage());
    }

    @Test
    void testValidateRecommendedImageWithProviderValidationError() {
        Long workspaceId = 1L;
        ImageRecommendationV4Request request = new ImageRecommendationV4Request();
        RecommendedImageValidator.ValidationResult validationResult = new RecommendedImageValidator.ValidationResult();
        validationResult.setErrorMsg("Validation error message");
        when(recommendedImageValidator.validateRecommendedImage(anyLong(), any(), any()))
                .thenReturn(validationResult);

        ImageRecommendationV4Response response = victim.validateRecommendedImageWithProvider(workspaceId, request);

        verify(recommendedImageValidator).validateRecommendedImage(eq(workspaceId), any(), any());
        verify(restRequestThreadLocalService).getCloudbreakUser();

        assertNotNull(response);
        assertTrue(response.hasValidationError());
        assertEquals("Validation error message", response.getValidationMessage());
    }
}
