package com.sequenceiq.cloudbreak.controller;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

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
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;

@Controller
public class CredentialController {

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private AwsCredentialConverter awsCredentialConverter;

    @Autowired
    private AzureCredentialConverter azureCredentialConverter;

    @Autowired
    private GccCredentialConverter gccCredentialConverter;

    @Autowired
    private AzureStackUtil azureStackUtil;

    @RequestMapping(value = "user/credentials", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> savePrivateCredential(@ModelAttribute("user") CbUser user, @Valid @RequestBody CredentialJson credentialRequest) {
        return createCredential(user, credentialRequest, false);
    }

    @RequestMapping(value = "account/credentials", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> saveAccountCredential(@ModelAttribute("user") CbUser user, @Valid @RequestBody CredentialJson credentialRequest)  {
        return createCredential(user, credentialRequest, true);
    }

    @RequestMapping(value = "user/credentials", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<CredentialJson>> getPrivateCredentials(@ModelAttribute("user") CbUser user) {
        Set<Credential> credentials = credentialService.retrievePrivateCredentials(user);
        return new ResponseEntity<>(convertCredentials(credentials), HttpStatus.OK);
    }

    @RequestMapping(value = "account/credentials", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<CredentialJson>> getAccountCredentials(@ModelAttribute("user") CbUser user) {
        Set<Credential> credentials = credentialService.retrieveAccountCredentials(user);
        return new ResponseEntity<>(convertCredentials(credentials), HttpStatus.OK);
    }

    @RequestMapping(value = "user/credentials/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CredentialJson> getPrivateCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Credential credentials = credentialService.getPrivateBlueprint(name, user);
        return new ResponseEntity<>(convert(credentials), HttpStatus.OK);
    }

    @RequestMapping(value = "account/credentials/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CredentialJson> getAccountCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Credential credentials = credentialService.getPublicBlueprint(name, user);
        return new ResponseEntity<>(convert(credentials), HttpStatus.OK);
    }

    @RequestMapping(value = "credentials/{credentialId}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CredentialJson> getCredential(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId) {
        Credential credential = credentialService.get(credentialId);
        return new ResponseEntity<>(convert(credential), HttpStatus.OK);
    }

    @RequestMapping(value = "credentials/{credentialId}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<CredentialJson> deleteCredential(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId) {
        credentialService.delete(credentialId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "account/credentials/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<CredentialJson> deletePublicCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        credentialService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "user/credentials/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<CredentialJson> deletePrivateCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        credentialService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

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

    private ResponseEntity<IdJson> createCredential(CbUser user, CredentialJson credentialRequest, Boolean publicInAccount) {
        Credential credential = convert(credentialRequest);
        credential.setPublicInAccount(publicInAccount);
        credential = credentialService.create(user, credential);
        return new ResponseEntity<>(new IdJson(credential.getId()), HttpStatus.CREATED);
    }

    private Credential convert(CredentialJson json) {
        Credential ret = null;
        switch (json.getCloudPlatform()) {
            case AWS:
                ret = awsCredentialConverter.convert(json);
                break;
            case AZURE:
                ret = azureCredentialConverter.convert(json);
                break;
            case GCC:
                ret = gccCredentialConverter.convert(json);
                break;
            default:
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", json.getCloudPlatform()));
        }
        return ret;
    }

    private CredentialJson convert(Credential credential) {
        CredentialJson ret = null;
        switch (credential.getCloudPlatform()) {
            case AWS:
                ret = awsCredentialConverter.convert((AwsCredential) credential);
                break;
            case AZURE:
                ret = azureCredentialConverter.convert((AzureCredential) credential);
                break;
            case GCC:
                ret = gccCredentialConverter.convert((GccCredential) credential);
                break;
            default:
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", credential.getCloudPlatform()));
        }
        return ret;
    }

    private Set<CredentialJson> convertCredentials(Set<Credential> credentials) {
        Set<CredentialJson> jsons = new HashSet<>();
        for (Credential current : credentials) {
            jsons.add(convert(current));
        }
        return jsons;
    }
}
