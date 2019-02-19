package com.sequenceiq.cloudbreak.converter.v4.clusterdefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.requests.ClusterDefinitionV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;

@Component
public class ClusterDefinitionToClusterDefinitionV4RequestConverter
        extends AbstractConversionServiceAwareConverter<ClusterDefinition, ClusterDefinitionV4Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDefinitionToClusterDefinitionV4RequestConverter.class);

    @Override
    public ClusterDefinitionV4Request convert(ClusterDefinition source) {
        ClusterDefinitionV4Request clusterDefinitionV4Request = new ClusterDefinitionV4Request();
        clusterDefinitionV4Request.setName(source.getName());
        clusterDefinitionV4Request.setDescription(source.getDescription());
        clusterDefinitionV4Request.setClusterDefinition(source.getClusterDefinitionText());
        clusterDefinitionV4Request.setDescription(source.getDescription());
        return clusterDefinitionV4Request;
    }

}
