package com.sequenceiq.cloudbreak.orchestrator.swarm

import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.createContainer
import com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.startContainer
import org.mockito.Matchers.anyBoolean
import org.mockito.Matchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.NotFoundException
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.InspectContainerCmd
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.command.RemoveContainerCmd
import com.github.dockerjava.api.command.StartContainerCmd
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException

@RunWith(MockitoJUnitRunner::class)
class DockerClientUtilTest {

    @Mock
    private val client: DockerClient? = null

    @Mock
    private val inspectContainerResponse: InspectContainerResponse? = null

    @Mock
    private val createContainerResponse: CreateContainerResponse? = null

    @Mock
    private val inspectContainerCmd: InspectContainerCmd? = null

    @Mock
    private val removeContainerCmd: RemoveContainerCmd? = null

    @Mock
    private val createContainerCmd: CreateContainerCmd? = null

    @Mock
    private val startContainerCmd: StartContainerCmd? = null

    @Mock
    private val containerState: InspectContainerResponse.ContainerState? = null


    @Before
    fun before() {
        reset<DockerClient>(client)
        reset<CreateContainerCmd>(createContainerCmd)
        reset<InspectContainerResponse>(inspectContainerResponse)
        reset<RemoveContainerCmd>(removeContainerCmd)
        reset<InspectContainerCmd>(inspectContainerCmd)
        reset<InspectContainerResponse.ContainerState>(containerState)
        reset<StartContainerCmd>(startContainerCmd)
        reset<CreateContainerResponse>(createContainerResponse)
    }

    @Test
    @Throws(Exception::class)
    fun removeContainerIExistWhenInspectReturnWithNull() {
        `when`(client!!.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd)
        `when`(inspectContainerCmd!!.exec()).thenReturn(null)
        `when`(createContainerCmd!!.name).thenReturn("ambari-server")

        createContainer(client, createContainerCmd, "vm1")

        verify(client, times(1)).inspectContainerCmd(anyString())
        verify(client, times(0)).removeContainerCmd(anyString())
    }

    @Test
    @Throws(Exception::class)
    fun removeContainerIExistWhenInspectDropNotFoundException() {
        `when`(client!!.inspectContainerCmd(anyString())).thenThrow(NotFoundException("notfound"))
        `when`(createContainerCmd!!.name).thenReturn("ambari-server")

        createContainer(client, createContainerCmd, "vm1")

        verify(client, times(1)).inspectContainerCmd(anyString())
        verify(client, times(0)).removeContainerCmd(anyString())
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun removeContainerIExistWhenInspectDropActualException() {
        `when`(client!!.inspectContainerCmd(anyString())).thenThrow(IllegalArgumentException("illegal argument"))
        `when`(createContainerCmd!!.name).thenReturn("ambari-server")

        createContainer(client, createContainerCmd, "vm1")
    }

    @Test
    @Throws(Exception::class)
    fun removeContainerIExistWhenInspectReturnWithValueThenRemoveContainer() {
        `when`(client!!.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd)
        `when`(inspectContainerCmd!!.exec()).thenReturn(inspectContainerResponse)
        `when`(inspectContainerResponse!!.id).thenReturn("xx666xx")
        val state = mock<InspectContainerResponse.ContainerState>(InspectContainerResponse.ContainerState::class.java)
        `when`(inspectContainerResponse.state).thenReturn(state)
        `when`(state.isRunning).thenReturn(false)
        `when`(client.removeContainerCmd(anyString())).thenReturn(removeContainerCmd)
        `when`(removeContainerCmd!!.withForce(anyBoolean())).thenReturn(removeContainerCmd)
        `when`(removeContainerCmd.exec()).thenReturn(null)
        `when`(createContainerCmd!!.name).thenReturn("ambari-server")

        createContainer(client, createContainerCmd, "vm1")

        verify(client, times(1)).inspectContainerCmd(anyString())
        verify(client, times(1)).removeContainerCmd(anyString())
    }


    @Test
    @Throws(Exception::class)
    fun startContainerWhenEverythingWorksFine() {
        `when`(inspectContainerResponse!!.state).thenReturn(containerState)
        `when`(containerState!!.isRunning).thenReturn(true)
        `when`(inspectContainerCmd!!.exec()).thenReturn(inspectContainerResponse)
        `when`(client!!.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd)
        `when`(client.startContainerCmd(anyString())).thenReturn(startContainerCmd)
        startContainer(client, "OK")

        verify<StartContainerCmd>(startContainerCmd, times(1)).exec()
    }

    @Test(expected = CloudbreakOrchestratorFailedException::class)
    @Throws(Exception::class)
    fun startContainerButContainerNotStarted() {
        `when`(inspectContainerResponse!!.state).thenReturn(containerState)
        `when`(containerState!!.isRunning).thenReturn(false)
        `when`(inspectContainerCmd!!.exec()).thenReturn(inspectContainerResponse)
        `when`(client!!.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd)
        `when`(client.startContainerCmd(anyString())).thenReturn(startContainerCmd)

        startContainer(client, "OK")
    }

    @Test
    @Throws(Exception::class)
    fun createContainerWhenEverythingWorksFine() {
        `when`(createContainerResponse!!.id).thenReturn("xxx666xxx")
        `when`(client!!.inspectContainerCmd(anyString())).thenReturn(inspectContainerCmd)
        `when`(inspectContainerCmd!!.exec()).thenReturn(inspectContainerResponse)
        `when`(createContainerCmd!!.exec()).thenReturn(createContainerResponse)

        createContainer(client, createContainerCmd, "vm12")

        //No exception
    }


}