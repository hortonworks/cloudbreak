package com.sequenceiq.distrox.v1.distrox.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.distrox.api.v1.distrox.model.sharedservice.SdxV1Request;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class SdxConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxConverter.class);

    public SharedServiceV4Request getSharedService(SdxClusterResponse sdx) {
        if (sdx == null) {
            LOGGER.info("We don't attach the cluster to any Datalake, because the environment has not SDX. So we continue the creation as simple WORKLOAD.");
            return null;
        }
        if (sdx.getStatus() != SdxClusterStatusResponse.RUNNING) {
            throw new BadRequestException(
                    String.format("Your current Environment %s contains one Data Lake the name of which is %s. " +
                            "This Data Lake should be in running/available state but currently it is in '%s' instead of Running. " +
                            "Please make sure your Data Lake is up and running before you provision the Data Hub. " +
                            "If your Data Lake is in stopped state, please restart it. " +
                            "If your Data Lake has failed to provision please check our documentation " +
                            "https://docs.cloudera.com/management-console/cloud/data-lakes/topics/mc-data-lake.html or contact " +
                            "the Cloudera support to get some help or try to provision a new Data Lake with the " +
                            "correct configuration.", sdx.getEnvironmentName(),  sdx.getName(), sdx.getStatus()));
        }
        return getSharedServiceV4Request(sdx);
    }

    public SdxV1Request getSdx(SharedServiceV4Request sharedServiceV4Request) {
        SdxV1Request sdx = new SdxV1Request();
        sdx.setName(sharedServiceV4Request.getDatalakeName());
        return sdx;
    }

    public SharedServiceV4Request getSharedServiceV4Request(SdxClusterResponse sdx) {
        SharedServiceV4Request sharedServiceV4Request = new SharedServiceV4Request();
        sharedServiceV4Request.setDatalakeName(sdx.getName());
        sharedServiceV4Request.setRuntimeVersion(sdx.getRuntime());
        return sharedServiceV4Request;
    }
}
