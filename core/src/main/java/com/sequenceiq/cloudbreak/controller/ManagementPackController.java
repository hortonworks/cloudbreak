package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.ManagementPackEndpoint;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackRequest;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackResponse;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.mpack.ManagementPackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
@Transactional(TxType.NEVER)
public class ManagementPackController extends NotificationController implements ManagementPackEndpoint {

    @Inject
    private ManagementPackService mpackService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public ManagementPackResponse get(Long id) {
        return conversionService.convert(mpackService.getById(id), ManagementPackResponse.class);
    }

    private String getMpackName(Long id) {
        return mpackService.getById(id).getName();
    }

    @Override
    public void delete(Long id) {
        ManagementPack managementPack = mpackService.getById(id);
        executeAndNotify(user -> mpackService.delete(managementPack),
                ResourceEvent.MANAGEMENT_PACK_DELETED);
    }

    @Override
    public ManagementPackResponse postPrivate(ManagementPackRequest mpackRequest) {
        return post(mpackRequest);
    }

    @Override
    public Set<ManagementPackResponse> getPrivates() {
        return getAll();
    }

    @Override
    public ManagementPackResponse getPrivate(String name) {
        return getByName(name);
    }

    @Override
    public void deletePrivate(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        executeAndNotify(identityUser -> mpackService.deleteByNameFromWorkspace(name, workspace.getId()), ResourceEvent.MANAGEMENT_PACK_DELETED);
    }

    @Override
    public ManagementPackResponse postPublic(ManagementPackRequest mpackRequest) {
        return post(mpackRequest);
    }

    @Override
    public Set<ManagementPackResponse> getPublics() {
        return getAll();
    }

    @Override
    public ManagementPackResponse getPublic(String name) {
        return getByName(name);
    }

    @Override
    public void deletePublic(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        executeAndNotify(identityUser -> mpackService.deleteByNameFromWorkspace(name, workspace.getId()), ResourceEvent.MANAGEMENT_PACK_DELETED);
    }

    private ManagementPackResponse createMpack(User user, ManagementPackRequest mpackRequest) {
        ManagementPack mpack = conversionService.convert(mpackRequest, ManagementPack.class);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        try {
            mpack = mpackService.create(mpack, workspace, user);
            notify(ResourceEvent.MANAGEMENT_PACK_CREATED);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.MANAGEMENT_PACK, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
        return conversionService.convert(mpack, ManagementPackResponse.class);
    }

    private Set<ManagementPackResponse> toJsonList(Set<ManagementPack> mpacks) {
        return (Set<ManagementPackResponse>) conversionService.convert(mpacks,
                TypeDescriptor.forObject(mpacks),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(ManagementPackResponse.class)));
    }

    private ManagementPackResponse post(ManagementPackRequest mpackRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return createMpack(user, mpackRequest);
    }

    private Set<ManagementPackResponse> getAll() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return toJsonList(mpackService.findAllByWorkspace(workspace));
    }

    private ManagementPackResponse getByName(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return conversionService.convert(mpackService.getByNameForWorkspace(name, workspace), ManagementPackResponse.class);
    }
}
