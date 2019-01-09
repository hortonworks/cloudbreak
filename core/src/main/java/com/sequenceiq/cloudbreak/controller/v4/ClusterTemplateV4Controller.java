package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.ClusterTemplateV4EndPoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.controller.NotificationController;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.ConverterUtil;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ClusterTemplate.class)
public class ClusterTemplateV4Controller extends NotificationController implements ClusterTemplateV4EndPoint {

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ClusterTemplateService clusterTemplateService;

    @Inject
    private TransactionService transactionService;

    @Override
    public ClusterTemplateV4Response post(Long workspaceId, @Valid ClusterTemplateV4Request request) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        ClusterTemplate clusterTemplate = clusterTemplateService.create(converterUtil.convert(request, ClusterTemplate.class), workspaceId, user);
        return get(workspaceId, clusterTemplate.getName());
    }

    @Override
    public Set<ClusterTemplateV4Response> list(Long workspaceId) {
        try {
            return transactionService.required(() ->
                    converterUtil.convertAllAsSet(clusterTemplateService.findAllByWorkspaceId(workspaceId), ClusterTemplateV4Response.class));
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
}
