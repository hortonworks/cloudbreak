package com.sequenceiq.thunderhead.grpc.service.auth;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewGrpc;
import com.cloudera.thunderhead.service.personalresourceview.PersonalResourceViewProto;
import com.google.common.base.Strings;

import io.grpc.stub.StreamObserver;

@Service
public class MockPersonalResourceViewService extends PersonalResourceViewGrpc.PersonalResourceViewImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockPersonalResourceViewService.class);

    @Override
    public void hasResourcesByRight(PersonalResourceViewProto.HasResourcesByRightRequest request,
            StreamObserver<PersonalResourceViewProto.HasResourcesByRightResponse> responseObserver) {
        LOGGER.info("Has rights for {}, right {}, resource count: {}, ", request.getUserCrn(), request.getRight(), request.getResourceCount());
        checkArgument(!Strings.isNullOrEmpty(request.getUserCrn()));
        checkNotNull(request.getRight());
        checkArgument(CollectionUtils.isNotEmpty(request.getResourceList()));

        PersonalResourceViewProto.HasResourcesByRightResponse.Builder builder = PersonalResourceViewProto.HasResourcesByRightResponse.newBuilder();
        request.getResourceList().forEach(resource -> {
            LOGGER.info("Add result true for has {} right on {}", request.getRight(), resource);
            builder.addResult(true);
        });
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
