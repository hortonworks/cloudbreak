package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeCandidateFilterServiceTest {

    private static final String IMAGE_ID = "image-id-first";

    private static final String IMAGE_ID_LAST = "image-id-last";

    private static final String V_7_0_3 = "7.0.3";

    private static final String V_7_0_2 = "7.0.2";

    private static final String CURRENT_IMAGE_ID = "16ad7759-83b1-42aa-aadf-0e3a6e7b5444";

    private static final String ANOTHER_IMAGE_ID = "another-image-id";

    private static final String MATCHING_TARGET_RUNTIME = "7.0.2";

    private static final String ANOTHER_TARGET_RUNTIME = "7.2.0";

    @InjectMocks
    private ClusterUpgradeCandidateFilterService underTest;

    @Test
    public void testFilterUpgradeOptionsUpgradeRequestEmpty() {
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2L);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));

        underTest.filterUpgradeOptions(response, request, true);

        assertEquals(1, response.getUpgradeCandidates().size());
        assertEquals(IMAGE_ID_LAST, response.getUpgradeCandidates().get(0).getImageId());
    }

    @Test
    public void testFilterUpgradeOptionsImageIdValid() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setImageId(IMAGE_ID);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2L);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        ImageInfoV4Response currentImageInfo = new ImageInfoV4Response();
        currentImageInfo.setImageId(CURRENT_IMAGE_ID);
        response.setCurrent(currentImageInfo);

        underTest.filterUpgradeOptions(response, request, true);

        assertEquals(1, response.getUpgradeCandidates().size());
        assertEquals(IMAGE_ID, response.getUpgradeCandidates().get(0).getImageId());
    }

    @Test
    public void testFilterUpgradeOptionsImageIdInvalid() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setImageId(ANOTHER_IMAGE_ID);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2L);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        ImageInfoV4Response currentImageInfo = new ImageInfoV4Response();
        currentImageInfo.setImageId(CURRENT_IMAGE_ID);
        response.setCurrent(currentImageInfo);

        Exception e = assertThrows(BadRequestException.class, () -> underTest.filterUpgradeOptions(response, request, true));
        assertEquals("The given image (another-image-id) is not eligible for the cluster upgrade. "
                + "Please choose an id from the following image(s): image-id-first,image-id-last", e.getMessage());
    }

    @Test
    public void testFilterUpgradeOptionsImageIdIsValidWhenTheCurrentImageIsSentInTheRequest() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setImageId(CURRENT_IMAGE_ID);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2L);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response currentImageInfo = new ImageInfoV4Response();
        currentImageInfo.setImageId(CURRENT_IMAGE_ID);
        currentImageInfo.setCreated(1L);
        currentImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo, currentImageInfo));
        response.setCurrent(currentImageInfo);

        underTest.filterUpgradeOptions(response, request, true);

        assertEquals(1, response.getUpgradeCandidates().size());
        assertEquals(1, response.getUpgradeCandidates().stream().map(ImageInfoV4Response::getImageId).collect(Collectors.toSet()).size());
        assertTrue(response.getUpgradeCandidates().contains(currentImageInfo));
    }

    @Test
    public void testFilterUpgradeOptionsRuntimeValid() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setRuntime(MATCHING_TARGET_RUNTIME);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2L);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));

        underTest.filterUpgradeOptions(response, request, true);

        assertEquals(2, response.getUpgradeCandidates().size());
        assertEquals(2, response.getUpgradeCandidates().stream().map(ImageInfoV4Response::getImageId).collect(Collectors.toSet()).size());
    }

    @Test
    public void testFilterUpgradeOptionsRuntimeInvalid() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setRuntime(ANOTHER_TARGET_RUNTIME);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2L);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));

        Exception e = assertThrows(BadRequestException.class, () -> underTest.filterUpgradeOptions(response, request, true));
        assertEquals("There is no image eligible for the cluster upgrade with runtime: 7.2.0. "
                + "Please choose a runtime from the following: 7.0.2", e.getMessage());
    }

    @Test
    public void testFilterUpgradeOptionsLockComponents() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setLockComponents(true);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2L);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        underTest.filterUpgradeOptions(response, request, true);

        assertEquals(2, response.getUpgradeCandidates().size());
        assertTrue(response.getUpgradeCandidates().stream().map(ImageInfoV4Response::getImageId).anyMatch(id -> id.equals(IMAGE_ID_LAST)));
        assertTrue(response.getUpgradeCandidates().stream().map(ImageInfoV4Response::getImageId).anyMatch(id -> id.equals(IMAGE_ID)));
    }

    @Test
    public void testFilterUpgradeOptionsDefaultCase() {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setDryRun(true);
        UpgradeV4Response response = new UpgradeV4Response();
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2L);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        underTest.filterUpgradeOptions(response, request, true);

        assertEquals(2, response.getUpgradeCandidates().size());
        assertEquals(2, response.getUpgradeCandidates().stream().map(ImageInfoV4Response::getImageId).collect(Collectors.toSet()).size());
    }

    private ImageComponentVersions createExpectedPackageVersions() {
        ImageComponentVersions imageComponentVersions = new ImageComponentVersions();
        imageComponentVersions.setCm(V_7_0_3);
        imageComponentVersions.setCdp(V_7_0_2);
        return imageComponentVersions;
    }
}