package com.sequenceiq.cloudbreak.workspace.authorization;

import java.lang.annotation.Annotation;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.repository.DisableCheckPermissions;
import com.sequenceiq.authorization.service.AbstractPermissionCheckerService;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByTargetId;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspace;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspaceCrn;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByWorkspaceId;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;

@Service
public class PermissionCheckerService extends AbstractPermissionCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCheckerService.class);

    private static final List<Class<? extends Annotation>> POSSIBLE_METHOD_ANNOTATIONS = List.of(CheckPermissionsByWorkspace.class,
            CheckPermissionsByWorkspaceId.class, CheckPermissionsByTarget.class, CheckPermissionsByTargetId.class,
            CheckPermissionsByReturnValue.class, DisableCheckPermissions.class, CheckPermissionsByWorkspaceCrn.class);

    @Override
    protected List<Class<? extends Annotation>> getPossibleMethodAnnotations() {
        return POSSIBLE_METHOD_ANNOTATIONS;
    }

    @Override
    protected List<Class> getRepositoryClass() {
        return Lists.newArrayList(WorkspaceResourceRepository.class);
    }
}
