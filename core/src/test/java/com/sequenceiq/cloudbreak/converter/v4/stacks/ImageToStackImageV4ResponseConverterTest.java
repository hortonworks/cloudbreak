package com.sequenceiq.cloudbreak.converter.v4.stacks;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ImageToStackImageV4ResponseConverterTest {

    private static final String IMAGE_NAME = "image name";

    private static final String OS = "os";

    private static final String OS_TYPE = "os type";

    private static final String IMAGE_CATALOG_NAME = "image catalog name";

    private static final String IMAGE_ID = "image id";

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private UserService userService;

    @Mock
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @InjectMocks
    private ImageToStackImageV4ResponseConverter victim;
    
    @Test
    public void shouldAllowNullImageCatalogUrlInCaseOfCustomImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        Image image = anImageWithoutImageCatalogUrl();

        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(imageCatalogService.get(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(imageCatalog);

        StackImageV4Response actual = victim.convert(image);
        assertNull(actual.getCatalogUrl());
        assertEquals(IMAGE_CATALOG_NAME, actual.getCatalogName());
        assertEquals(IMAGE_ID, actual.getId());
        assertEquals(IMAGE_NAME, actual.getName());
    }

    private Image anImageWithoutImageCatalogUrl() {
        return new Image(IMAGE_NAME, null, OS, OS_TYPE, null, IMAGE_CATALOG_NAME, IMAGE_ID, null);
    }
}