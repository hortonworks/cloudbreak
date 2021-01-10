package com.sequenceiq.cloudbreak.clustertemplate;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;

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
        return r;
    }

}
