package com.sequenceiq.cloudbreak.cmtemplate;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.HandleBarModelKey;
import com.sequenceiq.cloudbreak.template.TemplateParameterFilter;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;

@Component
public class CentralBlueprintParameterQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralBlueprintParameterQueryService.class);

    @Inject
    private TemplateProcessor templateProcessor;

    @Inject
    private TemplateParameterFilter templateParameterFilter;

    public Set<String> queryDatalakeParameters(String sourceTemplate) throws BlueprintProcessingException {
        Set<String> blueprintParameters;
        try {
            blueprintParameters = templateParameterFilter.queryForDatalakeParameters(
                    HandleBarModelKey.DATALAKE,
                    templateProcessor.queryParameters(sourceTemplate));
        } catch (IOException e) {
            String message = String.format("Unable to query cluster template parameters from cluster template which was: %s", sourceTemplate);
            LOGGER.warn(message);
            throw new BlueprintProcessingException(message, e);
        }
        return blueprintParameters;
    }

    public Set<String> queryCustomParameters(String sourceTemplate) throws BlueprintProcessingException {
        Set<String> blueprintParameters;
        try {
            blueprintParameters = templateParameterFilter.queryForCustomParameters(templateProcessor.queryParameters(sourceTemplate));
        } catch (IOException e) {
            String message = String.format("Unable to query cluster template parameters from cluster template which was: %s", sourceTemplate);
            LOGGER.warn(message);
            throw new BlueprintProcessingException(message, e);
        }
        return blueprintParameters;
    }

}
