package com.sequenceiq.cloudbreak.cloud.arm.task

import javax.inject.Inject

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.arm.ArmClient
import com.sequenceiq.cloudbreak.cloud.arm.context.NetworkInterfaceCheckerContext
import com.sequenceiq.cloudbreak.cloud.arm.context.ResourceGroupCheckerContext
import com.sequenceiq.cloudbreak.cloud.arm.context.StorageCheckerContext
import com.sequenceiq.cloudbreak.cloud.arm.context.VirtualMachineCheckerContext
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.task.PollTask

@Component
class ArmPollTaskFactory {
    @Inject
    private val applicationContext: ApplicationContext? = null

    fun newNetworkInterfaceDeleteStatusCheckerTask(authenticatedContext: AuthenticatedContext, armClient: ArmClient,
                                                   networkInterfaceCheckerContext: NetworkInterfaceCheckerContext): PollTask<Boolean> {
        return createPollTask(ArmNetworkInterfaceDeleteStatusCheckerTask.NAME, authenticatedContext, armClient, networkInterfaceCheckerContext)
    }

    fun newResourceGroupDeleteStatusCheckerTask(authenticatedContext: AuthenticatedContext, armClient: ArmClient, resourceGroupDeleteCheckerContext: ResourceGroupCheckerContext): PollTask<Boolean> {
        return createPollTask(ArmResourceGroupDeleteStatusCheckerTask.NAME, authenticatedContext, armClient, resourceGroupDeleteCheckerContext)
    }

    fun newStorageStatusCheckerTask(authenticatedContext: AuthenticatedContext, storageCheckerContext: StorageCheckerContext): PollTask<Boolean> {
        return createPollTask(ArmStorageStatusCheckerTask.NAME, authenticatedContext, storageCheckerContext)
    }

    fun newVirtualMachineDeleteStatusCheckerTask(authenticatedContext: AuthenticatedContext, armClient: ArmClient,
                                                 virtualMachineCheckerContext: VirtualMachineCheckerContext): PollTask<Boolean> {
        return createPollTask(ArmVirtualMachineDeleteStatusCheckerTask.NAME, authenticatedContext, armClient, virtualMachineCheckerContext)
    }

    fun newVirtualMachineStatusCheckerTask(authenticatedContext: AuthenticatedContext, armClient: ArmClient, virtualMachineCheckerContext: VirtualMachineCheckerContext): PollTask<Boolean> {
        return createPollTask(ArmVirtualMachineStatusCheckerTask.NAME, authenticatedContext, armClient, virtualMachineCheckerContext)
    }

    @SuppressWarnings("unchecked")
    private fun <T> createPollTask(name: String, vararg args: Any): T {
        return applicationContext!!.getBean(name, *args) as T
    }
}
