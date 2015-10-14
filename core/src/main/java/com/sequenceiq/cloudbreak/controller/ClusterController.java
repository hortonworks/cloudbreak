package com.sequenceiq.cloudbreak.controller;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.controller.doc.ContentType;
import com.sequenceiq.cloudbreak.controller.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.controller.doc.Notes;
import com.sequenceiq.cloudbreak.controller.doc.OperationDescriptions.ClusterOpDescription;
import com.sequenceiq.cloudbreak.controller.json.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.FileSystemRequest;
import com.sequenceiq.cloudbreak.controller.json.HostGroupJson;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.controller.json.UpdateClusterJson;
import com.sequenceiq.cloudbreak.controller.json.UserNamePasswordJson;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.decorator.Decorator;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@Api(value = "/cluster", description = ControllerDescription.CLUSTER_DESCRIPTION, position = 4)
public class ClusterController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private Decorator<Cluster> clusterDecorator;

    @Inject
    private Decorator<HostGroup> hostGroupDecorator;

    @Inject
    private ClusterService clusterService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private StackService stackService;

    @Inject
    private JsonHelper jsonHelper;

    @ApiOperation(value = ClusterOpDescription.POST_FOR_STACK, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    @RequestMapping(value = "/stacks/{stackId}/cluster", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> create(@ModelAttribute("user") CbUser user, @PathVariable Long stackId, @RequestBody @Valid ClusterRequest request) {
        if (request.getEnableSecurity()
                && (request.getKerberosMasterKey() == null || request.getKerberosAdmin() == null || request.getKerberosPassword() == null)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        MDCBuilder.buildUserMdcContext(user);
        if (request.getFileSystem() != null) {
            validateFilesystemRequest(request.getFileSystem());
        }
        Cluster cluster = conversionService.convert(request, Cluster.class);
        cluster = clusterDecorator.decorate(cluster, stackId, request.getBlueprintId(), request.getHostGroups(), request.getValidateBlueprint());
        clusterService.create(user, stackId, cluster);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    private void validateFilesystemRequest(FileSystemRequest fileSystemRequest) {
        ObjectMapper mapper = new ObjectMapper();
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        try {
            if (fileSystemRequest != null) {
                String json = mapper.writeValueAsString(fileSystemRequest.getProperties());
                Object fsConfig = mapper.readValue(json, fileSystemRequest.getType().getClazz());
                Set<ConstraintViolation<Object>> violations = validator.validate(fsConfig);
                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException(violations);
                }
            }
        } catch (IOException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    @ApiOperation(value = ClusterOpDescription.GET_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    @RequestMapping(value = "/stacks/{stackId}/cluster", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ClusterResponse> retrieveCluster(@ModelAttribute("user") CbUser user, @PathVariable Long stackId) {
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.get(stackId);
        Cluster cluster = clusterService.retrieveClusterForCurrentUser(stackId);
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stackId);
        ClusterResponse response = getClusterResponse(cluster, clusterJson);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ApiOperation(value = ClusterOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    @RequestMapping(value = "user/stacks/{name}/cluster", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ClusterResponse> retrievePrivateCluster(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.getPrivateStack(name, user);
        Cluster cluster = clusterService.retrieveClusterForCurrentUser(stack.getId());
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stack.getId());
        ClusterResponse response = getClusterResponse(cluster, clusterJson);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ApiOperation(value = ClusterOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    @RequestMapping(value = "account/stacks/{name}/cluster", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ClusterResponse> retrievePublicCluster(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.getPublicStack(name, user);
        Cluster cluster = clusterService.retrieveClusterForCurrentUser(stack.getId());
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stack.getId());
        ClusterResponse response = getClusterResponse(cluster, clusterJson);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ApiOperation(value = ClusterOpDescription.PUT_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    @RequestMapping(value = "/stacks/{stackId}/cluster", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<String> updateCluster(@PathVariable Long stackId, @RequestBody UpdateClusterJson updateJson) throws CloudbreakSecuritySetupException {
        Stack stack = stackService.get(stackId);
        MDCBuilder.buildMdcContext(stack);
        UserNamePasswordJson userNamePasswordJson = updateJson.getUserNamePasswordJson();
        if (userNamePasswordJson != null) {
            ambariUserNamePasswordChange(stackId, stack, userNamePasswordJson);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        if (updateJson.getStatus() != null) {
            LOGGER.info("Cluster status update request received. Stack id:  {}, status: {} ", stackId, updateJson.getStatus());
            clusterService.updateStatus(stackId, updateJson.getStatus());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        if (updateJson.getBlueprintId() != null && updateJson.getHostgroups() != null && stack.getCluster().isCreateFailed()) {
            LOGGER.info("Cluster rebuild request received. Stack id:  {}", stackId);
            recreateCluster(stackId, updateJson);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        if (updateJson.getHostGroupAdjustment() != null) {
            clusterHostgroupAdjusmentChange(stackId, updateJson, stack);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        LOGGER.error("Invalid cluster update request received. Stack id: {}", stackId);
        throw new BadRequestException("Invalid update cluster request!");
    }

    private void clusterHostgroupAdjusmentChange(Long stackId, UpdateClusterJson updateJson, Stack stack)
            throws CloudbreakSecuritySetupException {
        if (!stack.isAvailable()) {
            throw new BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stackId,
                    stack.getStatus()));
        }
        LOGGER.info("Cluster host adjustment request received. Stack id: {} ", stackId);
        clusterService.updateHosts(stackId, updateJson.getHostGroupAdjustment());
    }

    private void recreateCluster(Long stackId, UpdateClusterJson updateJson) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupJson json : updateJson.getHostgroups()) {
            HostGroup hostGroup = conversionService.convert(json, HostGroup.class);
            hostGroup = hostGroupDecorator.decorate(hostGroup, stackId, json.getInstanceGroupName(), json.getRecipeIds(), false);
            hostGroups.add(hostGroupService.save(hostGroup));
        }
        AmbariStackDetailsJson stackDetails = updateJson.getAmbariStackDetails();
        AmbariStackDetails ambariStackDetails = null;
        if (stackDetails != null) {
            ambariStackDetails = conversionService.convert(stackDetails, AmbariStackDetails.class);
        }
        clusterService.recreate(stackId, updateJson.getBlueprintId(), hostGroups, updateJson.getValidateBlueprint(), ambariStackDetails);
    }

    private void ambariUserNamePasswordChange(Long stackId, Stack stack, UserNamePasswordJson userNamePasswordJson) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stackId,
                    stack.getStatus()));
        }
        if (!userNamePasswordJson.getOldPassword().equals(stack.getCluster().getPassword())) {
            throw new BadRequestException(String.format(
                    "Cluster actual password does not match in the request, please pass the real password.", stackId,
                    stack.getStatus()));
        }
        LOGGER.info("Cluster username password update request received. Stack id:  {}, username: {}, password: {}",
                stackId, userNamePasswordJson.getUserName(), userNamePasswordJson.getPassword());
        clusterService.updateUserNamePassword(stackId, userNamePasswordJson);
    }

    private ClusterResponse getClusterResponse(Cluster cluster, String clusterJson) {
        ClusterResponse response = conversionService.convert(cluster, ClusterResponse.class);
        response.setCluster(jsonHelper.createJsonFromString(clusterJson));
        return response;
    }
}
