package com.sequenceiq.cloudbreak.cloud.openstack.common;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.image.v2.ImageService;
import org.openstack4j.openstack.image.v2.domain.GlanceImage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@RunWith(MockitoJUnitRunner.class)
public class OpenStackImageVerifierTest {

    @InjectMocks
    private OpenStackImageVerifier underTest = new OpenStackImageVerifier();

    @Mock
    private OSClient<?> osClient;

    @Mock
    private ImageService imageService;

    @Before
    public void setUp() {
        when(osClient.imagesV2()).thenReturn(imageService);
    }

    @Test(expected = CloudConnectorException.class)
    public void testNullResult() {
        try {
            when(imageService.list(anyMap())).thenReturn(null);
            when(imageService.list()).thenReturn(null);
            underTest.exist(osClient, "invalidentryTgatResultsNull");
        } catch (CloudConnectorException cce) {
            Assert.assertEquals("OpenStack image: invalidentryTgatResultsNull not found", cce.getMessage());
            throw cce;
        }
    }

    @Test
    public void testFoundOne() {
        GlanceImage newImage = new GlanceImage();
        newImage.setId("id1");
        newImage.setName("exist-id1");
        List<GlanceImage> returnedImages = ImmutableList.of(newImage);
        Map<String, String> map = ImmutableMap.of("name", "exist-id1");
        doReturn(returnedImages).when(imageService).list(Mockito.eq(map));
        underTest.exist(osClient, "exist-id1");
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
            doReturn(returnedImages).when(imageService).list(Mockito.eq(map));
            underTest.exist(osClient, "exist-id1");
        } catch (CloudConnectorException cce) {
            Assert.assertEquals("OpenStack image: exist-id1 not found with ids: id1, id2", cce.getMessage());
            throw cce;
        }
    }
}
