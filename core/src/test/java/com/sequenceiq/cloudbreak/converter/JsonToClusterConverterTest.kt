package com.sequenceiq.cloudbreak.converter

import org.mockito.BDDMockito.given
import org.mockito.Matchers.any

import java.util.Arrays

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.core.convert.ConversionService

import com.sequenceiq.cloudbreak.api.model.ClusterRequest
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.FileSystem

class JsonToClusterConverterTest : AbstractJsonConverterTest<ClusterRequest>() {

    @InjectMocks
    private var underTest: JsonToClusterConverter? = null

    @Mock
    private val conversionService: ConversionService? = null

    @Before
    fun setUp() {
        underTest = JsonToClusterConverter()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(getRequest("stack/cluster.json"))
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "ambariStackDetails", "fileSystem", "sssdConfig", "certDir", "rdsConfig"))
    }

    @Test
    fun testConvertWithAmbariStackDetails() {
        // GIVEN
        val clusterRequest = getRequest("stack/cluster-with-ambari-stack-details.json")
        given(conversionService!!.convert<AmbariStackDetails>(clusterRequest.ambariStackDetails, AmbariStackDetails::class.java)).willReturn(AmbariStackDetails())
        // WHEN
        val result = underTest!!.convert(clusterRequest)
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "fileSystem", "sssdConfig", "certDir", "rdsConfig"))
    }

    @Test
    fun testConvertWithFileSystemDetails() {
        // GIVEN
        given(conversionService!!.convert<Any>(any<FileSystemRequest>(FileSystemRequest::class.java), any<Class>(Class<Any>::class.java))).willReturn(FileSystem())
        // WHEN
        val result = underTest!!.convert(getRequest("stack/cluster-with-file-system.json"))
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "ambariStackDetails", "sssdConfig", "certDir", "rdsConfig"))
    }

    override val requestClass: Class<ClusterRequest>
        get() = ClusterRequest::class.java
}
