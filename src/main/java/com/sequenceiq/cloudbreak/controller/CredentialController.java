package com.sequenceiq.cloudbreak.controller;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.security.CurrentUser;
import com.sequenceiq.cloudbreak.service.CredentialService;
import com.sequenceiq.cloudbreak.service.azure.AzureCredentialService;

@Controller
@RequestMapping("credentials")
public class CredentialController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialController.class);

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private AzureCredentialService azureCredentialService;

    @Autowired
    private UserRepository userRepository;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> saveCredential(@CurrentUser User user, @Valid @RequestBody CredentialJson credentialRequest) throws Exception {
        IdJson save = credentialService.save(user, credentialRequest);
        return new ResponseEntity<>(save, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<CredentialJson>> listCredentials(@CurrentUser User user) {
        Set<CredentialJson> credentials = new HashSet<>();
        try {
            Set<CredentialJson> credentialSet = credentialService.getAll(userRepository.findOneWithLists(user.getId()));
            credentials.addAll(credentialSet);
        } catch (NotFoundException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return new ResponseEntity<>(credentials, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{credentialId}")
    @ResponseBody
    public ResponseEntity<CredentialJson> getCredential(@CurrentUser User user, @PathVariable Long credentialId) {
        try {
            CredentialJson credentialJson = credentialService.get(credentialId);
            return new ResponseEntity<>(credentialJson, HttpStatus.OK);
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
        File cerFile = azureCredentialService.getCertificateFile(credentialId, user);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + user.emailAsFolder() + ".cer");
        FileCopyUtils.copy(Files.readAllBytes(cerFile.toPath()), response.getOutputStream());
        return null;
    }
}
