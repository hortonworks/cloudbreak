package com.sequenceiq.cloudbreak.service.credential;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.service.CloudPlarformService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderCredentialAdapter;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;

@Service
public class CredentialService extends AbstractOrganizationAwareResourceService<Credential> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialService.class);

    private static final String NOT_FOUND_FORMAT_MESS_ID = "Credential with id:";

    private static final String NOT_FOUND_FORMAT_MESS_NAME = "Credential with name:";

    @Resource
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private CredentialRepository credentialRepository;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ServiceProviderCredentialAdapter credentialAdapter;

    @Inject
    private UserProfileHandler userProfileHandler;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private CredentialValidator credentialValidator;

    @Inject
    private CloudPlarformService cloudPlarformService;

    public Set<Credential> listAvailablesByOrganizationId(Long orgId) {
        return credentialRepository.findActiveForOrganizationFilterByPlatforms(orgId, cloudPlarformService.enabledPlatforms());
    }

    public Credential get(Long id, Organization organization) {
        return Optional.ofNullable(credentialRepository.findActiveByIdAndOrganizationFilterByPlatforms(id, organization.getId(),
                cloudPlarformService.enabledPlatforms())).orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_ID, id));
    }

    public Map<String, String> interactiveLogin(Long organizationId, Credential credential, Organization organization, User user) {
        credential.setOrganization(organization);
        return credentialAdapter.interactiveLogin(credential, organizationId, user.getUserId());
    }

    public Credential updateByOrganizationId(Long organizationId, Credential credential, User user) {
        credentialValidator.validateCredentialCloudPlatform(credential.cloudPlatform());
        Credential original = Optional.ofNullable(
                credentialRepository.findActiveByNameAndOrgIdFilterByPlatforms(credential.getName(), organizationId,
                        cloudPlarformService.enabledPlatforms()))
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, credential.getName()));
        if (original.cloudPlatform() != null && !Objects.equals(credential.cloudPlatform(), original.cloudPlatform())) {
            throw new BadRequestException("Modifying credential platform is forbidden");
        }
        credential.setId(original.getId());
        credential.setOrganization(organizationService.get(organizationId, user));
        Credential updated = super.create(credentialAdapter.init(credential, organizationId, user.getUserId()), organizationId, user);
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_MODIFIED);
        return updated;
    }

    @Retryable(value = BadRequestException.class, maxAttempts = 30, backoff = @Backoff(delay = 2000))
    public void createWithRetry(Credential credential, Long organizationId, User user) {
        create(credential, organizationId, user);
    }

    @Override
    public Credential create(Credential credential, Long organizationId, User user) {
        LOGGER.debug("Creating credential for organization: {}", getOrganizationService().get(organizationId, user).getName());
        credentialValidator.validateCredentialCloudPlatform(credential.cloudPlatform());
        Credential created = super.create(credentialAdapter.init(credential, organizationId, user.getUserId()), organizationId, user);
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_CREATED);
        return created;
    }

    public void delete(Long id, Organization organization) {
        Credential credential = Optional.ofNullable(
                credentialRepository.findActiveByIdAndOrganizationFilterByPlatforms(id, organization.getId(), cloudPlarformService.enabledPlatforms()))
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_ID, id));
        delete(credential, organization);
    }

    public void delete(String name, Organization organization) {
        Credential credential = Optional.ofNullable(
                credentialRepository.findActiveByNameAndOrgIdFilterByPlatforms(name, organization.getId(), cloudPlarformService.enabledPlatforms()))
                .orElseThrow(notFound(NOT_FOUND_FORMAT_MESS_NAME, name));
        delete(credential, organization);
    }

    public Set<CredentialResponse> convertAllToResponse(@Nonnull Iterable<Credential> credentials) {
        Set<CredentialResponse> jsonSet = new LinkedHashSet<>();
        for (Credential credential : credentials) {
            jsonSet.add(convertToResponse(credential));
        }
        return jsonSet;
    }

    public CredentialResponse convertToResponse(Credential credential) {
        return conversionService.convert(credential, CredentialResponse.class);
    }

    public Credential convertToCredential(CredentialRequest request) {
        return conversionService.convert(request, Credential.class);
    }

    public Set<Credential> findAllByCloudPlatform(String cloudPlatform) {
        return credentialRepository.findAllByCloudPlatform(cloudPlatform);
    }

    public void saveAllCredential(Iterable<Credential> credentials) {
        credentialRepository.saveAll(credentials);
    }

    public Set<Credential> findAllCredentialByTopology(Topology topology) {
        return credentialRepository.findByTopology(topology);
    }

    private void delete(Credential credential, Organization organization) {
        checkCredentialIsDeletable(credential);
        LOGGER.info(String.format("Starting to delete credential [name: %s, organization: %s]", credential.getName(), organization.getName()));
        userProfileHandler.destroyProfileCredentialPreparation(credential);
        archiveCredential(credential);
        sendCredentialNotification(credential, ResourceEvent.CREDENTIAL_DELETED);
    }

    private void checkCredentialIsDeletable(Credential credential) {
        LOGGER.info("Checking whether the desired credential is able to delete or not.");
        if (credential == null) {
            throw new NotFoundException("Credential not found.");
        }
        Set<Stack> stacksForCredential = stackRepository.findByCredential(credential);
        if (!stacksForCredential.isEmpty()) {
            String clusters;
            String message;
            if (stacksForCredential.size() > 1) {
                clusters = stacksForCredential.stream()
                        .map(Stack::getName)
                        .collect(Collectors.joining(", "));
                message = "There are clusters associated with credential config '%s'. Please remove these before deleting the credential. "
                        + "The following clusters are using this credential: [%s]";
            } else {
                clusters = stacksForCredential.iterator().next().getName();
                message = "There is a cluster associated with credential config '%s'. Please remove before deleting the credential. "
                        + "The following cluster is using this credential: [%s]";
            }
            throw new BadRequestException(String.format(message, credential.getName(), clusters));
        }
    }

    @Override
    protected OrganizationResourceRepository<Credential, Long> repository() {
        return credentialRepository;
    }

    @Override
    protected OrganizationResource resource() {
        return OrganizationResource.CREDENTIAL;
    }

    @Override
    protected void prepareDeletion(Credential resource) {
        checkCredentialIsDeletable(resource);
    }

    @Override
    protected void prepareCreation(Credential resource) {

    }

    public void archiveCredential(Credential credential) {
        credential.setName(generateArchiveName(credential.getName()));
        credential.setArchived(true);
        credential.setTopology(null);
        credentialRepository.save(credential);
    }

    private void sendCredentialNotification(Credential credential, ResourceEvent resourceEvent) {
        CloudbreakEventsJson notification = new CloudbreakEventsJson();
        notification.setEventType(resourceEvent.name());
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(messagesService.getMessage(resourceEvent.getMessage()));
        notification.setCloud(credential.cloudPlatform());
        notificationSender.send(new Notification<>(notification));
    }
}