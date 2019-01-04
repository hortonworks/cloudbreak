package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class ClusterTemplateStringToClouderaApiClusterTemplateConverter extends AbstractConversionServiceAwareConverter<String, ApiClusterTemplate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateStringToClouderaApiClusterTemplateConverter.class);

    @Override
    public ApiClusterTemplate convert(String source) {
        try {
            return JsonUtil.readValue(source, ApiClusterTemplate.class);
        } catch (IOException e) {
            LOGGER.info("Invalid Cloudera template json", e);
            throw new CloudbreakServiceException(e);
        }
    }
}

