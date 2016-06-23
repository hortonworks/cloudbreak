package com.sequenceiq.cloudbreak.cloud.handler.testcontext

import org.mockito.Matchers.any
import org.mockito.Matchers.anyList
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.eq
import org.mockito.Mockito.`when`

import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.concurrent.ScheduledThreadPoolExecutor

import javax.annotation.PostConstruct
import javax.inject.Inject

import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.cloud.Authenticator
import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.CredentialConnector
import com.sequenceiq.cloudbreak.cloud.InstanceConnector
import com.sequenceiq.cloudbreak.cloud.MetadataCollector
import com.sequenceiq.cloudbreak.cloud.ResourceConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.handler.ParameterGenerator
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Variant
import com.sequenceiq.cloudbreak.cloud.model.Volume
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.cloud.service.Persister
import com.sequenceiq.cloudbreak.common.type.ResourceType

import reactor.Environment

@Configuration
@ComponentScan("com.sequenceiq.cloudbreak.cloud")
@PropertySource("classpath:application.properties")
class TestApplicationContext {

    private val cloudInstance = CloudInstance("instanceId",
            InstanceTemplate("flavor", "groupName", 1L, emptyList<Volume>(), InstanceStatus.CREATE_REQUESTED, HashMap<String, Any>()))
    private val cloudInstanceBad = CloudInstance("instanceIdBad",
            InstanceTemplate("flavor", "groupName", 1L, emptyList<Volume>(), InstanceStatus.CREATE_REQUESTED, HashMap<String, Any>()))

    @Mock
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    @Mock
    private val cloudConnector: CloudConnector? = null

    @Mock
    private val authenticator: Authenticator? = null

    @Mock
    private val credentialConnector: CredentialConnector? = null

    @Mock
    private val collector: MetadataCollector? = null

    @Mock
    private val resourceConnector: ResourceConnector? = null

    @Mock
    private val instanceConnector: InstanceConnector? = null

    @Mock
    private val persistenceNotifier: PersistenceNotifier? = null

    @Mock
    val persister: Persister<Any>? = null

    @Inject
    private val g: ParameterGenerator? = null

    @PostConstruct
    fun initMocks() {
        MockitoAnnotations.initMocks(this)
    }

    @Bean(name = "instance")
    fun cloudInstance(): CloudInstance {
        return cloudInstance
    }

    @Bean(name = "bad-instance")
    fun cloudInstanceBad(): CloudInstance {
        return cloudInstanceBad
    }

    @Bean
    fun listeningScheduledExecutorService(): ListeningScheduledExecutorService {
        return MoreExecutors.listeningDecorator(ScheduledThreadPoolExecutor(1))
    }

    @Bean
    fun cloudPlatformConnectors(): CloudPlatformConnectors {
        `when`(cloudPlatformConnectors!!.get(any<Any>() as CloudPlatformVariant)).thenReturn(cloudConnector)
        return cloudPlatformConnectors
    }

    @Bean
    @Throws(Exception::class)
    fun cloudConnectors(): CloudConnector {
        val resource = CloudResource.Builder().type(ResourceType.HEAT_STACK).name("ref").build()
        `when`(cloudConnector!!.authentication()).thenReturn(authenticator)
        `when`(cloudConnector.credentials()).thenReturn(credentialConnector)
        `when`(credentialConnector!!.create(any<AuthenticatedContext>(AuthenticatedContext::class.java))).thenReturn(CloudCredentialStatus(null, CredentialStatus.CREATED))
        `when`(credentialConnector.delete(any<AuthenticatedContext>(AuthenticatedContext::class.java))).thenReturn(CloudCredentialStatus(null, CredentialStatus.DELETED))
        `when`(authenticator!!.authenticate(any<Any>() as CloudContext, any<Any>() as CloudCredential)).thenReturn(g!!.createAuthenticatedContext())
        `when`(cloudConnector.platform()).thenReturn(Platform.platform("TESTCONNECTOR"))
        `when`(cloudConnector.variant()).thenReturn(Variant.variant("TESTVARIANT"))
        `when`(cloudConnector.resources()).thenReturn(resourceConnector)
        `when`(cloudConnector.instances()).thenReturn(instanceConnector)
        `when`(cloudConnector.metadata()).thenReturn(collector)
        `when`(resourceConnector!!.launch(any<Any>() as AuthenticatedContext, any<Any>() as CloudStack, any<Any>() as PersistenceNotifier, any<Any>() as AdjustmentType, anyLong())).thenReturn(Arrays.asList(CloudResourceStatus(resource, ResourceStatus.CREATED)))
        `when`(resourceConnector.terminate(any<Any>() as AuthenticatedContext, any<Any>() as CloudStack, any<Any>() as List<CloudResource>)).thenReturn(Arrays.asList(CloudResourceStatus(resource, ResourceStatus.DELETED)))
        `when`(resourceConnector.update(any<Any>() as AuthenticatedContext, any<Any>() as CloudStack, any<Any>() as List<CloudResource>)).thenReturn(Arrays.asList(CloudResourceStatus(resource, ResourceStatus.UPDATED)))
        `when`(resourceConnector.upscale(any<Any>() as AuthenticatedContext, any<Any>() as CloudStack, any<Any>() as List<CloudResource>)).thenReturn(Arrays.asList(CloudResourceStatus(resource, ResourceStatus.UPDATED)))
        `when`(resourceConnector.downscale(any<Any>() as AuthenticatedContext, any<Any>() as CloudStack, any<Any>() as List<CloudResource>, anyList())).thenReturn(Arrays.asList(CloudResourceStatus(resource, ResourceStatus.UPDATED)))
        `when`(instanceConnector!!.check(any<Any>() as AuthenticatedContext, any<Any>() as List<CloudInstance>)).thenReturn(Arrays.asList(CloudVmInstanceStatus(cloudInstance, InstanceStatus.STARTED)))
        val collectInstanceStatus = CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS)
        `when`(collector!!.collect(any<Any>() as AuthenticatedContext, any<Any>() as List<CloudResource>, any<Any>() as List<CloudInstance>)).thenReturn(Arrays.asList(CloudVmMetaDataStatus(collectInstanceStatus, CloudInstanceMetaData("privateIp", "publicIp", "hypervisor"))))
        `when`(instanceConnector.start(any<Any>() as AuthenticatedContext, any<Any>() as List<CloudResource>, any<Any>() as List<CloudInstance>)).thenReturn(Arrays.asList(CloudVmInstanceStatus(cloudInstance, InstanceStatus.STARTED)))
        `when`(instanceConnector.stop(any<Any>() as AuthenticatedContext, any<Any>() as List<CloudResource>, any<Any>() as List<CloudInstance>)).thenReturn(Arrays.asList(CloudVmInstanceStatus(cloudInstance, InstanceStatus.STOPPED)))
        `when`(instanceConnector.getConsoleOutput(any<Any>() as AuthenticatedContext, eq(cloudInstance))).thenReturn(g.sshFingerprint + "    RSA/n-----END SSH HOST KEY FINGERPRINTS-----")
        `when`(instanceConnector.getConsoleOutput(any<Any>() as AuthenticatedContext, eq(cloudInstanceBad))).thenReturn("XYZ    RSA/n-----END SSH HOST KEY FINGERPRINTS-----")
        return cloudConnector
    }

    @Bean
    fun env(): Environment {
        return Environment.initializeIfEmpty()
    }

    companion object {

        @Bean
        fun propertyConfigInDev(): PropertySourcesPlaceholderConfigurer {
            return PropertySourcesPlaceholderConfigurer()
        }
    }

}
