package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.NameComparator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.UserIdComparator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.UserV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Controller
@DisableCheckPermissions
@Transactional(Transactional.TxType.NEVER)
public class WorkspaceV4Controller extends NotificationController implements WorkspaceV4Endpoint {

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public WorkspaceV4Responses list() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Set<Workspace> workspaces = workspaceService.retrieveForUser(user);
        return new WorkspaceV4Responses(workspacesToSortedResponse(workspaces));
    }

    @Override
    public WorkspaceV4Response get(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.getByNameForUserOrThrowNotFound(name, user);
        return converterUtil.convert(workspace, WorkspaceV4Response.class);
    }

    private SortedSet<WorkspaceV4Response> workspacesToSortedResponse(Set<Workspace> workspaces) {
        Set<WorkspaceV4Response> jsons = converterUtil.convertAllAsSet(workspaces, WorkspaceV4Response.class);
        SortedSet<WorkspaceV4Response> sortedResponses = new TreeSet<>(new NameComparator());
        sortedResponses.addAll(jsons);
        return sortedResponses;
    }

    private SortedSet<UserV4Response> usersToSortedResponse(Set<User> users) {
        Set<UserV4Response> jsons = converterUtil.convertAllAsSet(users, UserV4Response.class);
        SortedSet<UserV4Response> sortedResponses = new TreeSet<>(new UserIdComparator());
        sortedResponses.addAll(jsons);
        return sortedResponses;
    }

}
