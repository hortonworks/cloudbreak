package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ClusterTemplateV3EndPoint;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateViewResponse;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateViewService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.ConverterUtil;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ClusterTemplate.class)
public class ClusterTemplateV3Controller extends NotificationController implements ClusterTemplateV3EndPoint {

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ClusterTemplateService clusterTemplateService;

    @Inject
    private ClusterTemplateViewService clusterTemplateViewService;

    @Inject
    private TransactionService transactionService;

    @Override
    public ClusterTemplateResponse createInWorkspace(Long workspaceId, @Valid ClusterTemplateRequest request) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        ClusterTemplate clusterTemplate = clusterTemplateService.create(converterUtil.convert(request, ClusterTemplate.class), workspaceId, user);
        return getByNameInWorkspace(workspaceId, clusterTemplate.getName());
    }

    @Override
    public Set<ClusterTemplateViewResponse> listByWorkspace(Long workspaceId) {
        try {
            return transactionService.required(() ->
                    converterUtil.convertAllAsSet(clusterTemplateViewService.findAllByWorkspaceId(workspaceId), ClusterTemplateViewResponse.class));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @Override
    public ClusterTemplateResponse getByNameInWorkspace(Long workspaceId, String name) {
        try {
            return transactionService.required(() ->
                    converterUtil.convert(clusterTemplateService.getByNameForWorkspaceId(name, workspaceId), ClusterTemplateResponse.class));
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @Override
    public void deleteInWorkspace(Long workspaceId, String name) {
        clusterTemplateService.delete(name, workspaceId);
    }
}
