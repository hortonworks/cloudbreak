package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ClusterTemplate.class)
public class ClusterTemplateV4Controller extends NotificationController implements ClusterTemplateV4Endpoint {

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ClusterTemplateService clusterTemplateService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private TransactionService transactionService;

    @Override
    public ClusterTemplateV4Response post(Long workspaceId, @Valid ClusterTemplateV4Request request) {
        ClusterTemplate clusterTemplate = clusterTemplateService.createForLoggedInUser(converterUtil.convert(request, ClusterTemplate.class), workspaceId);
        return get(workspaceId, clusterTemplate.getName());
    }

    @Override
    public ClusterTemplateViewV4Responses list(Long workspaceId) {
        try {
            blueprintService.updateDefaultBlueprints(workspaceId);
            clusterTemplateService.updateDefaultClusterTemplates(workspaceId);
            Set<ClusterTemplateViewV4Response> responses = transactionService.required(() ->
                    converterUtil.convertAllAsSet(clusterTemplateService.getAllAvailableViewInWorkspace(workspaceId), ClusterTemplateViewV4Response.class));
            return new ClusterTemplateViewV4Responses(responses);
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @Override
    public ClusterTemplateV4Response get(Long workspaceId, String name) {
        try {
            return transactionService.required(() ->
                    converterUtil.convert(clusterTemplateService.getByNameForWorkspaceId(name, workspaceId), ClusterTemplateV4Response.class));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @Override
    public ClusterTemplateV4Response delete(Long workspaceId, String name) {
        ClusterTemplate clusterTemplate = clusterTemplateService.delete(name, workspaceId);
        return converterUtil.convert(clusterTemplate, ClusterTemplateV4Response.class);
    }

    @Override
    public ClusterTemplateV4Responses deleteMultiple(Long workspaceId, Set<String> names) {
        Set<ClusterTemplate> clusterTemplates = clusterTemplateService.deleteMultiple(names, workspaceId);
        return new ClusterTemplateV4Responses(converterUtil.convertAllAsSet(clusterTemplates, ClusterTemplateV4Response.class));
    }
}
