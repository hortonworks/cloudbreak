package com.sequenceiq.cloudbreak.cloud.openstack.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.image.v2.ImageService;
import org.openstack4j.model.image.Image.Status;
import org.openstack4j.model.image.v2.Image.ImageStatus;
import org.openstack4j.openstack.image.v2.domain.GlanceImage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@RunWith(MockitoJUnitRunner.class)
public class OpenStackImageVerifierTest {

    @InjectMocks
    private final OpenStackImageVerifier underTest = new OpenStackImageVerifier();

    @Mock
    private OSClient<?> osClient;

    @Mock
    private ImageService imageService;

    @Mock
    private org.openstack4j.api.image.ImageService imageServiceV1;

    @Before
    public void setUp() {
        when(osClient.imagesV2()).thenReturn(imageService);
    }

    @Test
    public void testNotFoundResult() {
        GlanceImage newImage1 = new GlanceImage();
        newImage1.setId("id1");
        newImage1.setName("exist-id1");
        when(imageService.list(anyMap())).thenReturn(null);
        doReturn(ImmutableList.of(newImage1)).when(imageService).list();
        assertFalse(underTest.exist(osClient, "invalidentryTgatResultsNull"));
    }

    @Test
    public void testFoundOneAnyState() {
        for (ImageStatus status : ImageStatus.values()) {
            foundOneAnyState(status);
        }
    }

    @Test
    public void testV2RequestFallsBackToV1() {
        when(imageService.list(anyMap())).thenThrow(ProcessingException.class);
        when(osClient.images()).thenReturn(imageServiceV1);
        doReturn(image(Status.KILLED),
                image(Status.ACTIVE),
                image(Status.DELETED),
                image(Status.PENDING_DELETE),
                image(Status.QUEUED),
                image(Status.SAVING),
                image(Status.UNRECOGNIZED))
                .when(imageServiceV1).list(anyMap());

        testWithV1ImageStatus(ImageStatus.KILLED);
        testWithV1ImageStatus(ImageStatus.ACTIVE);
        testWithV1ImageStatus(ImageStatus.DELETED);
        testWithV1ImageStatus(ImageStatus.PENDING_DELETE);
        testWithV1ImageStatus(ImageStatus.QUEUED);
        testWithV1ImageStatus(ImageStatus.SAVING);
        testWithV1ImageStatus(ImageStatus.UNRECOGNIZED);
    }

    private ImmutableList<org.openstack4j.openstack.image.domain.GlanceImage> image(Status status) {
        org.openstack4j.openstack.image.domain.GlanceImage image = new org.openstack4j.openstack.image.domain.GlanceImage();
        image.setId("id1");
        image.setName("image1");
        image.status(status);
        return ImmutableList.of(image);
    }

    private void testWithV1ImageStatus(ImageStatus expectedStatus) {
        ImageStatus statusResult = underTest.getStatus(osClient, "image1").get();
        assertEquals(expectedStatus, statusResult);
    }

    @Test(expected = CloudConnectorException.class)
    public void testFoundTwo() {
        try {
            GlanceImage newImage1 = new GlanceImage();
            newImage1.setId("id1");
            newImage1.setName("exist-id1");
            GlanceImage newImage2 = new GlanceImage();
            newImage2.setId("id2");
            newImage2.setName("exist-id1");
            List<GlanceImage> returnedImages = ImmutableList.of(newImage1, newImage2);
            Map<String, String> map = ImmutableMap.of("name", "exist-id1");
            doReturn(returnedImages).when(imageService).list(ArgumentMatchers.eq(map));
            underTest.exist(osClient, "exist-id1");
        } catch (CloudConnectorException cce) {
            assertEquals("Multiple OpenStack images found with ids: id1, id2, image name: exist-id1", cce.getMessage());
            throw cce;
        }
    }

    private void foundOneAnyState(ImageStatus status) {
        GlanceImage newImage = Mockito.mock(GlanceImage.class);
        when(newImage.getStatus()).thenReturn(status);
        List<GlanceImage> returnedImages = ImmutableList.of(newImage);
        Map<String, String> map = ImmutableMap.of("name", "exist-id1");
        doReturn(returnedImages).when(imageService).list(ArgumentMatchers.eq(map));
        assertTrue(underTest.exist(osClient, "exist-id1"));
    }
}
