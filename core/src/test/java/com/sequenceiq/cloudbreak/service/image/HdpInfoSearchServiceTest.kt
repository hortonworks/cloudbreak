package com.sequenceiq.cloudbreak.service.image

import org.mockito.BDDMockito.given

import java.io.IOException

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import com.sequenceiq.cloudbreak.cloud.model.CloudbreakImageCatalog
import com.sequenceiq.cloudbreak.cloud.model.HDPInfo
import com.sequenceiq.cloudbreak.util.FileReaderUtils
import com.sequenceiq.cloudbreak.util.JsonUtil

@RunWith(MockitoJUnitRunner::class)
class HdpInfoSearchServiceTest {

    private var cloudbreakImageCatalog: CloudbreakImageCatalog? = null

    @Mock
    private val imageCatalogProvider: ImageCatalogProvider? = null

    @InjectMocks
    private val underTest: HdpInfoSearchService? = null

    @Before
    @Throws(IOException::class)
    fun setup() {
        val catalogJson = FileReaderUtils.readFileFromClasspath("image/cb-image-catalog.json")
        cloudbreakImageCatalog = JsonUtil.readValue<CloudbreakImageCatalog>(catalogJson, CloudbreakImageCatalog::class.java)

        given(imageCatalogProvider!!.imageCatalog).willReturn(cloudbreakImageCatalog)
    }

    @Test
    @Throws(IOException::class)
    fun testWithNull() {
        val hdpInfo = underTest!!.searchHDPInfo(null, null)
        Assert.assertNull("HDP info shall be null for null input", hdpInfo)
    }

    @Test
    fun testWithNotExsisting() {
        var hdpInfo = underTest!!.searchHDPInfo("2.4.0.0-661", "2.4.3.0-21")
        Assert.assertNull("HDP info shall be null for non exsisting combination", hdpInfo)

        hdpInfo = underTest.searchHDPInfo("2.4.0.0-660", "2.4.3.0-14")
        Assert.assertNull("HDP info shall be null for non exsisting combination", hdpInfo)
    }

    @Test
    fun testExactVersion() {
        var hdpInfo = underTest!!.searchHDPInfo("2.4.0.0-660", "2.4.3.0-21")
        Assert.assertEquals("ap-northeast-1-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.images!!["aws"].get("ap-northeast-1"))
        Assert.assertEquals("ap-northeast-2-ami-2.4.0.0-660-2.4.3.0-21", hdpInfo.images!!["aws"].get("ap-northeast-2"))

        hdpInfo = underTest.searchHDPInfo("2.5.0.0-222", "2.5.0.0-723")
        Assert.assertEquals("ap-northeast-1-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.images!!["aws"].get("ap-northeast-1"))
        Assert.assertEquals("ap-northeast-2-ami-2.5.0.0-222-2.5.0.0-723", hdpInfo.images!!["aws"].get("ap-northeast-2"))
    }

    @Test
    fun testPrefix() {
        val hdpInfo = underTest!!.searchHDPInfo("2.4", "2.4")
        Assert.assertEquals("ap-northeast-1-ami-2.4.0.0-770-2.4.10.0-100", hdpInfo.images!!["aws"].get("ap-northeast-1"))
        Assert.assertEquals("ap-northeast-2-ami-2.4.0.0-770-2.4.10.0-100", hdpInfo.images!!["aws"].get("ap-northeast-2"))
    }

}