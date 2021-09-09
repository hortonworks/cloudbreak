package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ClouderaManagerStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.cloud.model.component.ClouderaManagerDefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;

@Component
public class StackInfoToClouderaManagerStackDescriptorV4ResponseConverter {

    public ClouderaManagerStackDescriptorV4Response convert(DefaultCDHInfo source) {
        ClouderaManagerStackDescriptorV4Response stackDescriptorV4 = new ClouderaManagerStackDescriptorV4Response();
        stackDescriptorV4.setVersion(source.getVersion());
        stackDescriptorV4.setRepository(defaultStackRepoDetailsToStackRepoDetailsV4Response(source.getRepo()));
        return stackDescriptorV4;
    }

    private ClouderaManagerStackRepoDetailsV4Response defaultStackRepoDetailsToStackRepoDetailsV4Response(
            ClouderaManagerDefaultStackRepoDetails cmDefaultStackRepoDetails) {
        if (cmDefaultStackRepoDetails == null) {
            return null;
        }
        ClouderaManagerStackRepoDetailsV4Response cmStackRepoDetailsV4Response = new ClouderaManagerStackRepoDetailsV4Response();
        cmStackRepoDetailsV4Response.setStack(cmDefaultStackRepoDetails.getStack());
        return cmStackRepoDetailsV4Response;
    }
}
