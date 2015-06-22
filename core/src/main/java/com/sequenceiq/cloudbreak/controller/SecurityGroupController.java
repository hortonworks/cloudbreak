package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;

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

import com.sequenceiq.cloudbreak.controller.doc.ContentType;
import com.sequenceiq.cloudbreak.controller.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.controller.doc.Notes;
import com.sequenceiq.cloudbreak.controller.doc.OperationDescriptions.SecurityGroupOpDescription;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.SecurityGroupJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.service.securitygroup.DefaultSecurityGroupCreator;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@Api(value = "/securitygroups", description = ControllerDescription.SECURITY_GROUPS_DESCRIPTION, position = 9)
public class SecurityGroupController {
    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private SecurityGroupService securityGroupService;

    @Inject
    private DefaultSecurityGroupCreator securityGroupCreator;

    @ApiOperation(value = SecurityGroupOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    @RequestMapping(value = "user/securitygroups", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createPrivateSecurityGroup(@ModelAttribute("user") CbUser user, @RequestBody @Valid SecurityGroupJson securityGroupJson) {
        return createSecurityGroup(user, securityGroupJson, false);
    }

    @ApiOperation(value = SecurityGroupOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    @RequestMapping(value = "account/securitygroups", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createAccountSecurityGroup(@ModelAttribute("user") CbUser user, @RequestBody @Valid SecurityGroupJson securityGroupJson) {
        return createSecurityGroup(user, securityGroupJson, true);
    }

    @ApiOperation(value = SecurityGroupOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    @RequestMapping(value = "user/securitygroups", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<SecurityGroupJson>> getPrivateSecurityGroups(@ModelAttribute("user") CbUser user) {
        Set<SecurityGroup> securityGroups = securityGroupCreator.createDefaultSecurityGroups(user);
        securityGroups.addAll(securityGroupService.retrievePrivateSecurityGroups(user));
        return new ResponseEntity<>(convert(securityGroups), HttpStatus.OK);
    }

    @ApiOperation(value = SecurityGroupOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    @RequestMapping(value = "account/securitygroups", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<SecurityGroupJson>> getAccountSecurityGroups(@ModelAttribute("user") CbUser user) {
        Set<SecurityGroup> securityGroups = securityGroupCreator.createDefaultSecurityGroups(user);
        securityGroups.addAll(securityGroupService.retrieveAccountSecurityGroups(user));
        return new ResponseEntity<>(convert(securityGroups), HttpStatus.OK);
    }

    @ApiOperation(value = SecurityGroupOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    @RequestMapping(value = "securitygroups/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<SecurityGroupJson> getSecurityGroup(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        SecurityGroup securityGroup = securityGroupService.getById(id);
        return new ResponseEntity<>(convert(securityGroup), HttpStatus.OK);
    }

    @ApiOperation(value = SecurityGroupOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    @RequestMapping(value = "user/securitygroups/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<SecurityGroupJson> getSecurityGroupInPrivate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        SecurityGroup securityGroup = securityGroupService.getPrivateSecurityGroup(name, user);
        return new ResponseEntity<>(convert(securityGroup), HttpStatus.OK);
    }

    @ApiOperation(value = SecurityGroupOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    @RequestMapping(value = "account/securitygroups/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<SecurityGroupJson> getSecurityGroupInAccount(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        SecurityGroup securityGroup = securityGroupService.getPublicSecurityGroup(name, user);
        return new ResponseEntity<>(convert(securityGroup), HttpStatus.OK);
    }

    @ApiOperation(value = SecurityGroupOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    @RequestMapping(value = "securitygroups/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<SecurityGroupJson> deleteNetworkById(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        securityGroupService.delete(id, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = SecurityGroupOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    @RequestMapping(value = "account/securitygroups/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<SecurityGroupJson> deletePublicSecurityGroup(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        securityGroupService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = SecurityGroupOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    @RequestMapping(value = "user/securitygroups/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<SecurityGroupJson> deletePrivateSecurityGroup(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        securityGroupService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ResponseEntity<IdJson> createSecurityGroup(CbUser user, SecurityGroupJson securityGroupJson, boolean publicInAccount) {
        SecurityGroup securityGroup = convert(securityGroupJson, publicInAccount);
        securityGroup = securityGroupService.create(user, securityGroup);
        return new ResponseEntity<>(new IdJson(securityGroup.getId()), HttpStatus.CREATED);
    }

    private SecurityGroup convert(SecurityGroupJson securityGroupJson, boolean publicInAccount) {
        SecurityGroup securityGroup = conversionService.convert(securityGroupJson, SecurityGroup.class);
        securityGroup.setPublicInAccount(publicInAccount);
        return securityGroup;
    }

    private SecurityGroupJson convert(SecurityGroup securityGroup) {
        return conversionService.convert(securityGroup, SecurityGroupJson.class);
    }

    private Set<SecurityGroupJson> convert(Set<SecurityGroup> securityGroups) {
        Set<SecurityGroupJson> jsons = new HashSet<>();
        for (SecurityGroup securityGroup : securityGroups) {
            jsons.add(convert(securityGroup));
        }
        return jsons;
    }
}
