package com.sequenceiq.cloudbreak.controller.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.user.UserService;

@ExtendWith(MockitoExtension.class)
public class RecommendImageServiceTest {

    @InjectMocks
    private RecommendImageService recommendImageService;

    @Mock
    private ImageService imageService;

    @Mock
    private UserService userService;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Test
    public void testRecommendImage() throws Exception {
        Long workspaceId = 1L;
        CloudbreakUser user = new CloudbreakUser("testId", "testCrn", "testName", "testEmail", "tenant");
        ImageSettingsV4Request imageSettings = new ImageSettingsV4Request();
        String region = "us-west-1";
        String platform = "AWS";
        String blueprintName = "testBlueprint";
        CloudPlatformVariant cloudPlatform = new CloudPlatformVariant("AWS", "variant");

        Blueprint blueprint = new Blueprint();
        blueprint.setName(blueprintName);
        Set<Blueprint> blueprints = new HashSet<>();
        blueprints.add(blueprint);
        when(blueprintService.findAllByWorkspaceId(anyLong())).thenReturn(blueprints);

        StatedImage statedImage = mock(StatedImage.class);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogImage =
                new com.sequenceiq.cloudbreak.cloud.model.catalog.Image(
                        "date",
                        0L,
                        0L,
                        "desc",
                        "os",
                        "uuid",
                        "ver",
                        Map.of(),
                        Map.of(),
                        null,
                        "type",
                        Map.of(),
                        List.of(),
                        List.of(),
                        "",
                        true,
                        "",
                        "");
        when(imageService.determineImageFromCatalog(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(), any())).thenReturn(statedImage);
        when(statedImage.getImage()).thenReturn(catalogImage);

        Image result = recommendImageService.recommendImage(workspaceId, user, imageSettings, region, platform, blueprintName, cloudPlatform);

        assertEquals(result.getOs(), catalogImage.getOs());
    }

    @Test
    public void testRecommendImageBlueprintNotFound() {
        Long workspaceId = 1L;
        CloudbreakUser user = new CloudbreakUser("testId", "testCrn", "testName", "testEmail", "tenant");
        ImageSettingsV4Request imageSettings = new ImageSettingsV4Request();
        String region = "us-west-1";
        String platform = "AWS";
        String blueprintName = "testBlueprint";
        CloudPlatformVariant cloudPlatform = new CloudPlatformVariant("AWS", "variant");

        Set<Blueprint> blueprints = new HashSet<>();
        when(blueprintService.findAllByWorkspaceId(anyLong())).thenReturn(blueprints);

        assertThrows(BadRequestException.class, () -> {
            recommendImageService.recommendImage(workspaceId, user, imageSettings, region, platform, blueprintName, cloudPlatform);
        });
    }

    @Test
    public void testRecommendImageWithCloudbreakImageNotFoundException() throws Exception {
        Long workspaceId = 1L;
        CloudbreakUser user = new CloudbreakUser("testId", "testCrn", "testName", "testEmail", "tenant");
        ImageSettingsV4Request imageSettings = new ImageSettingsV4Request();
        String region = "us-west-1";
        String platform = "AWS";
        String blueprintName = "testBlueprint";
        CloudPlatformVariant cloudPlatform = new CloudPlatformVariant("AWS", "variant");

        Blueprint blueprint = new Blueprint();
        blueprint.setName(blueprintName);
        Set<Blueprint> blueprints = new HashSet<>();
        blueprints.add(blueprint);
        when(blueprintService.findAllByWorkspaceId(anyLong())).thenReturn(blueprints);

        when(imageService.determineImageFromCatalog(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(), any()))
                .thenThrow(new CloudbreakImageNotFoundException("Image not found"));

        assertThrows(BadRequestException.class, () -> {
            recommendImageService.recommendImage(workspaceId, user, imageSettings, region, platform, blueprintName, cloudPlatform);
        });
    }

    @Test
    public void testRecommendImageWithCloudbreakImageCatalogException() throws Exception {
        Long workspaceId = 1L;
        CloudbreakUser user = new CloudbreakUser("testId", "testCrn", "testName", "testEmail", "tenant");
        ImageSettingsV4Request imageSettings = new ImageSettingsV4Request();
        String region = "us-west-1";
        String platform = "AWS";
        String blueprintName = "testBlueprint";
        CloudPlatformVariant cloudPlatform = new CloudPlatformVariant("AWS", "variant");

        Blueprint blueprint = new Blueprint();
        blueprint.setName(blueprintName);
        Set<Blueprint> blueprints = new HashSet<>();
        blueprints.add(blueprint);
        when(blueprintService.findAllByWorkspaceId(anyLong())).thenReturn(blueprints);

        when(imageService.determineImageFromCatalog(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(), any()))
                .thenThrow(new CloudbreakImageCatalogException("Catalog exception"));

        assertThrows(BadRequestException.class, () -> {
            recommendImageService.recommendImage(workspaceId, user, imageSettings, region, platform, blueprintName, cloudPlatform);
        });
    }

}
