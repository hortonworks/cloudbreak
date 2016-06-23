package com.sequenceiq.cloudbreak.service.image

import com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator

import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.cloud.PlatformParameters
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones
import com.sequenceiq.cloudbreak.cloud.model.DiskType
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator
import com.sequenceiq.cloudbreak.cloud.model.Region
import com.sequenceiq.cloudbreak.cloud.model.Regions
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation
import com.sequenceiq.cloudbreak.cloud.model.VmType
import com.sequenceiq.cloudbreak.cloud.model.VmTypes
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants
import com.sequenceiq.cloudbreak.util.FileReaderUtils

import freemarker.template.Configuration
import freemarker.template.TemplateException

class UserDataBuilderTest {

    @Before
    @Throws(IOException::class, TemplateException::class)
    fun setup() {
        val factoryBean = FreeMarkerConfigurationFactoryBean()
        factoryBean.setPreferFileSystemAccess(false)
        factoryBean.setTemplateLoaderPath("classpath:/")
        factoryBean.afterPropertiesSet()
        val configuration = factoryBean.`object`
        userDataBuilder.setFreemarkerConfiguration(configuration)

        val params = UserDataBuilderParams()
        params.customData = "date >> /tmp/time.txt"

        ReflectionTestUtils.setField(userDataBuilder, "userDataBuilderParams", params)
    }

    @Test
    @Throws(IOException::class)
    fun testBuildUserDataAzure() {
        val expectedGwScript = FileReaderUtils.readFileFromClasspath("azure-gateway-init.sh")
        val expectedCoreScript = FileReaderUtils.readFileFromClasspath("azure-core-init.sh")
        val userdata = userDataBuilder.buildUserData(Platform.platform("AZURE_RM"), "ssh-rsa public", "ssh-rsa test", "cloudbreak", platformParameters, true)
        Assert.assertEquals(expectedGwScript, userdata[InstanceGroupType.GATEWAY])
        Assert.assertEquals(expectedCoreScript, userdata[InstanceGroupType.CORE])
    }

    val platformParameters: PlatformParameters
        get() = object : PlatformParameters {
            override fun scriptParams(): ScriptParams {
                return ScriptParams("sd", 98)
            }

            override fun diskTypes(): DiskTypes {
                return DiskTypes(ArrayList<DiskType>(), DiskType.diskType(""), HashMap<String, VolumeParameterType>())
            }

            override fun regions(): Regions {
                return Regions(ArrayList<Region>(), Region.region(""))
            }

            override fun availabilityZones(): AvailabilityZones {
                return AvailabilityZones(HashMap<Region, List<AvailabilityZone>>())
            }

            override fun resourceDefinition(resource: String): String {
                return ""
            }

            override fun additionalStackParameters(): List<StackParamValidation> {
                return ArrayList()
            }

            override fun orchestratorParams(): PlatformOrchestrator {
                return PlatformOrchestrator(Arrays.asList<T>(Companion.orchestrator(OrchestratorConstants.SALT), Companion.orchestrator(OrchestratorConstants.SWARM)),
                        Companion.orchestrator(OrchestratorConstants.SALT))
            }

            override fun vmTypes(): VmTypes {
                return VmTypes(ArrayList<VmType>(), VmType.vmType(""))
            }
        }

    companion object {

        private val userDataBuilder = UserDataBuilder()
    }
}