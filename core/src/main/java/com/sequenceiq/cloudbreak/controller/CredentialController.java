package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
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
import com.sequenceiq.cloudbreak.controller.doc.OperationDescriptions.CredentialOpDescription;
import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;
import com.sequenceiq.cloudbreak.controller.json.CredentialResponse;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@Api(value = "/credentials", description = ControllerDescription.CREDENTIAL_DESCRIPTION, position = 1)
public class CredentialController {

    @Resource
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private CredentialService credentialService;

    @ApiOperation(value = CredentialOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    @RequestMapping(value = "user/credentials", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> savePrivateCredential(@ModelAttribute("user") CbUser user, @Valid @RequestBody CredentialRequest credentialRequest) {
        MDCBuilder.buildUserMdcContext(user);
        return createCredential(user, credentialRequest, false);
    }

    @ApiOperation(value = CredentialOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    @RequestMapping(value = "account/credentials", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> saveAccountCredential(@ModelAttribute("user") CbUser user, @Valid @RequestBody CredentialRequest credentialRequest) {
        MDCBuilder.buildUserMdcContext(user);
        return createCredential(user, credentialRequest, true);
    }

    @ApiOperation(value = CredentialOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    @RequestMapping(value = "user/credentials", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<CredentialResponse>> getPrivateCredentials(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildUserMdcContext(user);
        Set<Credential> credentials = credentialService.retrievePrivateCredentials(user);
        return new ResponseEntity<>(convertCredentials(credentials), HttpStatus.OK);
    }

    @ApiOperation(value = CredentialOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    @RequestMapping(value = "account/credentials", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<CredentialResponse>> getAccountCredentials(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildUserMdcContext(user);
        Set<Credential> credentials = credentialService.retrieveAccountCredentials(user);
        return new ResponseEntity<>(convertCredentials(credentials), HttpStatus.OK);
    }

    @ApiOperation(value = CredentialOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    @RequestMapping(value = "user/credentials/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CredentialResponse> getPrivateCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildUserMdcContext(user);
        Credential credentials = credentialService.getPrivateCredential(name, user);
        return new ResponseEntity<>(convert(credentials), HttpStatus.OK);
    }

    @ApiOperation(value = CredentialOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    @RequestMapping(value = "account/credentials/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CredentialResponse> getAccountCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildUserMdcContext(user);
        Credential credentials = credentialService.getPublicCredential(name, user);
        return new ResponseEntity<>(convert(credentials), HttpStatus.OK);
    }

    @ApiOperation(value = CredentialOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    @RequestMapping(value = "credentials/{credentialId}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CredentialResponse> getCredential(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId) {
        MDCBuilder.buildUserMdcContext(user);
        Credential credential = credentialService.get(credentialId);
        return new ResponseEntity<>(convert(credential), HttpStatus.OK);
    }

    @ApiOperation(value = CredentialOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    @RequestMapping(value = "credentials/{credentialId}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<CredentialResponse> deleteCredential(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId) {
        MDCBuilder.buildUserMdcContext(user);
        credentialService.delete(credentialId, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = CredentialOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    @RequestMapping(value = "account/credentials/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<CredentialResponse> deletePublicCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildUserMdcContext(user);
        credentialService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = CredentialOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    @RequestMapping(value = "user/credentials/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<CredentialResponse> deletePrivateCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildUserMdcContext(user);
        credentialService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ResponseEntity<IdJson> createCredential(CbUser user, CredentialRequest credentialRequest, boolean publicInAccount) {
        Credential credential = convert(credentialRequest, publicInAccount);
        credential = credentialService.create(user, credential);
        return new ResponseEntity<>(new IdJson(credential.getId()), HttpStatus.CREATED);
    }

    private Credential convert(CredentialRequest json, boolean publicInAccount) {
        Credential converted = conversionService.convert(json, Credential.class);
        converted.setPublicInAccount(publicInAccount);
        return converted;
    }

    private CredentialResponse convert(Credential credential) {
        return conversionService.convert(credential, CredentialResponse.class);
    }

    private Set<CredentialResponse> convertCredentials(Set<Credential> credentials) {
        Set<CredentialResponse> jsonSet = new HashSet<>();
        for (Credential credential : credentials) {
            jsonSet.add(convert(credential));
        }
        return jsonSet;
    }
}
