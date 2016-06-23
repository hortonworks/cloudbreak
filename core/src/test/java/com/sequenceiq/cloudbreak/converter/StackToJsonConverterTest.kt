package com.sequenceiq.cloudbreak.converter

import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Mockito.`when`

import java.util.Arrays
import java.util.HashMap
import java.util.HashSet

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.ClusterResponse
import com.sequenceiq.cloudbreak.api.model.FailurePolicyJson
import com.sequenceiq.cloudbreak.api.model.ImageJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson
import com.sequenceiq.cloudbreak.api.model.OrchestratorResponse
import com.sequenceiq.cloudbreak.api.model.StackResponse
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.FailurePolicy
import com.sequenceiq.cloudbreak.domain.Network
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.image.ImageService

class StackToJsonConverterTest : AbstractEntityConverterTest<Stack>() {

    @InjectMocks
    private var underTest: StackToJsonConverter? = null

    @Mock
    private val conversionService: ConversionService? = null

    @Mock
    private val imageService: ImageService? = null

    @Before
    @Throws(CloudbreakImageNotFoundException::class)
    fun setUp() {
        underTest = StackToJsonConverter()
        MockitoAnnotations.initMocks(this)
        `when`(imageService!!.getImage(anyLong())).thenReturn(Image("testimage", HashMap<InstanceGroupType, String>(), HDPRepo(), "2.2.4"))
    }

    @Test
    fun testConvert() {
        // GIVEN
        given(conversionService!!.convert<Any>(any<Any>(Any::class.java), any<Class>(Class<Any>::class.java))).willReturn(ImageJson()).willReturn(ClusterResponse()).willReturn(FailurePolicyJson()).willReturn(OrchestratorResponse())
        given(conversionService.convert(any<Any>(Any::class.java), any<TypeDescriptor>(TypeDescriptor::class.java), any<TypeDescriptor>(TypeDescriptor::class.java))).willReturn(HashSet<InstanceGroupJson>())
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("platformVariant"))
    }

    @Test
    fun testConvertWithoutCredential() {
        // GIVEN
        given(conversionService!!.convert<Any>(any<Any>(Any::class.java), any<Class>(Class<Any>::class.java))).willReturn(ImageJson()).willReturn(ClusterResponse()).willReturn(FailurePolicyJson()).willReturn(OrchestratorResponse())
        given(conversionService.convert(any<Any>(Any::class.java), any<TypeDescriptor>(TypeDescriptor::class.java), any<TypeDescriptor>(TypeDescriptor::class.java))).willReturn(HashSet<InstanceGroupJson>())
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("credentialId", "cloudPlatform", "platformVariant"))
    }

    @Test
    fun testConvertWithoutCluster() {
        // GIVEN
        source.cluster = null
        given(conversionService!!.convert<Any>(any<Any>(Any::class.java), any<Class>(Class<Any>::class.java))).willReturn(ImageJson()).willReturn(FailurePolicyJson()).willReturn(OrchestratorResponse())
        given(conversionService.convert(any<Any>(Any::class.java), any<TypeDescriptor>(TypeDescriptor::class.java), any<TypeDescriptor>(TypeDescriptor::class.java))).willReturn(HashSet<InstanceGroupJson>())
        source.cluster = null
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cluster", "platformVariant"))
    }

    @Test
    fun testConvertWithoutFailurePolicy() {
        // GIVEN
        source.failurePolicy = null
        given(conversionService!!.convert<Any>(any<Any>(Any::class.java), any<Class>(Class<Any>::class.java))).willReturn(ImageJson()).willReturn(ClusterResponse()).willReturn(OrchestratorResponse())
        given(conversionService.convert(any<Any>(Any::class.java), any<TypeDescriptor>(TypeDescriptor::class.java), any<TypeDescriptor>(TypeDescriptor::class.java))).willReturn(HashSet<InstanceGroupJson>())
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("failurePolicy", "platformVariant"))
    }

    @Test
    fun testConvertWithoutNetwork() {
        // GIVEN
        source.network = null
        given(conversionService!!.convert<Any>(any<Any>(Any::class.java), any<Class>(Class<Any>::class.java))).willReturn(ImageJson()).willReturn(ClusterResponse()).willReturn(FailurePolicyJson()).willReturn(OrchestratorResponse())
        given(conversionService.convert(any<Any>(Any::class.java), any<TypeDescriptor>(TypeDescriptor::class.java), any<TypeDescriptor>(TypeDescriptor::class.java))).willReturn(HashSet<InstanceGroupJson>())
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("networkId", "platformVariant"))
    }

    override fun createSource(): Stack {
        val stack = TestUtil.stack()
        val cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L)
        stack.cluster = cluster
        stack.availabilityZone = "avZone"
        val network = Network()
        network.id = 1L
        stack.network = network
        stack.failurePolicy = FailurePolicy()
        val orchestrator = Orchestrator()
        orchestrator.id = 1L
        orchestrator.apiEndpoint = "endpoint"
        orchestrator.type = "type"
        stack.orchestrator = orchestrator
        stack.parameters = HashMap<String, String>()
        stack.setCloudPlatform("OPENSTACK")
        stack.gatewayPort = 9443
        return stack
    }
}
