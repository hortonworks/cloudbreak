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
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

class DistroXUpgradeImageSelectorTest {

    private final DistroXUpgradeImageSelector underTest = new DistroXUpgradeImageSelector();

    @Test
    public void testLatestImageFromCandidatesWhenRequestNull() {
        List<ImageInfoV4Response> candidates =
                List.of(createImageResponse("A", 1L, "5"), createImageResponse("B", 6L, "3"), createImageResponse("C", 3L, "1"));

        ImageInfoV4Response result = underTest.determineImageId(null, candidates);

        assertEquals("B", result.getImageId());
        assertEquals("3", result.getComponentVersions().getCdp());
        assertEquals(6L, result.getCreated());
    }

    @Test
    public void testLatestImageFromCandidatesWhenRequestIsEmpty() {
        List<ImageInfoV4Response> candidates =
                List.of(createImageResponse("A", 1L, "5"), createImageResponse("B", 6L, "3"), createImageResponse("C", 3L, "1"));
        UpgradeV4Request request = mock(UpgradeV4Request.class);
        when(request.isEmpty()).thenReturn(Boolean.TRUE);

        ImageInfoV4Response result = underTest.determineImageId(request, candidates);

        assertEquals("B", result.getImageId());
        assertEquals("3", result.getComponentVersions().getCdp());
        assertEquals(6L, result.getCreated());
    }

    @Test
    public void testLatestImageFromCandidatesWhenLockComponentsIsTrue() {
        List<ImageInfoV4Response> candidates =
                List.of(createImageResponse("A", 1L, "5"), createImageResponse("B", 6L, "3"), createImageResponse("C", 3L, "1"));
        UpgradeV4Request request = mock(UpgradeV4Request.class);
        when(request.getLockComponents()).thenReturn(Boolean.TRUE);

        ImageInfoV4Response result = underTest.determineImageId(request, candidates);

        assertEquals("B", result.getImageId());
        assertEquals("3", result.getComponentVersions().getCdp());
        assertEquals(6L, result.getCreated());
    }

    @Test
    public void testSelectedImageByIdFound() {
        List<ImageInfoV4Response> candidates =
                List.of(createImageResponse("A", 1L, "5"), createImageResponse("B", 6L, "3"), createImageResponse("C", 3L, "1"));
        UpgradeV4Request request = new UpgradeV4Request();
        request.setImageId("C");

        ImageInfoV4Response result = underTest.determineImageId(request, candidates);

        assertEquals("C", result.getImageId());
        assertEquals("1", result.getComponentVersions().getCdp());
        assertEquals(3L, result.getCreated());
    }

    @Test
    public void testSelectedImageByIdNotInCandidates() {
        List<ImageInfoV4Response> candidates =
                List.of(createImageResponse("A", 1L, "5"), createImageResponse("B", 6L, "3"), createImageResponse("C", 3L, "1"));
        UpgradeV4Request request = new UpgradeV4Request();
        request.setImageId("D");

        assertThrows(BadRequestException.class, () -> underTest.determineImageId(request, candidates));
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

        ImageInfoV4Response result = underTest.determineImageId(request, candidates);

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

        assertThrows(BadRequestException.class, () -> underTest.determineImageId(request, candidates));
    }

    private ImageInfoV4Response createImageResponse(String id, long creation, String cdp) {
        ImageInfoV4Response image = new ImageInfoV4Response();
        image.setImageId(id);
        image.setCreated(creation);
        image.setComponentVersions(new ImageComponentVersions("a", "b", cdp, "d", "g", "a", List.of()));
        return image;
    }
}