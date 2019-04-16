package com.sequenceiq.cloudbreak.service.securitygroup;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.SecurityGroupRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class SecurityGroupService extends AbstractWorkspaceAwareResourceService<SecurityGroup> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityGroupService.class);

    @Inject
    private SecurityGroupRepository groupRepository;

    @Inject
    private InstanceGroupService instanceGroupService;

    public SecurityGroup create(User user, SecurityGroup securityGroup) {
        LOGGER.debug("Creating SecurityGroup: [User: '{}']", user.getUserId());
        try {
            return groupRepository.save(securityGroup);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.SECURITY_GROUP, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public SecurityGroup get(Long id) {
        return groupRepository.findById(id).orElseThrow(notFound("SecurityGroup", id));
    }

    public void delete(Long id) {
        delete(get(id));
    }

    public void delete(String name, Workspace workspace) {
        SecurityGroup securityGroup = groupRepository.findByNameAndWorkspace(name, workspace).orElseThrow(notFound("SecurityGroup", name));
        deleteImpl(securityGroup);
    }

    public void deleteImpl(SecurityGroup securityGroup) {
        LOGGER.debug("Deleting SecurityGroup with name: {}", securityGroup.getName());
        List<InstanceGroup> instanceGroupsWithThisSecurityGroup = new ArrayList<>(instanceGroupService.findBySecurityGroup(securityGroup));
        if (!instanceGroupsWithThisSecurityGroup.isEmpty()) {
            if (instanceGroupsWithThisSecurityGroup.size() > 1) {
                String clusters = instanceGroupsWithThisSecurityGroup
                        .stream()
                        .map(instanceGroup -> instanceGroup.getStack().getCluster().getName())
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with SecurityGroup '%s'(ID:'%d'). Please remove these before deleting the SecurityGroup. "
                                + "The following clusters are using this SecurityGroup: [%s]",
                        securityGroup.getName(), securityGroup.getId(), clusters));
            } else {
                throw new BadRequestException(String.format("There is a cluster ['%s'] which uses SecurityGroup '%s'(ID:'%d'). Please remove this "
                                + "cluster before deleting the SecurityGroup",
                        instanceGroupsWithThisSecurityGroup.get(0).getStack().getCluster().getName(), securityGroup.getName(), securityGroup.getId()));
            }
        }
        if (ResourceStatus.USER_MANAGED.equals(securityGroup.getStatus())) {
            groupRepository.delete(securityGroup);
        } else {
            securityGroup.setName(NameUtil.postfixWithTimestamp(securityGroup.getName()));
            securityGroup.setStatus(ResourceStatus.DEFAULT_DELETED);
            groupRepository.save(securityGroup);
        }
    }

    @Override
    public WorkspaceResourceRepository<SecurityGroup, Long> repository() {
        return groupRepository;
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.STACK;
    }

    @Override
    protected void prepareDeletion(SecurityGroup resource) {

    }

    @Override
    protected void prepareCreation(SecurityGroup resource) {

    }

    public SecurityGroup save(SecurityGroup securityGroup) {
        return repository().save(securityGroup);
    }

    public Optional<SecurityGroup> findById(Long id) {
        return repository().findById(id);
    }

}
