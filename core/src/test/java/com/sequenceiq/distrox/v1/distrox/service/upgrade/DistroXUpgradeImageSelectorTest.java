package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.model.OsType;

class DistroXUpgradeImageSelectorTest {

    private final DistroXUpgradeImageSelector underTest = new DistroXUpgradeImageSelector();

    @Test
    public void testLatestImageFromCandidatesWhenRequestNull() {
        List<ImageInfoV4Response> candidates = List.of(createImageResponse("A", 1L, "5"), createImageResponse("B", 6L, "3"), createImageResponse("C", 3L, "1"));

        ImageInfoV4Response result = underTest.determineImageId(null, createUpgradeV4Response(candidates));

        assertEquals("B", result.getImageId());
        assertEquals("3", result.getComponentVersions().getCdp());
        assertEquals(6L, result.getCreated());
    }

    @Test
    public void testLatestImageFromCandidatesWhenRequestIsEmpty() {
        List<ImageInfoV4Response> candidates = List.of(createImageResponse("A", 1L, "5"), createImageResponse("B", 6L, "3"), createImageResponse("C", 3L, "1"));
        UpgradeV4Request request = mock(UpgradeV4Request.class);
        when(request.isEmpty()).thenReturn(Boolean.TRUE);

        ImageInfoV4Response result = underTest.determineImageId(request, createUpgradeV4Response(candidates));

        assertEquals("B", result.getImageId());
        assertEquals("3", result.getComponentVersions().getCdp());
        assertEquals(6L, result.getCreated());
    }

    @Test
    public void testLatestImageFromCandidatesWhenLockComponentsIsTrue() {
        List<ImageInfoV4Response> candidates = List.of(createImageResponse("A", 1L, "5"), createImageResponse("B", 6L, "3"), createImageResponse("C", 3L, "1"));
        UpgradeV4Request request = mock(UpgradeV4Request.class);
        when(request.getLockComponents()).thenReturn(Boolean.TRUE);

        ImageInfoV4Response result = underTest.determineImageId(request, createUpgradeV4Response(candidates));

        assertEquals("B", result.getImageId());
        assertEquals("3", result.getComponentVersions().getCdp());
        assertEquals(6L, result.getCreated());
    }

    @Test
    public void testSelectedImageByIdFound() {
        List<ImageInfoV4Response> candidates = List.of(createImageResponse("A", 1L, "5"), createImageResponse("B", 6L, "3"), createImageResponse("C", 3L, "1"));
        UpgradeV4Request request = new UpgradeV4Request();
        request.setImageId("C");

        ImageInfoV4Response result = underTest.determineImageId(request, createUpgradeV4Response(candidates));

        assertEquals("C", result.getImageId());
        assertEquals("1", result.getComponentVersions().getCdp());
        assertEquals(3L, result.getCreated());
    }

    @Test
    public void testSelectedImageByIdNotInCandidates() {
        List<ImageInfoV4Response> candidates = List.of(createImageResponse("A", 1L, "5"), createImageResponse("B", 6L, "3"), createImageResponse("C", 3L, "1"));
        UpgradeV4Request request = new UpgradeV4Request();
        request.setImageId("D");

        assertThrows(BadRequestException.class, () -> underTest.determineImageId(request, createUpgradeV4Response(candidates)));
    }

    @Test
    public void testSelectedImageByRuntimeFound() {
        List<ImageInfoV4Response> candidates =
                List.of(createImageResponse("A", 1L, "5"),
                        createImageResponse("B", 6L, "3"),
                        createImageResponse("C", 3L, "1"),
                        createImageResponse("D", 8L, "3"));
        UpgradeV4Request request = new UpgradeV4Request();
        request.setRuntime("3");

        ImageInfoV4Response result = underTest.determineImageId(request, createUpgradeV4Response(candidates));

        assertEquals("D", result.getImageId());
        assertEquals("3", result.getComponentVersions().getCdp());
        assertEquals(8L, result.getCreated());
    }

    @Test
    public void testSelectedImageByRuntimeShouldReturnCentOsImageEvenIfTheRedHatImageIsNewer() {
        List<ImageInfoV4Response> candidates =
                List.of(createImageResponseWithRedHatImage("A", 9L, "3"),
                        createImageResponse("B", 6L, "3"),
                        createImageResponse("C", 3L, "1"),
                        createImageResponse("D", 8L, "3"));
        UpgradeV4Request request = new UpgradeV4Request();
        request.setRuntime("3");

        ImageInfoV4Response result = underTest.determineImageId(request, createUpgradeV4Response(candidates));

        assertEquals("D", result.getImageId());
        assertEquals("3", result.getComponentVersions().getCdp());
        assertEquals(8L, result.getCreated());
    }

    @Test
    public void testSelectedImageByRuntimeNotFound() {
        List<ImageInfoV4Response> candidates =
                List.of(createImageResponse("A", 1L, "5"),
                        createImageResponse("B", 6L, "3"),
                        createImageResponse("C", 3L, "1"),
                        createImageResponse("D", 8L, "3"));
        UpgradeV4Request request = new UpgradeV4Request();
        request.setRuntime("8");

        assertThrows(BadRequestException.class, () -> underTest.determineImageId(request, createUpgradeV4Response(candidates)));
    }

    private ImageInfoV4Response createImageResponse(String id, long creation, String cdp) {
        ImageInfoV4Response image = new ImageInfoV4Response();
        image.setImageId(id);
        image.setCreated(creation);
        image.setComponentVersions(new ImageComponentVersions("a", "b", cdp, "d", OsType.CENTOS7.getOs(), "a", List.of()));
        return image;
    }

    private ImageInfoV4Response createImageResponseWithRedHatImage(String id, long creation, String cdp) {
        ImageInfoV4Response image = createImageResponse(id, creation, cdp);
        image.getComponentVersions().setOs(OsType.RHEL8.getOs());
        return image;
    }

    private UpgradeV4Response createUpgradeV4Response(List<ImageInfoV4Response> candidates) {
        ImageInfoV4Response currentImage = new ImageInfoV4Response();
        ImageComponentVersions imageComponentVersions = new ImageComponentVersions();
        imageComponentVersions.setOs(OsType.CENTOS7.getOs());
        currentImage.setComponentVersions(imageComponentVersions);
        return new UpgradeV4Response(currentImage, candidates, null);
    }
}