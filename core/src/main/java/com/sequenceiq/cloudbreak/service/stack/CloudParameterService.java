package com.sequenceiq.cloudbreak.service.stack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformImagesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformImagesResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformOrchestratorsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformOrchestratorsResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVariantsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVariantsResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesResult;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParametersRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParametersResult;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.PlatformImages;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class CloudParameterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudParameterService.class);

    @Value("${cb.enable.custom.image:false}")
    private Boolean enableCustomImage;

    @Inject
    private EventBus eventBus;

    public PlatformVariants getPlatformVariants() {
        LOGGER.debug("Get platform variants");
        GetPlatformVariantsRequest getPlatformVariantsRequest = new GetPlatformVariantsRequest();
        eventBus.notify(getPlatformVariantsRequest.selector(), Event.wrap(getPlatformVariantsRequest));
        try {
            GetPlatformVariantsResult res = getPlatformVariantsRequest.await();
            LOGGER.info("Platform variants result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform variants", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformVariants();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform variants", e);
            throw new OperationException(e);
        }
    }

    public String getPlatformByVariant(String requestedVariant) {
        PlatformVariants platformVariants = getPlatformVariants();
        for (Map.Entry<Platform, Collection<Variant>> platformCollectionEntry : platformVariants.getPlatformToVariants().entrySet()) {
            for (Variant variant : platformCollectionEntry.getValue()) {
                if (variant.value().equals(requestedVariant)) {
                    return platformCollectionEntry.getKey().value();
                }
            }
        }
        return null;
    }

    public PlatformDisks getDiskTypes() {
        LOGGER.debug("Get platform disktypes");
        GetDiskTypesRequest getDiskTypesRequest = new GetDiskTypesRequest();
        eventBus.notify(getDiskTypesRequest.selector(), Event.wrap(getDiskTypesRequest));
        try {
            GetDiskTypesResult res = getDiskTypesRequest.await();
            LOGGER.info("Platform disk types result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform disk types", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformDisks();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform disk types", e);
            throw new OperationException(e);
        }
    }

    public PlatformVirtualMachines getVmtypes(Boolean extended) {
        if (extended == null) {
            extended = true;
        }
        LOGGER.debug("Get platform vm types");
        GetVirtualMachineTypesRequest getVirtualMachineTypesRequest = new GetVirtualMachineTypesRequest(extended);
        eventBus.notify(getVirtualMachineTypesRequest.selector(), Event.wrap(getVirtualMachineTypesRequest));
        try {
            GetVirtualMachineTypesResult res = getVirtualMachineTypesRequest.await();
            LOGGER.info("Platform vm types result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform vm types", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformVirtualMachines();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform vm types", e);
            throw new OperationException(e);
        }
    }

    public PlatformRegions getRegions() {
        LOGGER.debug("Get platform regions");
        GetPlatformRegionsRequest getPlatformRegionsRequest = new GetPlatformRegionsRequest();
        eventBus.notify(getPlatformRegionsRequest.selector(), Event.wrap(getPlatformRegionsRequest));
        try {
            GetPlatformRegionsResult res = getPlatformRegionsRequest.await();
            LOGGER.info("Platform regions result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform regions", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformRegions();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform regions", e);
            throw new OperationException(e);
        }
    }

    public PlatformOrchestrators getOrchestrators() {
        LOGGER.debug("Get platform orchestrators");
        GetPlatformOrchestratorsRequest getPlatformOrchestratorsRequest = new GetPlatformOrchestratorsRequest();
        eventBus.notify(getPlatformOrchestratorsRequest.selector(), Event.wrap(getPlatformOrchestratorsRequest));
        try {
            GetPlatformOrchestratorsResult res = getPlatformOrchestratorsRequest.await();
            LOGGER.info("Platform orchestrators result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform orchestrators", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformOrchestrators();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform orchestrators", e);
            throw new OperationException(e);
        }
    }

    public PlatformImages getImages() {
        LOGGER.debug("Get platform orchestrators");
        GetPlatformImagesRequest getPlatformImagesRequest = new GetPlatformImagesRequest();
        eventBus.notify(getPlatformImagesRequest.selector(), Event.wrap(getPlatformImagesRequest));
        try {
            GetPlatformImagesResult res = getPlatformImagesRequest.await();
            LOGGER.info("Platform images result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform images", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformImages();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform images", e);
            throw new OperationException(e);
        }
    }

    public Map<Platform, PlatformParameters> getPlatformParameters() {
        LOGGER.debug("Get platform parameters for");
        PlatformParametersRequest parametersRequest = new PlatformParametersRequest();
        eventBus.notify(parametersRequest.selector(), Event.wrap(parametersRequest));
        try {
            PlatformParametersResult res = parametersRequest.await();
            LOGGER.info("Platform parameter result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform parameters", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformParameters();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting platform parameters", e);
            throw new OperationException(e);
        }
    }

    public SpecialParameters getSpecialParameters() {
        LOGGER.debug("Get special platform parameters");
        Map<String, Boolean> specialParameters = new HashMap<>();
        specialParameters.put("enableCustomImage", enableCustomImage);
        return new SpecialParameters(specialParameters);
    }
}
