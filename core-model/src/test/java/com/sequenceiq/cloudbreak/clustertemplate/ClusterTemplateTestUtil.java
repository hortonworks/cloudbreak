package com.sequenceiq.cloudbreak.clustertemplate;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;

public final class ClusterTemplateTestUtil {

    private static final String TEST_ENVIRONMENT_NAME = "some-awesome-env";

    private ClusterTemplateTestUtil() {
    }

    public static ClusterTemplateV4Request createClusterTemplateV4RequestForAws() {
        return createClusterTemplateV4RequestFor(AWS);
    }

    public static ClusterTemplateV4Request createClusterTemplateV4RequestFor(CloudPlatform cloudPlatform) {
        ClusterTemplateV4Request r = new ClusterTemplateV4Request();
        r.setDistroXTemplate(createDistroXV1Request());
        r.setCloudPlatform(cloudPlatform.name());
        return r;
    }

    private static DistroXV1Request createDistroXV1Request() {
        DistroXV1Request r = new DistroXV1Request();
        r.setEnvironmentName(TEST_ENVIRONMENT_NAME);
        Set<InstanceGroupV1Request> instanceGroupV1Requests = new HashSet<>();
        InstanceGroupV1Request ig1 = new InstanceGroupV1Request();
        ig1.setType(InstanceGroupType.GATEWAY);
        InstanceGroupV1Request ig2 = new InstanceGroupV1Request();
        ig2.setType(InstanceGroupType.CORE);
        instanceGroupV1Requests.add(ig1);
        instanceGroupV1Requests.add(ig2);
        r.setInstanceGroups(instanceGroupV1Requests);
        return r;
    }

}
