package com.sequenceiq.cloudbreak.cloud

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus

/**
 * This interface includes operations that can be executed on individual instances.
 */
interface InstanceConnector {

    /**
     * Start instances. You can start instances trough this method. It does not need to wait/block until the VM instances are started, but it can return
     * immediately and the [.check] method is invoked to check regularly whether the VM instances have already been started
     * or not.

     * @param authenticatedContext the authenticated context which holds the client object
     * *
     * @param resources            resources managed by Cloudbreak, can be used to figure out which resources are associated with the given VMs
     * *                             (e.g. floating IP) and they can be started as well
     * *
     * @param vms                  VM instances to be started
     * *
     * @return status of instances
     * *
     * @throws Exception in case of any error
     */
    @Throws(Exception::class)
    fun start(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmInstanceStatus>

    /**
     * Stop instances. You can start instances trough this method. It does not need to wait/block until the VM instances are stopped, but it can return
     * immediately and the [.check] method is invoked to check regularly whether the VM instances have already been stopped
     * or not.

     * @param authenticatedContext the authenticated context which holds the client object
     * *
     * @param resources            resources managed by Cloudbreak, can be used to figure out which resources are associated with the given VMs
     * *                             (e.g. floating IP) and they can be stopped as well
     * *
     * @param vms                  VM instances to be stopped
     * *
     * @return status of instances
     * *
     * @throws Exception in case of any error
     */
    @Throws(Exception::class)
    fun stop(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmInstanceStatus>

    /**
     * Invoked to check whether the instances have already reached a StatusGroup.PERMANENT state.

     * @param authenticatedContext the authenticated context which holds the client object
     * *
     * @param vms                  the VM instances for which the status needs to be checked
     * *
     * @return status of instances
     */
    fun check(authenticatedContext: AuthenticatedContext, vms: List<CloudInstance>): List<CloudVmInstanceStatus>

    /**
     * Gets the Consol output of a particular VM, useful for debugging and also required for setting up a secure connection between Cloudbreak and VM instances
     * since the SSH fingerprint is written into the console output.

     * @param authenticatedContext the authenticated context which holds the client object
     * *
     * @param vm                   the VM instance
     * *
     * @return the consol output as text
     */
    fun getConsoleOutput(authenticatedContext: AuthenticatedContext, vm: CloudInstance): String

}
