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

import com.sequenceiq.cloudbreak.cloud.model.CloudbreakImageCatalog;
import com.sequenceiq.cloudbreak.cloud.model.HDPInfo;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class HdpInfoUtilTest {

    private CloudbreakImageCatalog cloudbreakImageCatalog;

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @InjectMocks
    private HdpInfoUtil underTest;

    @Before
    public void setup() throws IOException {
        String catalogJson = FileReaderUtils.readFileFromClasspath("image/cb-image-catalog.json");
        cloudbreakImageCatalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalog.class);

        given(imageCatalogProvider.getImageCatalog()).willReturn(cloudbreakImageCatalog);
    }

    @Test
    public void testWithNull() throws IOException {
        HDPInfo hdpInfo = underTest.getHDPInfo(null, null);
        Assert.assertNull("HDP info shall be null for null input", hdpInfo);
    }

    @Test
    public void testWithNotExsisting() throws IOException {
        HDPInfo hdpInfo = underTest.getHDPInfo("2.4.0.0-661", "2.4.3.0-21");
        Assert.assertNull("HDP info shall be null for non exsisting combination", hdpInfo);

        hdpInfo = underTest.getHDPInfo("2.4.0.0-660", "2.4.3.0-14");
        Assert.assertNull("HDP info shall be null for non exsisting combination", hdpInfo);
    }

    @Test
    public void testExactVersion() throws IOException {
        HDPInfo hdpInfo = underTest.getHDPInfo("2.4.0.0-660", "2.4.3.0-21");
        Assert.assertEquals("ap-northeast-1-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.getImages().get("aws").get("ap-northeast-2"));

        hdpInfo = underTest.getHDPInfo("2.5.0.0-222", "2.5.0.0-723");
        Assert.assertEquals("ap-northeast-1-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-1"));
        Assert.assertEquals("ap-northeast-2-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.getImages().get("aws").get("ap-northeast-2"));
    }


}