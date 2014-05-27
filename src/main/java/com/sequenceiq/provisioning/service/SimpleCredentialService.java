package com.sequenceiq.provisioning.service;

import java.util.HashSet;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.NotFoundException;
import com.sequenceiq.provisioning.controller.json.CredentialJson;
import com.sequenceiq.provisioning.converter.AwsCredentialConverter;
import com.sequenceiq.provisioning.converter.AzureCredentialConverter;
import com.sequenceiq.provisioning.domain.AwsCredential;
import com.sequenceiq.provisioning.domain.AzureCredential;
import com.sequenceiq.provisioning.domain.Credential;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.AwsCredentialRepository;
import com.sequenceiq.provisioning.repository.AzureCredentialRepository;
import com.sequenceiq.provisioning.repository.CredentialRepository;
import com.sequenceiq.provisioning.service.azure.AzureCredentialService;

@Service
public class SimpleCredentialService implements CredentialService {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private AwsCredentialConverter awsCredentialConverter;

    @Autowired
    private AzureCredentialConverter azureCredentialConverter;

    @Autowired
    private AzureCredentialRepository azureCredentialRepository;

    @Autowired
    private AwsCredentialRepository awsCredentialRepository;

    @Autowired
    private AzureCredentialService azureCredentialService;

    public Set<CredentialJson> getAll(User user) {
        Set<CredentialJson> result = new HashSet<>();
        result.addAll(awsCredentialConverter.convertAllEntityToJson(user.getAwsCredentials()));
        result.addAll(azureCredentialConverter.convertAllEntityToJson(user.getAzureCredentials()));
        return result;
    }

    public CredentialJson get(Long id) {
        Credential credential = credentialRepository.findOne(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Template '%s' not found.", id));
        } else {
            switch (credential.cloudPlatform()) {
                case AWS:
                    return awsCredentialConverter.convert((AwsCredential) credential);
                case AZURE:
                    return azureCredentialConverter.convert((AzureCredential) credential);
                default:
                    throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", credential.cloudPlatform()));
            }
        }
    }

    public void save(User user, CredentialJson credentialJson) {
        switch (credentialJson.getCloudPlatform()) {
            case AWS:
                AwsCredential awsCredential = awsCredentialConverter.convert(credentialJson);
                awsCredential.setUser(user);
                awsCredentialRepository.save(awsCredential);
                break;
            case AZURE:
                AzureCredential azureCredential = azureCredentialConverter.convert(credentialJson);
                azureCredential.setUser(user);
                azureCredentialRepository.save(azureCredential);
                azureCredentialService.generateCertificate(azureCredential, user);
                break;
            default:
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", credentialJson.getCloudPlatform()));
        }
    }

    public void delete(Long id) {
        Credential credential = credentialRepository.findOne(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", id));
        }
        credentialRepository.delete(credential);
    }

}
