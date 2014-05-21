package com.sequenceiq.provisioning.controller;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.sequenceiq.provisioning.controller.json.CredentialJson;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.security.CurrentUser;
import com.sequenceiq.provisioning.service.CredentialService;
import com.sequenceiq.provisioning.service.azure.AzureCredentialService;

@Controller
@RequestMapping("credential")
public class CredentialController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialController.class);

    @Resource
    private Map<CloudPlatform, CredentialService> credentialServices;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> saveCredential(@CurrentUser User user, @Valid @RequestBody CredentialJson credentialRequest) throws Exception {
        CredentialService credentialService = credentialServices.get(credentialRequest.getCloudPlatform());
        credentialService.saveCredentials(user, credentialRequest);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<CredentialJson> listCredentials(@CurrentUser User user) {
        List<CredentialJson> credentials = new ArrayList<>();
        for (CredentialService credentialService : credentialServices.values()) {
            try {
                CredentialJson credential = credentialService.retrieveCredentials(user);
                credentials.add(credential);
            } catch (NotFoundException e) {
                LOGGER.info(e.getMessage());
            }
        }
        return credentials;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{cloudPlatform}")
    @ResponseBody
    public CredentialJson getCredential(@CurrentUser User user, @PathVariable String cloudPlatform) {
        try {
            CredentialService credentialService = credentialServices.get(CloudPlatform.valueOf(cloudPlatform.toUpperCase()));
            return credentialService.retrieveCredentials(user);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown cloud platform: " + cloudPlatform, e);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/certificate")
    @ResponseBody
    public ModelAndView getJksFile(@CurrentUser User user, HttpServletResponse response) throws Exception {
        AzureCredentialService azureCredentialService = (AzureCredentialService) credentialServices.get(CloudPlatform.AZURE);
        File cerFile = azureCredentialService.getCertificateFile(user);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + user.emailAsFolder() + ".cer");
        FileCopyUtils.copy(Files.readAllBytes(cerFile.toPath()), response.getOutputStream());
        return null;
    }
}
