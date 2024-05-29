package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewGrpc;
import com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.altus.CallingServiceNameInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.grpc.ManagedChannel;

public class PersonalResourceViewClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonalResourceViewClient.class);

    private final ManagedChannel channel;

    private final UmsClientConfig umsClientConfig;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    PersonalResourceViewClient(ManagedChannel channel, UmsClientConfig umsClientConfig,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.channel = channel;
        this.umsClientConfig = umsClientConfig;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public List<Boolean> hasResourcesByRight(String actorCrn, String right, Iterable<String> resources) {
        checkNotNull(actorCrn, "actorCrn should not be null.");
        checkNotNull(resources, "resources should not be null.");
        return newStub()
                .hasResourcesByRight(
                        PersonalResourceViewProto.HasResourcesByRightRequest
                                .newBuilder()
                                .setUserCrn(actorCrn)
                                .setRight(right)
                                .addAllResource(resources)
                                .build())
                .getResultList();
    }

    private PersonalResourceViewGrpc.PersonalResourceViewBlockingStub newStub() {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        return PersonalResourceViewGrpc.newBlockingStub(channel).withInterceptors(
                GrpcUtil.getTimeoutInterceptor(umsClientConfig.getGrpcShortTimeoutSec()),
                new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()),
                new CallingServiceNameInterceptor(umsClientConfig.getCallingServiceName())
        );
    }
}
