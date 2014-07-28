package com.sequenceiq.cloudbreak.service.credential;

import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.converter.AwsCredentialConverter;
import com.sequenceiq.cloudbreak.converter.AzureCredentialConverter;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.AwsCredentialRepository;
import com.sequenceiq.cloudbreak.repository.AzureCredentialRepository;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UnknownFormatConversionException;

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
    private AzureCertificateService azureCertificateService;

    @Autowired
    private WebsocketService websocketService;


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

    public IdJson save(User user, CredentialJson credentialJson) {
        switch (credentialJson.getCloudPlatform()) {
            case AWS:
                return saveAwsCredential(user, credentialJson);
            case AZURE:
                return saveAzureCredential(user, credentialJson);
            default:
                websocketService.sendToTopicUser(user.getEmail(), WebsocketEndPoint.CREDENTIAL,
                        new StatusMessage(-1L, credentialJson.getName(), Status.CREATE_FAILED.name()));
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", credentialJson.getCloudPlatform()));
        }
    }

    public void delete(Long id) {
        Credential credential = credentialRepository.findOne(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential '%s' not found.", id));
        }
        credentialRepository.delete(credential);
        websocketService.sendToTopicUser(credential.getOwner().getEmail(), WebsocketEndPoint.CREDENTIAL,
                new StatusMessage(credential.getId(), credential.getCredentialName(), Status.DELETE_COMPLETED.name()));
    }

    private IdJson saveAwsCredential(User user, CredentialJson credentialJson) {
        AwsCredential awsCredential = awsCredentialConverter.convert(credentialJson);
        awsCredential.setAwsCredentialOwner(user);
        awsCredential = awsCredentialRepository.save(awsCredential);
        websocketService.sendToTopicUser(user.getEmail(), WebsocketEndPoint.CREDENTIAL,
                new StatusMessage(awsCredential.getId(), awsCredential.getName(), Status.CREATE_COMPLETED.name()));
        return new IdJson(awsCredential.getId());
    }

    private IdJson saveAzureCredential(User user, CredentialJson credentialJson) {
        AzureCredential azureCredential = azureCredentialConverter.convert(credentialJson);
        azureCredential.setAzureCredentialOwner(user);
        azureCredential = azureCredentialRepository.save(azureCredential);
        if (azureCredential.getPublicKey() != null) {
            azureCertificateService.generateSshCertificate(user, azureCredential, azureCredential.getPublicKey());
        }
        azureCertificateService.generateCertificate(azureCredential, user);
        websocketService.sendToTopicUser(user.getEmail(), WebsocketEndPoint.CREDENTIAL,
                new StatusMessage(azureCredential.getId(), azureCredential.getName(), Status.CREATE_COMPLETED.name()));
        return new IdJson(azureCredential.getId());
    }

    @Override
    public File getSshPublicKeyFile(User user, Long credentialId) {
        Credential one = credentialRepository.findOne(credentialId);
        if (CloudPlatform.AZURE.equals(one.cloudPlatform())) {
            return azureCertificateService.getSshPublicKeyFile(user, credentialId);
        } else {
            throw new UnsupportedOperationException("Ssh key function supported only on Azure platform.");
        }
    }

}
