package com.sequenceiq.provisioning.controller;

import java.io.File;
import java.nio.file.Files;

import javax.servlet.http.HttpServletResponse;

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

import com.sequenceiq.provisioning.domain.CertificateRequest;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.security.CurrentUser;
import com.sequenceiq.provisioning.service.azure.AzureProvisionService;
import com.sequenceiq.provisioning.service.azure.CertificateGeneratorService;

@Controller
public class CertificateController {

    @Autowired
    private CertificateGeneratorService certificateGeneratorService;

    @Autowired
    private AzureProvisionService azureProvisionService;

    @RequestMapping(method = RequestMethod.POST, value = "/certificate")
    @ResponseBody
    public ResponseEntity<String> generate(@CurrentUser User user, @RequestBody CertificateRequest certificateRequest) throws Exception {
        certificateGeneratorService.generateCertificate(user);
        return new ResponseEntity<>("generate", HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/certificate")
    @ResponseBody
    public ModelAndView getCertificateFile(@CurrentUser User user, HttpServletResponse response) throws Exception {
        File cerFile = certificateGeneratorService.getCertificateFile(user);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + user.emailAsFolder() + ".cer");
        FileCopyUtils.copy(Files.readAllBytes(cerFile.toPath()), response.getOutputStream());
        return null;
    }
}
