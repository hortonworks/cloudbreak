package com.sequenceiq.distrox.v1.distrox.converter;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.domain.projection.StackStatusView;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.distrox.api.v1.distrox.model.sharedservice.SdxV1Request;

@Component
public class SdxConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxConverter.class);

    @Inject
    private StackService stackService;

    public SharedServiceV4Request getSharedService(SdxV1Request sdx) {
        return getSharedServiceV4Request(sdx.getName());
    }

    public SharedServiceV4Request getSharedService(SdxV1Request sdx, String environemntCrn) {
        List<StackStatusView> datalakes = stackService.getByEnvironmentCrnAndStackType(environemntCrn, StackType.DATALAKE);
        StringBuilder name = new StringBuilder();
        if (sdx == null) {
            if (datalakes.isEmpty()) {
                LOGGER.info("We don't attach the cluster to any Datalake, because the environemnt has not SDX. So we continue the creation as simple WORKLOAD.");
                return null;
            } else if (datalakes.size() > 1) {
                throw new BadRequestException("More than one Datalake attached to the environment. Please specify one.");
            }
            name.append(datalakes.get(0).getName());
        } else {
            name.append(sdx.getName());
        }
        StackStatusView datalake = datalakes.stream()
                .filter(sr -> sr.getName().equals(name.toString()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("The given Datalake not attached to the environment"));
        if (datalake.getStatus().getStatus() != Status.AVAILABLE) {
            throw new BadRequestException(String.format("Datalake status is invalid. Current state is %s instead of %s", datalake.getStatus().getStatus().name(),
                    Status.AVAILABLE));
        }
        return getSharedServiceV4Request(name.toString());
    }

    public SdxV1Request getSdx(SharedServiceV4Request sharedServiceV4Request) {
        SdxV1Request sdx = new SdxV1Request();
        sdx.setName(sharedServiceV4Request.getDatalakeName());
        return sdx;
    }

    public SharedServiceV4Request getSharedServiceV4Request(String name) {
        SharedServiceV4Request sharedServiceV4Request = new SharedServiceV4Request();
        sharedServiceV4Request.setDatalakeName(name);
        return sharedServiceV4Request;
    }
}
