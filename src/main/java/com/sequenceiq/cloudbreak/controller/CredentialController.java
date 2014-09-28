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
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.security.CurrentUser;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;

@Controller
@RequestMapping("credentials")
public class CredentialController {

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private AzureCertificateService azureCertificateService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AwsCredentialConverter awsCredentialConverter;

    @Autowired
    private AzureCredentialConverter azureCredentialConverter;

    @Autowired
    private GccCredentialConverter gccCredentialConverter;


    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> saveCredential(@CurrentUser User user, @Valid @RequestBody CredentialJson credentialRequest) throws Exception {
        Credential credential = convert(credentialRequest);
        if (credential.getUserRoles().isEmpty()) {
            credential.getUserRoles().addAll(user.getUserRoles());
        }
        credential = credentialService.save(user, credential);
        return new ResponseEntity<>(new IdJson(credential.getId()), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<CredentialJson>> getAllCredentials(@CurrentUser User user) {
        User loadedUser = userRepository.findOneWithLists(user.getId());
        Set<Credential> credentials = credentialService.getAll(loadedUser);
        Set<CredentialJson> credentialJsons = convertCredentials(credentials);
        return new ResponseEntity<>(credentialJsons, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{credentialId}")
    @ResponseBody
    public ResponseEntity<CredentialJson> getCredential(@CurrentUser User user, @PathVariable Long credentialId) {
        try {
            Credential credential = credentialService.get(credentialId);
            return new ResponseEntity<>(convert(credential), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown cloud platform: " + credentialId, e);
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{credentialId}")
    @ResponseBody
    public ResponseEntity<CredentialJson> deleteCredential(@CurrentUser User user, @PathVariable Long credentialId) {
        try {
            credentialService.delete(credentialId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            throw new BadRequestException(
                    String.format("Deletion of: %s was not success. Delete all resources before you delete the credential.", credentialId), e);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/certificate/{credentialId}")
    @ResponseBody
    public ModelAndView getJksFile(@CurrentUser User user, @PathVariable Long credentialId, HttpServletResponse response) throws Exception {
        File cerFile = azureCertificateService.getCertificateFile(credentialId, user);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + user.emailAsFolder() + ".cer");
        FileCopyUtils.copy(Files.readAllBytes(cerFile.toPath()), response.getOutputStream());
        return null;
    }

    @RequestMapping(method = RequestMethod.GET, value = "{credentialId}/sshkey")
    @ResponseBody
    public ModelAndView getSshFile(@CurrentUser User user, @PathVariable Long credentialId, HttpServletResponse response) throws Exception {
        File cerFile = azureCertificateService.getSshPublicKeyFile(user, credentialId);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=public_key.pem");
        FileCopyUtils.copy(Files.readAllBytes(cerFile.toPath()), response.getOutputStream());
        return null;
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
        ret.getUserRoles().addAll(json.getUserRoles());
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
