package com.sequenceiq.cloudbreak.controller;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.converter.AwsCredentialConverter;
import com.sequenceiq.cloudbreak.converter.AzureCredentialConverter;
import com.sequenceiq.cloudbreak.converter.GccCredentialConverter;
import com.sequenceiq.cloudbreak.converter.OpenStackCredentialConverter;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;

@Controller
@Api(value = "/credentials", description = "Operations on credentials", position = 1)
public class CredentialController {

    private static final String CREDENTIAL_REQUEST_NOTES =
            "In the credential request, id and public parameters are not considered.";

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private AwsCredentialConverter awsCredentialConverter;

    @Autowired
    private AzureCredentialConverter azureCredentialConverter;

    @Autowired
    private GccCredentialConverter gccCredentialConverter;

    @Autowired
    private OpenStackCredentialConverter openStackCredentialConverter;

    @Autowired
    private AzureStackUtil azureStackUtil;

    @ApiOperation(value = "create credential as private resource", produces = "application/json", notes = CREDENTIAL_REQUEST_NOTES)
    @RequestMapping(value = "user/credentials", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> savePrivateCredential(@ModelAttribute("user") CbUser user, @Valid @RequestBody CredentialJson credentialRequest) {
        return createCredential(user, credentialRequest, false);
    }

    @ApiOperation(value = "create credential as public or private resource", produces = "application/json", notes = CREDENTIAL_REQUEST_NOTES)
    @RequestMapping(value = "account/credentials", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> saveAccountCredential(@ModelAttribute("user") CbUser user, @Valid @RequestBody CredentialJson credentialRequest) {
        return createCredential(user, credentialRequest, true);
    }

    @ApiOperation(value = "retrieve private credentials", produces = "application/json", notes = "")
    @RequestMapping(value = "user/credentials", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<CredentialJson>> getPrivateCredentials(@ModelAttribute("user") CbUser user) {
        Set<Credential> credentials = credentialService.retrievePrivateCredentials(user);
        return new ResponseEntity<>(convertCredentials(credentials), HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve public and private (owned) credentials", produces = "application/json", notes = "")
    @RequestMapping(value = "account/credentials", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<CredentialJson>> getAccountCredentials(@ModelAttribute("user") CbUser user) {
        Set<Credential> credentials = credentialService.retrieveAccountCredentials(user);
        return new ResponseEntity<>(convertCredentials(credentials), HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve a private credential by name", produces = "application/json", notes = "")
    @RequestMapping(value = "user/credentials/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CredentialJson> getPrivateCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Credential credentials = credentialService.getPrivateCredential(name, user);
        return new ResponseEntity<>(convert(credentials), HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve a public or private (owned) credential by name", produces = "application/json", notes = "")
    @RequestMapping(value = "account/credentials/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CredentialJson> getAccountCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Credential credentials = credentialService.getPublicCredential(name, user);
        return new ResponseEntity<>(convert(credentials), HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve credential by id", produces = "application/json", notes = "")
    @RequestMapping(value = "credentials/{credentialId}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CredentialJson> getCredential(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId) {
        Credential credential = credentialService.get(credentialId);
        return new ResponseEntity<>(convert(credential), HttpStatus.OK);
    }

    @ApiOperation(value = "delete credential by id", produces = "application/json", notes = "")
    @RequestMapping(value = "credentials/{credentialId}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<CredentialJson> deleteCredential(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId) {
        credentialService.delete(credentialId, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "delete public (owned) or private credential by name", produces = "application/json", notes = "")
    @RequestMapping(value = "account/credentials/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<CredentialJson> deletePublicCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        credentialService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "delete private credential by name", produces = "application/json", notes = "")
    @RequestMapping(value = "user/credentials/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<CredentialJson> deletePrivateCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        credentialService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiIgnore
    @RequestMapping(value = "credentials/certificate/{credentialId}", method = RequestMethod.GET)
    @ResponseBody
    public ModelAndView getJksFile(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId, HttpServletResponse response) throws Exception {
        AzureCredential credential = (AzureCredential) credentialService.get(credentialId);
        File cerFile = azureStackUtil.buildAzureCerFile(credential);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + user.getUsername() + ".cer");
        FileCopyUtils.copy(Files.readAllBytes(cerFile.toPath()), response.getOutputStream());
        return null;
    }

    @ApiIgnore
    @RequestMapping(value = "credentials/certificate/{credentialId}", method = RequestMethod.PUT)
    @ResponseBody
    public ModelAndView refreshCredential(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId, HttpServletResponse response) throws Exception {
        Credential credential = credentialService.update(credentialId);
        if (credential instanceof AzureCredential) {
            File cerFile = azureStackUtil.buildAzureCerFile((AzureCredential) credential);
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + user.getUsername() + ".cer");
            FileCopyUtils.copy(Files.readAllBytes(cerFile.toPath()), response.getOutputStream());
        }
        return null;
    }

    @ApiIgnore
    @RequestMapping(value = "credentials/{credentialId}/sshkey", method = RequestMethod.GET)
    @ResponseBody
    public ModelAndView getSshFile(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId, HttpServletResponse response) throws Exception {
        AzureCredential credential = (AzureCredential) credentialService.get(credentialId);
        File cerFile = azureStackUtil.buildAzureSshCerFile(credential);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=public_key.pem");
        FileCopyUtils.copy(Files.readAllBytes(cerFile.toPath()), response.getOutputStream());
        return null;
    }

    private ResponseEntity<IdJson> createCredential(CbUser user, CredentialJson credentialRequest, boolean publicInAccount) {
        Credential credential = convert(credentialRequest, publicInAccount);
        credential = credentialService.create(user, credential);
        return new ResponseEntity<>(new IdJson(credential.getId()), HttpStatus.CREATED);
    }

    private Credential convert(CredentialJson json, boolean publicInAccount) {
        switch (json.getCloudPlatform()) {
            case AWS:
                return awsCredentialConverter.convert(json, publicInAccount);
            case AZURE:
                return azureCredentialConverter.convert(json, publicInAccount);
            case GCC:
                return gccCredentialConverter.convert(json, publicInAccount);
            case OPENSTACK:
                return openStackCredentialConverter.convert(json, publicInAccount);
            default:
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", json.getCloudPlatform()));
        }
    }

    private CredentialJson convert(Credential credential) {
        switch (credential.cloudPlatform()) {
            case AWS:
                return awsCredentialConverter.convert((AwsCredential) credential);
            case AZURE:
                return azureCredentialConverter.convert((AzureCredential) credential);
            case GCC:
                return gccCredentialConverter.convert((GccCredential) credential);
            case OPENSTACK:
                return openStackCredentialConverter.convert((OpenStackCredential) credential);
            default:
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", credential.cloudPlatform()));
        }
    }

    private Set<CredentialJson> convertCredentials(Set<Credential> credentials) {
        Set<CredentialJson> jsons = new HashSet<>();
        for (Credential current : credentials) {
            jsons.add(convert(current));
        }
        return jsons;
    }
}
