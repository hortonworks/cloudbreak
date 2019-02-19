package com.sequenceiq.cloudbreak.template;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.TagType;
import com.github.jknack.handlebars.Template;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class TemplateProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateProcessor.class);

    private final Handlebars handlebars = HandlebarUtils.handlebars();

    public String process(String sourceTemplate, TemplatePreparationObject source, Map<String, Object> customProperties) throws IOException {
        long started = System.currentTimeMillis();
        String generateClusterDefinition = generateTemplateWithParameters(sourceTemplate, source, customProperties);
        long generationTime = System.currentTimeMillis() - started;
        LOGGER.debug("The template text processed successfully by the EL based template processor under {} ms, the text after processing is: {}",
                generationTime, JsonUtil.minify(generateClusterDefinition));
        return generateClusterDefinition;
    }

    public List<String> queryParameters(String sourceTemplate) throws IOException {
        long started = System.currentTimeMillis();
        List<String> clusterDefinitionParameters = queryTemplateParameters(sourceTemplate);
        long generationTime = System.currentTimeMillis() - started;
        LOGGER.debug("The template text processed successfully by the EL based template processor under {} ms, the parameters are: {}",
                generationTime, clusterDefinitionParameters);
        return clusterDefinitionParameters;
    }

    private String generateTemplateWithParameters(String sourceTemplate, TemplatePreparationObject source, Map<String, Object> customProperties)
            throws IOException {
        Template template = handlebars.compileInline(sourceTemplate, HandlebarTemplate.DEFAULT_PREFIX.key(), HandlebarTemplate.DEFAULT_POSTFIX.key());
        return template.apply(prepareTemplateObject(source, customProperties));
    }

    private List<String> queryTemplateParameters(String sourceTemplate)
            throws IOException {
        Template template = handlebars.compileInline(sourceTemplate, HandlebarTemplate.DEFAULT_PREFIX.key(), HandlebarTemplate.DEFAULT_POSTFIX.key());
        return template.collect(TagType.VAR);
    }

    private Map<String, Object> prepareTemplateObject(TemplatePreparationObject source, Map<String, Object> customProperties) {
        source.getFixInputs().putAll(customProperties);
        return new TemplateModelContextBuilder()
                .withLdap(source.getLdapConfig().orElse(null))
                .withKerberos(source.getKerberosConfig().orElse(null))
                .withSharedServiceConfigs(source.getSharedServiceConfigs().orElse(null))
                .withComponents(source.getClusterDefinitionView().getComponents())
                .withGateway(source.getGatewayView())
                .withClusterDefinitionView(source.getClusterDefinitionView())
                .withRdsConfigs(source.getRdsConfigs())
                .withFileSystemConfigs(source.getFileSystemConfigurationView().orElse(null))
                .withCustomInputs(source.getCustomInputs())
                .withFixInputs(source.getFixInputs())
                .withGeneralClusterConfigs(source.getGeneralClusterConfigs())
                .withHdfConfigs(source.getHdfConfigs().orElse(null))
                .build();
    }
}
