package com.sequenceiq.provisioning.controller;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.sequenceiq.provisioning.controller.json.CredentialRequest;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.security.CurrentUser;
import com.sequenceiq.provisioning.service.CredentialService;
import com.sequenceiq.provisioning.service.azure.AzureCredentialService;

@Controller
@RequestMapping("credential")
public class CredentialController {

    @Autowired
    private AzureCredentialService certificateGeneratorService;

    @Resource
    private Map<CloudPlatform, CredentialService> credentialServices;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> saveCredential(@CurrentUser User user, @Valid @RequestBody CredentialRequest credentialRequest) throws Exception {
        CredentialService credentialService = credentialServices.get(credentialRequest.getCloudPlatform());
        credentialService.saveCredentials(user, credentialRequest);
        return new ResponseEntity<>("generate", HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ModelAndView getCertificateFile(@CurrentUser User user, HttpServletResponse response) throws Exception {
        File cerFile = certificateGeneratorService.getCertificateFile(user);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + user.emailAsFolder() + ".cer");
        FileCopyUtils.copy(Files.readAllBytes(cerFile.toPath()), response.getOutputStream());
        return null;
    }
}
