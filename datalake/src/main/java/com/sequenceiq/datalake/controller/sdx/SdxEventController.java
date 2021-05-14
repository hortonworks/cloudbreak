package com.sequenceiq.datalake.controller.sdx;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Responses;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.api.endpoint.SdxEventEndpoint;
import com.sequenceiq.sdx.api.model.event.SdxEventResponse;
import com.sequenceiq.sdx.api.model.event.SdxEventResponses;

@Controller
public class SdxEventController implements SdxEventEndpoint {

    @Inject
    private EventV4Endpoint eventV4Endpoint;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxEventConverter sdxEventConverter;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public SdxEventResponses list(Long since) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        List<Long> sdxCbStackIdsInAccount = sdxService.findByAccountIdAndDeletedIsNull().stream()
                .map(SdxCluster::getStackId).collect(Collectors.toList());
        CloudbreakEventV4Responses cloudbreakEventV4Responses = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                eventV4Endpoint.list(since, accountId));
        return new SdxEventResponses(cloudbreakEventV4Responses.getResponses().stream()
                    .filter(cbEventResponse -> sdxCbStackIdsInAccount.contains(cbEventResponse.getClusterId()))
                    .map(cbEventResponse -> sdxEventConverter.convert(cbEventResponse))
                    .collect(Collectors.toList()));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATALAKE)
    public Page<SdxEventResponse> getEventsByCrn(@ResourceCrn String crn, Integer page, Integer size) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String name = sdxService.getByCrn(ThreadBasedUserCrnProvider.getUserCrn(), crn).getClusterName();
        return ThreadBasedUserCrnProvider.doAsInternalActor(() -> {
            Page<CloudbreakEventV4Response> cloudbreakEventsByStack = eventV4Endpoint.getCloudbreakEventsByStack(name, page, size, accountId);
            return cloudbreakEventsByStack.map(cbResponse -> sdxEventConverter.convert(cbResponse));
        });
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATALAKE)
    public StructuredEventContainer structured(@ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String name = sdxService.getByCrn(ThreadBasedUserCrnProvider.getUserCrn(), crn).getClusterName();
        return ThreadBasedUserCrnProvider.doAsInternalActor(() -> eventV4Endpoint.structured(name, accountId));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATALAKE)
    public Response download(@ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String name = sdxService.getByCrn(ThreadBasedUserCrnProvider.getUserCrn(), crn).getClusterName();
        return ThreadBasedUserCrnProvider.doAsInternalActor(() -> eventV4Endpoint.download(name, accountId));
    }
}
