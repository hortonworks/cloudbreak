package com.sequenceiq.remoteenvironment.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.IterableUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CompositeAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.EnvironmentPropertyProvider;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.account.AbstractAccountAwareResourceService;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationRequests;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.deregistration.PrivateControlPlaneDeRegistrationResponses;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationRequest;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationRequests;
import com.sequenceiq.remoteenvironment.api.v1.controlplane.model.registration.PrivateControlPlaneRegistrationResponses;
import com.sequenceiq.remoteenvironment.controller.v1.converter.PrivateControlPlaneDeRegistrationRequestsToPrivateControlPlaneDeRegistrationResponsesConverter;
import com.sequenceiq.remoteenvironment.controller.v1.converter.PrivateControlPlaneRegistrationRequestsToPrivateControlPlaneRegistrationResponsesConverter;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;
import com.sequenceiq.remoteenvironment.repository.PrivateControlPlaneRepository;

@Service
public class PrivateControlPlaneService extends AbstractAccountAwareResourceService<PrivateControlPlane>
        implements ResourceIdProvider, EnvironmentPropertyProvider, CompositeAuthResourcePropertyProvider {

    @Inject
    private PrivateControlPlaneRepository privateControlPlaneRepository;

    @Inject
    private PrivateControlPlaneRegistrationRequestsToPrivateControlPlaneRegistrationResponsesConverter privateControlPlaneRegistrationRequestsConverter;

    @Inject
    private PrivateControlPlaneDeRegistrationRequestsToPrivateControlPlaneDeRegistrationResponsesConverter privateControlPlaneDeRegistrationRequestsConverter;

    public PrivateControlPlaneRegistrationResponses register(PrivateControlPlaneRegistrationRequests request) {
        for (PrivateControlPlaneRegistrationRequest item : request.getItems()) {
            String crn = item.getCrn();
            PrivateControlPlane privateControlPlane = new PrivateControlPlane();
            Crn pvcCrn = Crn.fromString(crn);
            privateControlPlane.setAccountId(pvcCrn.getAccountId());
            privateControlPlane.setResourceCrn(crn);
            privateControlPlane.setName(item.getName());
            privateControlPlane.setUrl(item.getUrl());
            privateControlPlane.setPrivateCloudAccountId(pvcCrn.getResource());

            repository().save(privateControlPlane);
        }
        return privateControlPlaneRegistrationRequestsConverter.convert(request);
    }

    public PrivateControlPlaneDeRegistrationResponses deregister(PrivateControlPlaneDeRegistrationRequests request) {
        Set<String> crns = request.getItems()
                .stream()
                .map(e -> e.getCrn())
                .collect(Collectors.toSet());
        deleteByResourceCrns(crns);
        return privateControlPlaneDeRegistrationRequestsConverter.convert(request);
    }

    public Optional<PrivateControlPlane> getByPrivateCloudAccountIdAndPublicCloudAccountId(String privateCloudAccountId, String publicCloudAccountId) {
        return privateControlPlaneRepository.findByPvcAccountAndPbcAccountId(privateCloudAccountId, publicCloudAccountId);
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return privateControlPlaneRepository.findAllCrnByNameInAndAccountId(resourceNames, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return getCrnByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId());
    }

    public String getCrnByNameAndAccountId(String resourceName, String accountId) {
        return privateControlPlaneRepository.findResourceCrnByNameAndAccountId(resourceName, accountId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Private Control Plane with name '%s' was not found for account '%s'.", resourceName, accountId)));
    }

    @Override
    protected void prepareDeletion(PrivateControlPlane resource) {

    }

    @Override
    protected void prepareCreation(PrivateControlPlane resource) {

    }

    @Override
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        return EnvironmentPropertyProvider.super.getNamesByCrnsForMessage(crns);
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return EnvironmentPropertyProvider.super.getSupportedAuthorizationResourceType();
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.PVC_CONTROL_PLANE);
    }

    @Override
    protected AccountAwareResourceRepository<PrivateControlPlane, Long> repository() {
        return privateControlPlaneRepository;
    }

    @Override
    public Long getResourceIdByResourceCrn(String resourceCrn) {
        return privateControlPlaneRepository.findIdByResourceCrnAndAccountId(resourceCrn, ThreadBasedUserCrnProvider.getAccountId())
                .orElseThrow(notFound("Private Control Plane with crn:", resourceCrn));
    }

    @Override
    public String getResourceCrnByResourceId(Long resourceId) {
        return privateControlPlaneRepository.findById(resourceId)
                .orElseThrow(notFound("Private Control Plane with resourceId:", resourceId))
                .getResourceCrn();
    }

    @Override
    public Long getResourceIdByResourceName(String resourceName) {
        return privateControlPlaneRepository.findIdByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .orElseThrow(notFound("Private Control Plane with name:", resourceName));
    }

    @Override
    public List<Long> getResourceIdsByResourceCrn(String resourceName) {
        return ResourceIdProvider.super.getResourceIdsByResourceCrn(resourceName);
    }

    public Set<PrivateControlPlane> listByAccountId(String accountId) {
        return repository().findAllByAccountId(accountId);
    }

    public List<PrivateControlPlane> findAll() {
        return IterableUtils.toList(repository().findAll());
    }

    public void deleteByResourceCrns(Set<String> crns) {
        privateControlPlaneRepository.deleteByResourceCrns(crns);
    }
}
