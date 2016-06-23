package com.sequenceiq.cloudbreak.orchestrator.swarm.containers

import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Matchers.anyBoolean
import org.mockito.Matchers.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

import javax.ws.rs.ProcessingException

import org.junit.Before
import org.junit.Test
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.InspectContainerCmd
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.command.StartContainerCmd
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.api.model.RestartPolicy
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath

import jersey.repackaged.com.google.common.collect.Sets

abstract class AbstractContainerBootstrapTest {

    private var underTest: OrchestratorBootstrap? = null

    @Mock
    val mockedDockerClient: DockerClient? = null

    @Mock
    private val mockedCreateContainerCmd: CreateContainerCmd? = null

    @Mock
    private val mockedStartContainerCmd: StartContainerCmd? = null

    @Mock
    private val inspectContainerCmd: InspectContainerCmd? = null

    @Mock
    private val inspectContainerResponse: InspectContainerResponse? = null

    @Mock
    private val createContainerResponse: CreateContainerResponse? = null

    @Mock
    private val containerState: InspectContainerResponse.ContainerState? = null


    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        underTest = testInstance
    }

    @Test
    @Throws(Exception::class)
    fun testCall() {
        // GIVEN
        mockDockerClient()
        // WHEN
        underTest!!.call()
        // THEN
        verify<DockerClient>(mockedDockerClient, times(1)).createContainerCmd(anyString())
    }

    @Test(expected = ProcessingException::class)
    @Throws(Exception::class)
    fun testCallWhenCreateContainerThrowsException() {
        // GIVEN
        mockDockerClient()
        doThrow(ProcessingException("EX")).`when`<DockerClient>(mockedDockerClient).createContainerCmd(anyString())
        // WHEN
        underTest!!.call()
    }

    @Test(expected = ProcessingException::class)
    @Throws(Exception::class)
    fun testCallWhenStartContainerThrowsException() {
        // GIVEN
        mockDockerClient()
        doThrow(ProcessingException("EX")).`when`<DockerClient>(mockedDockerClient).startContainerCmd(anyString())
        // WHEN
        underTest!!.call()
    }

    fun mockDockerClient() {
        given(mockedDockerClient!!.createContainerCmd(anyString())).willReturn(mockedCreateContainerCmd)
        given(mockedDockerClient.startContainerCmd(anyString())).willReturn(mockedStartContainerCmd)
        given(mockedDockerClient.inspectContainerCmd(anyString())).willReturn(inspectContainerCmd)
        mockCreateContainerCommand()
        mockInspectcontainerCmd()

    }

    private fun mockCreateContainerCommand() {
        given(mockedCreateContainerCmd!!.withCmd(anyString())).willReturn(mockedCreateContainerCmd)
        given(mockedCreateContainerCmd.withCmd(Matchers.anyVararg<String>())).willReturn(mockedCreateContainerCmd)
        given(mockedCreateContainerCmd.withName(anyString())).willReturn(mockedCreateContainerCmd)
        given(mockedCreateContainerCmd.withNetworkMode(anyString())).willReturn(mockedCreateContainerCmd)
        given(mockedCreateContainerCmd.withRestartPolicy(any<RestartPolicy>(RestartPolicy::class.java))).willReturn(mockedCreateContainerCmd)
        given(mockedCreateContainerCmd.withPrivileged(anyBoolean())).willReturn(mockedCreateContainerCmd)
        given(mockedCreateContainerCmd.withBinds(Matchers.anyVararg<Bind>())).willReturn(mockedCreateContainerCmd)
        given(mockedCreateContainerCmd.withPortBindings(any<Ports>(Ports::class.java))).willReturn(mockedCreateContainerCmd)
        given(mockedCreateContainerCmd.withEnv(Matchers.anyVararg<String>())).willReturn(mockedCreateContainerCmd)
        given(mockedCreateContainerCmd.withExposedPorts(Matchers.anyVararg<ExposedPort>())).willReturn(mockedCreateContainerCmd)
        given(mockedCreateContainerCmd.exec()).willReturn(createContainerResponse)
    }

    private fun mockInspectcontainerCmd() {
        given(inspectContainerCmd!!.exec()).willReturn(inspectContainerResponse)
        given(inspectContainerResponse!!.state).willReturn(containerState)
        given(containerState!!.isRunning).willReturn(true)
    }


    abstract val testInstance: OrchestratorBootstrap

    companion object {
        protected val DUMMY_CLOUD_PLATFORM = "GCP"
        protected val DUMMY_GENERATED_ID = "dummyGeneratedId"
        protected val DUMMY_IMAGE = "sequenceiq/dummy:0.0.1"
        protected val DUMMY_NODE = "dummyNode"
        protected val DUMMY_VOLUMES: Set<String> = Sets.newHashSet("/var/path1", "/var/path2")
        protected val CMD = arrayOf("cmd1", "cmd2")
        protected val DUMMY_LOG_VOLUME = LogVolumePath("/var/path1", "/var/path2")
    }
}
