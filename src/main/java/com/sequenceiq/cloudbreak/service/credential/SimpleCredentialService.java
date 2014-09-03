package com.sequenceiq.cloudbreak.service.credential;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.converter.GccCredentialConverter;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.AwsCredentialRepository;
import com.sequenceiq.cloudbreak.repository.AzureCredentialRepository;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.GccCredentialRepository;
import com.sequenceiq.cloudbreak.service.account.AccountService;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

@Service
public class SimpleCredentialService implements CredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCredentialService.class);

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private AzureCredentialRepository azureCredentialRepository;

    @Autowired
    private AwsCredentialRepository awsCredentialRepository;

    @Autowired
    private GccCredentialRepository gccCredentialRepository;

    @Autowired
    private GccCredentialConverter gccCredentialConverter;

    @Autowired
    private AzureCertificateService azureCertificateService;

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private AccountService accountService;

    public Set<Credential> getAll(User user) {
        Set<Credential> userCredentials = new HashSet<>();
        Set<Credential> legacyCredentials = new HashSet<>();
        userCredentials.addAll(user.getAwsCredentials());
        userCredentials.addAll(user.getAzureCredentials());
        userCredentials.addAll(user.getGccCredentials());
        LOGGER.debug("User credentials: #{}", userCredentials.size());

        if (user.getUserRoles().contains(UserRole.ACCOUNT_ADMIN)) {
            LOGGER.debug("Getting company user credentials for company admin; id: [{}]", user.getId());
            legacyCredentials = getCompanyUserCredentials(user);
        } else if (user.getUserRoles().contains(UserRole.ACCOUNT_USER)) {
            LOGGER.debug("Getting company wide credentials for company user; id: [{}]", user.getId());
            legacyCredentials = getCompanyCredentials(user);
        }
        LOGGER.debug("Found #{} legacy credentials for user [{}]", legacyCredentials.size(), user.getId());
        userCredentials.addAll(legacyCredentials);

        return userCredentials;
    }

    private Set<Credential> getCompanyCredentials(User user) {
        Set<Credential> companyCredentials = new HashSet<>();
        User adminWithFilteredData = accountService.accountUserData(user.getAccount().getId(), user.getUserRoles().iterator().next());
        if (adminWithFilteredData != null) {
            companyCredentials.addAll(adminWithFilteredData.getAwsCredentials());
            companyCredentials.addAll(adminWithFilteredData.getAzureCredentials());
        } else {
            LOGGER.debug("There's no company admin for user: [{}]", user.getId());
        }
        return companyCredentials;
    }

    private Set<Credential> getCompanyUserCredentials(User user) {
        Set<Credential> companyUserCredentials = new HashSet<>();
        Set<User> companyUsers = accountService.accountUsers(user.getAccount().getId());
        companyUsers.remove(user);
        for (User cUser : companyUsers) {
            LOGGER.debug("Adding credentials of company user: [{}]", cUser.getId());
            companyUserCredentials.addAll(cUser.getAzureCredentials());
            companyUserCredentials.addAll(cUser.getAwsCredentials());
        }
        return companyUserCredentials;

    }

    public Credential get(Long id) {
        Credential credential = credentialRepository.findOne(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Template '%s' not found.", id));
        } else {
            return credential;
        }
    }

    public Credential save(User user, Credential credential) {
        switch (credential.getCloudPlatform()) {
            case AWS:
                return saveAwsCredential(user, credential);
            case AZURE:
                return saveAzureCredential(user, credential);
            case GCC:
                return saveGccCredential(user, credential);
            default:
                websocketService.sendToTopicUser(user.getEmail(), WebsocketEndPoint.CREDENTIAL,
                        new StatusMessage(-1L, credential.getCredentialName(), Status.CREATE_FAILED.name()));
                throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", credential.getCloudPlatform()));
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

    private Credential saveAwsCredential(User user, Credential credential) {
        AwsCredential awsCredential = (AwsCredential) credential;
        awsCredential.setAwsCredentialOwner(user);
        awsCredential = awsCredentialRepository.save(awsCredential);
        websocketService.sendToTopicUser(user.getEmail(), WebsocketEndPoint.CREDENTIAL,
                new StatusMessage(awsCredential.getId(), awsCredential.getName(), Status.AVAILABLE.name()));
        return awsCredential;
    }

    private Credential saveAzureCredential(User user, Credential credential) {
        AzureCredential azureCredential = (AzureCredential) credential;
        azureCredential.setAzureCredentialOwner(user);
        azureCredential = azureCredentialRepository.save(azureCredential);
        if (azureCredential.getPublicKey() != null) {
            azureCertificateService.generateSshCertificate(user, azureCredential, azureCredential.getPublicKey());
        }
        azureCertificateService.generateCertificate(azureCredential, user);
        websocketService.sendToTopicUser(user.getEmail(), WebsocketEndPoint.CREDENTIAL,
                new StatusMessage(azureCredential.getId(), azureCredential.getName(), Status.AVAILABLE.name()));
        return azureCredential;
    }


    private Credential saveGccCredential(User user, Credential credential) {
        GccCredential gccCredential = (GccCredential) credential;
        gccCredential.setGccCredentialOwner(user);
        gccCredential = gccCredentialRepository.save(gccCredential);
        websocketService.sendToTopicUser(user.getEmail(), WebsocketEndPoint.CREDENTIAL,
                new StatusMessage(gccCredential.getId(), gccCredential.getName(), Status.AVAILABLE.name()));
        return gccCredential;
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
