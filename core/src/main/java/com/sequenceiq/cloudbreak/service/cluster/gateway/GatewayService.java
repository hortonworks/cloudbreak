package com.sequenceiq.cloudbreak.service.cluster.gateway;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.UpdateGatewayTopologiesJson;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayTopologyJsonValidator;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.repository.GatewayRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class GatewayService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayService.class);

    @Inject
    private StackService stackService;

    @Inject
    private GatewayRepository gatewayRepository;

    @Inject
    private ConversionService conversionService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private GatewayTopologyJsonValidator gatewayTopologyJsonValidator;

    @Inject
    private ReactorFlowManager reactorFlowManager;

    public GatewayJson updateGatewayTopologies(long stackId, UpdateGatewayTopologiesJson request) {
        try {
            return transactionService.required(() -> tryUpdatingGateway(stackId, request));
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof BadRequestException || e.getCause() instanceof FlowsAlreadyRunningException) {
                throw e.getCause();
            } else {
                throw new TransactionRuntimeExecutionException(e);
            }
        }
    }

    private GatewayJson tryUpdatingGateway(Long stackId, UpdateGatewayTopologiesJson request) {
        Stack stack = stackService.getById(stackId);
        validateRequest(stackId, request, stack);
        Gateway gateway = stack.getCluster().getGateway();
        Set<GatewayTopology> currentTopologies = copyTopologies(gateway);
        gateway = setTopologiesForGateway(gateway, request);
        try {
            reactorFlowManager.triggerEphemeralUpdate(stackId);
        } catch (FlowsAlreadyRunningException e) {
            revertTopologyUpdate(stackId, currentTopologies);
            throw e;
        }
        return conversionService.convert(gateway, GatewayJson.class);
    }

    private Set<GatewayTopology> copyTopologies(Gateway gateway) {
        return gateway.getTopologies().stream().map(GatewayTopology::copy).collect(Collectors.toSet());
    }

    private void validateRequest(Long stackId, UpdateGatewayTopologiesJson request, Stack stack) {
        UpdateGatewayTopologiesJsonValidator validator = new UpdateGatewayTopologiesJsonValidator(gatewayTopologyJsonValidator, stackId, stack);
        ValidationResult validationResult = validator.validate(request);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private Gateway setTopologiesForGateway(Gateway gateway, UpdateGatewayTopologiesJson request) {
        Set<GatewayTopology> existingTopologies = gateway.getTopologies();
        request.getTopologies().forEach(topologyUpdate -> updateServicesForTopology(existingTopologies, topologyUpdate));
        return gatewayRepository.save(gateway);
    }

    private void updateServicesForTopology(Set<GatewayTopology> existingTopologies, GatewayTopologyJson topologyUpdate) {
        Optional<GatewayTopology> existingTopologyForUpdate = findExistingTopologyForUpdate(existingTopologies, topologyUpdate);
        existingTopologyForUpdate.ifPresent(existingTopology -> {
            try {
                ExposedServices exposedServices = conversionService.convert(topologyUpdate, ExposedServices.class);
                existingTopology.setExposedServices(new Json(exposedServices));
            } catch (JsonProcessingException e) {
                LOGGER.error("Exception during Json creation from a valid Java object.", e);
            }
        });
    }

    private Optional<GatewayTopology> findExistingTopologyForUpdate(Set<GatewayTopology> existingTopologies, GatewayTopologyJson topologyUpdate) {
        return existingTopologies.stream()
                .filter(existingTopology -> existingTopology.getTopologyName().equals(topologyUpdate.getTopologyName()))
                .findFirst();
    }

    private void revertTopologyUpdate(Long stackId, Set<GatewayTopology> topologiesToBeReverted) {
        Stack stack = stackService.getById(stackId);
        Gateway gateway = stack.getCluster().getGateway();
        topologiesToBeReverted.forEach(revert -> gateway.getTopologies().stream()
                .filter(topology -> topology.getTopologyName().equals(revert.getTopologyName()))
                .findFirst()
                .ifPresent(topology -> topology.setExposedServices(revert.getExposedServices())));
        gatewayRepository.save(gateway);
    }
}
