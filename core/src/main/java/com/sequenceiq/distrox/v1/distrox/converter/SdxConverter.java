package com.sequenceiq.distrox.v1.distrox.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.distrox.api.v1.distrox.model.sharedservice.SdxV1Request;

@Component
public class SdxConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxConverter.class);

    public SharedServiceV4Request getSharedService(SdxBasicView sdx) {
        if (sdx == null) {
            LOGGER.info("We don't attach the cluster to any Datalake, because the environment has not SDX. So we continue the creation as simple WORKLOAD.");
            return null;
        }
        return getSharedServiceV4Request(sdx);
    }

    public SdxV1Request getSdx(SharedServiceV4Request sharedServiceV4Request) {
        SdxV1Request sdx = new SdxV1Request();
        sdx.setName(sharedServiceV4Request.getDatalakeName());
        return sdx;
    }

    public SharedServiceV4Request getSharedServiceV4Request(SdxBasicView sdx) {
        SharedServiceV4Request sharedServiceV4Request = new SharedServiceV4Request();
        sharedServiceV4Request.setDatalakeName(sdx.name());
        sharedServiceV4Request.setRuntimeVersion(sdx.runtime());
        return sharedServiceV4Request;
    }
}
