package com.sequenceiq.cloudbreak.structuredevent.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDefinitionDetails;

@Component
public class ClusterDefinitionToClusterDefinitionDetailsConverter extends AbstractConversionServiceAwareConverter<ClusterDefinition, ClusterDefinitionDetails> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDefinitionToClusterDefinitionDetailsConverter.class);

    @Override
    public ClusterDefinitionDetails convert(ClusterDefinition source) {
        ClusterDefinitionDetails clusterDefinitionDetails = new ClusterDefinitionDetails();
        clusterDefinitionDetails.setId(source.getId());
        clusterDefinitionDetails.setName(source.getName());
        clusterDefinitionDetails.setDescription(source.getDescription());
        clusterDefinitionDetails.setClusterDefinitionName(source.getStackName());
        clusterDefinitionDetails.setClusterDefinitionJson(source.getClusterDefinitionText());
        return clusterDefinitionDetails;
    }
}
