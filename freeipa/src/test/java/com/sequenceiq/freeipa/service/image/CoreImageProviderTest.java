package com.sequenceiq.freeipa.service.image;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.freeipa.api.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CoreImageProviderTest {

    private static final long WORKSPACE_ID_DEFAULT = 0L;

    private static final String CATALOG_NAME = "catalog name";

    private static final String IMAGE_ID = "image id";

    private static final String REGION = "region";

    private static final String PLATFORM = "AWS";

    private static final String DATE = "2021-04-09";

    private static final String DESCRIPTION = "description";

    private static final String UUID = "uuid";

    private static final String OS_TYPE = "os type";

    private static final String VM_IMAGE_REFERENCE = "vm image";

    private static final Map<String, Map<String, String>> VM_IMAGES = Collections.singletonMap(PLATFORM, Collections.singletonMap(REGION, VM_IMAGE_REFERENCE));

    @Mock
    private ImageCatalogV4Endpoint imageCatalogV4Endpoint;

    @InjectMocks
    private CoreImageProvider victim;

    @Test
    public void shouldReturnEmptyInCaseOfException() throws Exception {
        when(imageCatalogV4Endpoint.getSingleImageByCatalogNameAndImageId(WORKSPACE_ID_DEFAULT, CATALOG_NAME, IMAGE_ID)).thenThrow(new RuntimeException());

        Optional<ImageWrapper> actual = victim.getImage(anImageSettings(), REGION, PLATFORM);

        assertTrue(actual.isEmpty());
    }

    @Test
    public void shouldReturnEmptyInCaseOfNullResponse() throws Exception {
        when(imageCatalogV4Endpoint.getSingleImageByCatalogNameAndImageId(WORKSPACE_ID_DEFAULT, CATALOG_NAME, IMAGE_ID)).thenReturn(null);

        Optional<ImageWrapper> actual = victim.getImage(anImageSettings(), REGION, PLATFORM);

        assertTrue(actual.isEmpty());
    }

    @Test
    public void shouldReturnResult() throws Exception {
        when(imageCatalogV4Endpoint.getSingleImageByCatalogNameAndImageId(WORKSPACE_ID_DEFAULT, CATALOG_NAME, IMAGE_ID)).thenReturn(anImageResponse());

        Optional<ImageWrapper> actual = victim.getImage(anImageSettings(), REGION, PLATFORM);
        Image image = actual.get().getImage();

        assertEquals(DATE , image.getDate());
        assertEquals(DESCRIPTION , image.getDescription());
        assertEquals(UUID, image.getUuid());
        assertEquals(OS_TYPE, image.getOsType());
        assertEquals(VM_IMAGE_REFERENCE, image.getImageSetsByProvider().get(PLATFORM).get(REGION));
    }

    private ImageSettingsRequest anImageSettings() {
        ImageSettingsRequest imageSettings = new ImageSettingsRequest();
        imageSettings.setCatalog(CATALOG_NAME);
        imageSettings.setId(IMAGE_ID);

        return imageSettings;
    }

    private ImageV4Response anImageResponse() {
        ImageV4Response response = new ImageV4Response();
        response.setDate(DATE);
        response.setDescription(DESCRIPTION);
        response.setUuid(UUID);
        response.setOsType(OS_TYPE);
        response.setImageSetsByProvider(VM_IMAGES);

        return response;
    }
}