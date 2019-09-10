package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.authorization.AuthorizationGrpc;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;

import io.grpc.ManagedChannel;

/**
 * A simple wrapper to the GRPC user management service. This handles setting up
 * the appropriate context-propogatinng interceptors and hides some boilerplate.
 */
public class AuthorizationClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    /**
     * Constructor.
     *
     * @param channel  the managed channel.
     * @param actorCrn the actor CRN.
     */
    AuthorizationClient(ManagedChannel channel,
            String actorCrn) {
        this.channel = checkNotNull(channel);
        this.actorCrn = checkNotNull(actorCrn);
    }

    public void checkRight(String requestId, String userCrn, String right, String resource) {
        checkNotNull(requestId);
        checkNotNull(userCrn);
        checkNotNull(right);
        AuthorizationProto.RightCheck.Builder rightCheckBuilder = AuthorizationProto.RightCheck.newBuilder().setRight(right);
        if (!StringUtils.isEmpty(resource)) {
            rightCheckBuilder.setResource(resource);
        }
        newStub(requestId).checkRight(
                AuthorizationProto.CheckRightRequest.newBuilder()
                        .setActorCrn(userCrn)
                        .setCheck(rightCheckBuilder.build())
                        .build()
        );
    }

    // TODO POC

    public Table hasRights(String requestId, String userCrn, Map<String, List<String>> resourceRightMap) {
        checkNotNull(requestId);
        checkNotNull(userCrn);
        checkNotNull(resourceRightMap);

        AuthorizationProto.HasRightsRequest.Builder builder = AuthorizationProto.HasRightsRequest.newBuilder().setActorCrn(userCrn);
        List<AuthorizationProto.RightCheck> rightChecks = Lists.newArrayList();
        resourceRightMap.keySet().stream().forEach(resource -> resourceRightMap.get(resource).stream().forEach(right ->
                rightChecks.add(AuthorizationProto.RightCheck.newBuilder().setRight(right).setResource(resource).build())));
        builder.addAllCheck(rightChecks);

        AuthorizationProto.HasRightsResponse hasRightsResponse = newStub(requestId).hasRights(builder.build());
        Iterator<Boolean> resultIterator = hasRightsResponse.getResultList().iterator();

        // Table <resource, right, boolean result>
        Table<String, String, Boolean> resultTable = HashBasedTable.create();
        resourceRightMap.keySet().stream().forEach(resource -> resourceRightMap.get(resource).stream().forEach(right ->
                resultTable.put(resource, right, resultIterator.next())));
        return resultTable;
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private AuthorizationGrpc.AuthorizationBlockingStub newStub(String requestId) {
        checkNotNull(requestId);
        return AuthorizationGrpc.newBlockingStub(channel)
                .withInterceptors(new AltusMetadataInterceptor(requestId, actorCrn));
    }
}
