package com.sequenceiq.cloudbreak.cloud.template;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

/**
 * Create a generic context object which will be passed along with the resource creation / deletion / stop / start phases. It can be dynamically
 * extended in each phase if there are required information to be re-used in a later phase. For example the context can be extended with the different
 * resources like network resources which later required to create the compute resources. It is generally advised to group these resources by private id
 * which is provided and used by Cloudbreak only.
 */
public interface ResourceContextBuilder<C extends ResourceBuilderContext> extends CloudPlatformAware {

    /**
     * Initialize the context before the first resource builder is called.
     *
     * @param cloudContext Context for the specific cloud stack.
     * @param auth         Authenticated context is provided to be able to send the requests to the cloud provider.
     * @param resources    The context can be initialized with base resources.
     * @param build        Provides a simple boolean flag used to determine creation/deletion or stop/start
     * @return Returns the initialized context object.
     */
    C contextInit(CloudContext cloudContext, AuthenticatedContext auth, List<CloudResource> resources, boolean build);

}
