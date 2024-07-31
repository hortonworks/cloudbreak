package com.sequenceiq.cloudbreak.controller.v4;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageRecommendationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.RuntimeVersionsV4Response;
import com.sequenceiq.cloudbreak.controller.validation.RecommendedImageValidator;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;

@ExtendWith(MockitoExtension.class)
public class ImageCatalogV4ControllerTest {

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private RecommendedImageValidator recommendedImageValidator;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

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
