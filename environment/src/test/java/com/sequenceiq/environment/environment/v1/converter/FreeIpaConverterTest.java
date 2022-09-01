package com.sequenceiq.environment.environment.v1.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.FreeIpaImageRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.FreeIpaResponse;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaInstanceCountByGroupProvider;

@ExtendWith(MockitoExtension.class)
public class FreeIpaConverterTest {

    private static final String IMAGE_CATALOG = "image catalog";

    private static final String IMAGE_ID = "image id";

    private static final String INSTANCE_TYPE = "instance type";

    private static final int FREE_IPA_INSTANCE_COUNT_BY_GROUP = 2;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private FreeIpaInstanceCountByGroupProvider ipaInstanceCountByGroupProvider;

    @InjectMocks
    private FreeIpaConverter underTest;

    @Test
    public void testConvertWithNull() {
        // GIVEN
        FreeIpaCreationDto request = null;
        // WHEN
        FreeIpaResponse result = underTest.convert(request);
        // THEN
        assertNull(result);
    }

    @Test
    public void testConvertWithDefaults() {
        // GIVEN
        FreeIpaCreationDto request = FreeIpaCreationDto.builder(FREE_IPA_INSTANCE_COUNT_BY_GROUP).build();
        // WHEN
        FreeIpaResponse result = underTest.convert(request);
        // THEN
        assertNotNull(result);
        assertNotNull(result.getInstanceCountByGroup());
        assertEquals(FREE_IPA_INSTANCE_COUNT_BY_GROUP, result.getInstanceCountByGroup());
        assertNull(result.getAws());
        assertNull(result.getImage());
    }

    @Test
    public void testConvertWithFreeIpaImageCatalogAndId() {
        // GIVEN
        FreeIpaCreationDto request = FreeIpaCreationDto.builder(FREE_IPA_INSTANCE_COUNT_BY_GROUP)
                .withImageId(IMAGE_ID)
                .withImageCatalog(IMAGE_CATALOG)
                .build();
        // WHEN
        FreeIpaResponse result = underTest.convert(request);
        // THEN
        assertEquals(IMAGE_ID, result.getImage().getId());
        assertEquals(IMAGE_CATALOG, result.getImage().getCatalog());
    }

    @Test
    public void testConvertWithFreeIpaImageCatalogButWithoutImageId() {
        // GIVEN
        FreeIpaCreationDto request = FreeIpaCreationDto.builder(FREE_IPA_INSTANCE_COUNT_BY_GROUP)
                .withImageCatalog(IMAGE_CATALOG)
                .build();
        // WHEN
        FreeIpaResponse result = underTest.convert(request);
        // THEN
        assertNull(result.getImage());
    }

    @Test
    public void testConvertWithFreeIpaImageIdButWithoutImageCatalog() {
        // GIVEN
        FreeIpaCreationDto request = FreeIpaCreationDto.builder(FREE_IPA_INSTANCE_COUNT_BY_GROUP)
                .withImageId(IMAGE_ID)
                .build();
        // WHEN
        FreeIpaResponse result = underTest.convert(request);
        // THEN
        assertNull(result.getImage());
    }

    @Test
    public void testConvertWithTwoInstancesAndOnlySpotInstances() {
        // GIVEN
        FreeIpaCreationDto request = FreeIpaCreationDto.builder(FREE_IPA_INSTANCE_COUNT_BY_GROUP)
                .withAws(FreeIpaCreationAwsParametersDto.builder()
                        .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                                .withPercentage(100)
                                .withMaxPrice(0.9)
                                .build())
                        .build())
                .build();
        // WHEN
        FreeIpaResponse result = underTest.convert(request);
        // THEN
        assertNotNull(result);
        assertNotNull(result.getInstanceCountByGroup());
        assertEquals(FREE_IPA_INSTANCE_COUNT_BY_GROUP, result.getInstanceCountByGroup());
        assertEquals(100, result.getAws().getSpot().getPercentage());
        assertEquals(0.9, result.getAws().getSpot().getMaxPrice());
    }

    @Test
    public void testConvertWithImage() {
        // GIVEN
        AttachedFreeIpaRequest request = new AttachedFreeIpaRequest();
        request.setCreate(true);
        request.setImage(aFreeIpaImage(IMAGE_CATALOG, IMAGE_ID));
        request.setEnableMultiAz(true);
        // WHEN
        when(entitlementService.awsNativeFreeIpaEnabled(anyString())).thenReturn(true);
        FreeIpaCreationDto result = underTest.convert(request, "id", CloudConstants.AWS);
        // THEN
        verify(ipaInstanceCountByGroupProvider).getInstanceCount(any());
        assertEquals(IMAGE_CATALOG, result.getImageCatalog());
        assertEquals(IMAGE_ID, result.getImageId());
    }

    @Test
    public void testConvertWithoutImage() {
        // GIVEN
        AttachedFreeIpaRequest request = new AttachedFreeIpaRequest();
        request.setCreate(true);
        request.setImage(null);
        // WHEN
        FreeIpaCreationDto result = underTest.convert(request, "id", CloudConstants.AWS);
        // THEN
        verify(ipaInstanceCountByGroupProvider).getInstanceCount(any());
        assertNull(result.getImageCatalog());
        assertNull(result.getImageId());
    }

    @Test
    public void testConvertWithoutImageIdAndImageCatalog() {
        // GIVEN
        AttachedFreeIpaRequest request = new AttachedFreeIpaRequest();
        request.setCreate(true);
        request.setImage(aFreeIpaImage(null, null));
        // WHEN
        FreeIpaCreationDto result = underTest.convert(request, "id", CloudConstants.AWS);
        // THEN
        verify(ipaInstanceCountByGroupProvider).getInstanceCount(any());
        assertNull(result.getImageCatalog());
        assertNull(result.getImageId());
    }

    @Test
    public void testConvertWithInstanceType() {
        // GIVEN
        AttachedFreeIpaRequest request = new AttachedFreeIpaRequest();
        request.setCreate(true);
        request.setInstanceType(INSTANCE_TYPE);
        // WHEN
        FreeIpaCreationDto result = underTest.convert(request, "id", CloudConstants.AWS);
        // THEN
        verify(ipaInstanceCountByGroupProvider).getInstanceCount(any());
        assertNotNull(result.getInstanceType());
        assertEquals(INSTANCE_TYPE, result.getInstanceType());
    }

    @Test
    public void testConvertWithRecipes() {
        // GIVEN
        AttachedFreeIpaRequest request = new AttachedFreeIpaRequest();
        request.setCreate(true);
        request.setInstanceType(INSTANCE_TYPE);
        request.setRecipes(Set.of("recipe1", "recipe2"));
        // WHEN
        FreeIpaCreationDto result = underTest.convert(request, "id", CloudConstants.AWS);
        // THEN
        verify(ipaInstanceCountByGroupProvider).getInstanceCount(any());
        assertNotNull(result.getRecipes());
        assertThat(result.getRecipes()).containsExactlyInAnyOrder("recipe1", "recipe2");
    }

    private FreeIpaImageRequest aFreeIpaImage(String catalog, String id) {
        FreeIpaImageRequest request = new FreeIpaImageRequest();
        request.setId(id);
        request.setCatalog(catalog);

        return  request;
    }
}