package com.sequenceiq.cloudbreak.service.image;

import static org.mockito.BDDMockito.given;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudbreakImageCatalog;
import com.sequenceiq.cloudbreak.cloud.model.HDPInfo;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class HdpInfoSearchServiceTest {

    private CloudbreakImageCatalog cloudbreakImageCatalog;

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @InjectMocks
    private HdpInfoSearchService underTest;

    @Before
    public void setup() throws IOException {
        String catalogJson = FileReaderUtils.readFileFromClasspath("image/cb-image-catalog.json");
        cloudbreakImageCatalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalog.class);

        given(imageCatalogProvider.getImageCatalog(null)).willReturn(cloudbreakImageCatalog);
    }

    @Test
    public void testWithNull() throws IOException, CloudbreakImageNotFoundException {
        HDPInfo hdpInfo = underTest.searchHDPInfo(null, null, null, null);
        Assert.assertNull("HDP info shall be null for null input", hdpInfo);
    }

    @Test(expected = CloudbreakImageNotFoundException.class)
    public void testWithNotExsisting1() throws CloudbreakImageNotFoundException {
        underTest.searchHDPInfo("aws", "2.4.0.0-661", "2.4.3.0-21", null);

    }

    @Test(expected = CloudbreakImageNotFoundException.class)
    public void testWithNotExsisting2() throws CloudbreakImageNotFoundException {
        underTest.searchHDPInfo("aws", "2.4.0.0-660", "2.4.3.0-14", null);
    }

    @Test
    public void testExactVersion() throws CloudbreakImageNotFoundException {
        HDPInfo hdpInfo = underTest.searchHDPInfo("aws", "2.4.0.0-660", "2.4.3.0-21", null);
        Assert.assertEquals("ap-northeast-1-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.getImages().get("aws").get("ap-northeast-2"));

        hdpInfo = underTest.searchHDPInfo("aws", "2.5.0.0-222", "2.5.0.0-723", null);
        Assert.assertEquals("ap-northeast-1-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-2"));
    }

    @Test
    public void testExactVersionWithUnspecifiedCbVersion() throws CloudbreakImageNotFoundException {
        ReflectionTestUtils.setField(underTest, "cbVersion", "unspecified");

        HDPInfo hdpInfo = underTest.searchHDPInfo("aws", "2.4.0.0-660", "2.4.3.0-21", null);
        Assert.assertEquals("ap-northeast-1-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.getImages().get("aws").get("ap-northeast-2"));

        hdpInfo = underTest.searchHDPInfo("aws", "2.5.0.0-222", "2.5.0.0-723", null);
        Assert.assertEquals("ap-northeast-1-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-2"));
    }

    @Test
    public void testExactVersionWithNullCbVersion() throws CloudbreakImageNotFoundException {
        ReflectionTestUtils.setField(underTest, "cbVersion", null);

        HDPInfo hdpInfo = underTest.searchHDPInfo("aws", "2.4.0.0-660", "2.4.3.0-21", null);
        Assert.assertEquals("ap-northeast-1-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.getImages().get("aws").get("ap-northeast-2"));

        hdpInfo = underTest.searchHDPInfo("aws", "2.5.0.0-222", "2.5.0.0-723", null);
        Assert.assertEquals("ap-northeast-1-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-2"));
    }

    @Test
    public void testExactVersionWithSpecificCbVersion() throws CloudbreakImageNotFoundException {
        ReflectionTestUtils.setField(underTest, "cbVersion", "1.4-rc1");

        HDPInfo hdpInfo = underTest.searchHDPInfo("aws", "2.5", "2.4", null);
        Assert.assertEquals("ap-northeast-1-ami-2.5.0.0-222-2.4.10.0-100", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.5.0.0-222-2.4.10.0-100", hdpInfo.getImages().get("aws").get("ap-northeast-2"));

        hdpInfo = underTest.searchHDPInfo("aws", "2.5.0.0-222", "2.5.0.0-723", null);
        Assert.assertEquals("ap-northeast-1-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-2"));
    }

    @Test(expected = CloudbreakImageNotFoundException.class)
    public void testExactVersionWithSpecificCbVersionThatDoesNotExist1() throws CloudbreakImageNotFoundException {
        ReflectionTestUtils.setField(underTest, "cbVersion", "1.4-rc3");
        underTest.searchHDPInfo("aws", "2.5", "2.4", null);
    }

    @Test(expected = CloudbreakImageNotFoundException.class)
    public void testExactVersionWithSpecificCbVersionThatDoesNotExist2() throws CloudbreakImageNotFoundException {
        ReflectionTestUtils.setField(underTest, "cbVersion", "1.4-rc3");
        underTest.searchHDPInfo("aws", "2.5", "2.5", null);
    }

    @Test
    public void testExactVersionWithEmptyCbVersion() throws CloudbreakImageNotFoundException {
        ReflectionTestUtils.setField(underTest, "cbVersion", "");

        HDPInfo hdpInfo = underTest.searchHDPInfo("aws", "2.4.0.0-660", "2.4.3.0-21", null);
        Assert.assertEquals("ap-northeast-1-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.getImages().get("aws").get("ap-northeast-2"));

        hdpInfo = underTest.searchHDPInfo("aws", "2.5.0.0-222", "2.5.0.0-723", null);
        Assert.assertEquals("ap-northeast-1-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-2"));
    }

    @Test
    public void testPrefix() throws CloudbreakImageNotFoundException {
        HDPInfo hdpInfo = underTest.searchHDPInfo("aws", "2.4", "2.4", null);
        Assert.assertEquals("ap-northeast-1-ami-2.4.0.0-770-2.4.10.0-100", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.4.0.0-770-2.4.10.0-100", hdpInfo.getImages().get("aws").get("ap-northeast-2"));
    }

    @Test
    public void testLatestAmbariVersionWithEmptyCbVersion() throws CloudbreakImageNotFoundException {
        ReflectionTestUtils.setField(underTest, "cbVersion", "");

        HDPInfo hdpInfo = underTest.searchHDPInfo("aws", "2.5", "2.4", null);
        Assert.assertEquals("ap-northeast-1-ami-2.5.0.0-222-2.4.10.0-22-1", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.5.0.0-222-2.4.10.0-22-1", hdpInfo.getImages().get("aws").get("ap-northeast-2"));
    }

    @Test
    public void testLatestAmbariVersionWithCbVersion() throws CloudbreakImageNotFoundException {
        ReflectionTestUtils.setField(underTest, "cbVersion", "1.4-rc2");

        HDPInfo hdpInfo = underTest.searchHDPInfo("aws", "2.5", "2.4", null);
        Assert.assertEquals("ap-northeast-1-ami-2.5.0.0-222-2.4.10.0-22-1", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.5.0.0-222-2.4.10.0-22-1", hdpInfo.getImages().get("aws").get("ap-northeast-2"));
    }

    @Test
    public void testLatestAmbariVersionWithDifferentClouds() throws CloudbreakImageNotFoundException {
        ReflectionTestUtils.setField(underTest, "cbVersion", "1.4-rc2");

        HDPInfo hdpInfo = underTest.searchHDPInfo("azure", "2.5", "2.4", null);
        Assert.assertEquals("ap-northeast-1-ami-2.5.0.0-222-2.4.10.0-22", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.5.0.0-222-2.4.10.0-22", hdpInfo.getImages().get("aws").get("ap-northeast-2"));

        hdpInfo = underTest.searchHDPInfo("aws", "2.5", "2.4", null);
        Assert.assertEquals("ap-northeast-1-ami-2.5.0.0-222-2.4.10.0-22-1", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.5.0.0-222-2.4.10.0-22-1", hdpInfo.getImages().get("aws").get("ap-northeast-2"));
    }
}
