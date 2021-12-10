package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

/**
 * This interface includes operations that can be executed on individual instances.
 */
public interface InstanceConnector {

    /**
     * Start instances. You can start instances trough this method. It does not need to wait/block until the VM instances are started, but it can return
     * immediately and the {@link #check(AuthenticatedContext, List)} method is invoked to check regularly whether the VM instances have already been started
     * or not.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param resources            resources managed by Cloudbreak, can be used to figure out which resources are associated with the given VMs
     *                             (e.g. floating IP) and they can be started as well
     * @param vms                  VM instances to be started
     * @return status of instances
     */
    List<CloudVmInstanceStatus> start(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms);

    /**
     * A version of instance start which limits the retries that the operation may normally perform.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param resources            resources managed by Cloudbreak, can be used to figure out which resources are associated with the given VMs
     *                             (e.g. floating IP) and they can be started as well
     * @param vms                  VM instances to be started
     * @param timeboundInMs        A timebound in ms for this call.
     * @return status of instances
     */
    default List<CloudVmInstanceStatus> startWithLimitedRetry(AuthenticatedContext authenticatedContext, List<CloudResource> resources,
            List<CloudInstance> vms, Long timeboundInMs) {
        return start(authenticatedContext, resources, vms);
    }

    /**
     * Stop instances. You can stop instances trough this method. It does not need to wait/block until the VM instances are stopped, but it can return
     * immediately and the {@link #check(AuthenticatedContext, List)} method is invoked to check regularly whether the VM instances have already been stopped
     * or not.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param resources            resources managed by Cloudbreak, can be used to figure out which resources are associated with the given VMs
     *                             (e.g. floating IP) and they can be stopped as well
     * @param vms                  VM instances to be stopped
     * @return status of instances
     */
    List<CloudVmInstanceStatus> stop(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms);

    /**
     * Reboot instances. You can reboot instances through this method. It does not need to wait/block until the VM instances are rebooted, but it can return
     * immediately and the {@link #check(AuthenticatedContext, List)} method is invoked to check regularly whether the VM instances have already been started
     * after a reboot or not.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param instanceIds          a VM instances that need to be rebooted.
     * @return status of instances
     */
    List<CloudVmInstanceStatus> reboot(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> instanceIds);

    /**
     * Invoked to check whether the instances have already reached a StatusGroup.PERMANENT state.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param vms                  the VM instances for which the status needs to be checked
     * @return status of instances
     */
    List<CloudVmInstanceStatus> check(AuthenticatedContext authenticatedContext, List<CloudInstance> vms);

    /**
     * Invoked to check whether the instances have already reached a StatusGroup.PERMANENT state. (Retry logic won't be used in this case)
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param vms                  the VM instances for which the status needs to be checked
     * @return status of instances
     */
    default List<CloudVmInstanceStatus> checkWithoutRetry(AuthenticatedContext authenticatedContext, List<CloudInstance> vms) {
        return check(authenticatedContext, vms);
    }

    /**
     * Gets the Consol output of a particular VM, useful for debugging and also required for setting up a secure connection between Cloudbreak and VM instances
     * since the SSH fingerprint is written into the console output.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param vm                   the VM instance
     * @return the consol output as text
     */
    String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm);

}
