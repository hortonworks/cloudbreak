package com.sequenceiq.cloudbreak.converter.v2.cli;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.VaultService;

@Component
public class BlueprintToBlueprintRequestConverter
        extends AbstractConversionServiceAwareConverter<Blueprint, BlueprintRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintToBlueprintRequestConverter.class);

    @Inject
    private VaultService vaultService;

    @Override
    public BlueprintRequest convert(Blueprint source) {
        BlueprintRequest blueprintRequest = new BlueprintRequest();
        blueprintRequest.setName("");
        blueprintRequest.setDescription(source.getDescription());
        blueprintRequest.setAmbariBlueprint(vaultService.resolveSingleValue(source.getBlueprintText()));
        return blueprintRequest;
    }

}
