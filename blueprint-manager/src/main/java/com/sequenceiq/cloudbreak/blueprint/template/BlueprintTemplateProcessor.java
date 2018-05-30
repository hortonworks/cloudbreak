package com.sequenceiq.cloudbreak.blueprint.template;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.TagType;
import com.github.jknack.handlebars.Template;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.HandlebarTemplate;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class BlueprintTemplateProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintTemplateProcessor.class);

    private final Handlebars handlebars = HandlebarUtils.handlebars();

    public String process(String sourceTemplate, BlueprintPreparationObject source, Map<String, Object> customProperties) throws IOException {
        long started = System.currentTimeMillis();
        String generateBlueprint = generateBlueprintWithParameters(sourceTemplate, source, customProperties);
        long generationTime = System.currentTimeMillis() - started;
        LOGGER.info("The blueprint text processed successfully by the EL based template processor under {} ms, the text after processing is: {}",
                generationTime,
                JsonUtil.minify(generateBlueprint));
        return generateBlueprint;
    }

    public List<String> queryParameters(String sourceTemplate) throws IOException {
        long started = System.currentTimeMillis();
        List<String> blueprintParameters = queryBlueprintParameters(sourceTemplate);
        long generationTime = System.currentTimeMillis() - started;
        LOGGER.info("The blueprint text processed successfully by the EL based template processor under {} ms, the parameters are: {}",
                generationTime,
                blueprintParameters.toString());
        return blueprintParameters;
    }

    private String generateBlueprintWithParameters(String sourceTemplate, BlueprintPreparationObject source, Map<String, Object> customProperties)
            throws IOException {
        Template template = handlebars.compileInline(sourceTemplate, HandlebarTemplate.DEFAULT_PREFIX.key(), HandlebarTemplate.DEFAULT_POSTFIX.key());
        return template.apply(prepareTemplateObject(source, customProperties));
    }

    private List<String> queryBlueprintParameters(String sourceTemplate)
            throws IOException {
        Template template = handlebars.compileInline(sourceTemplate, HandlebarTemplate.DEFAULT_PREFIX.key(), HandlebarTemplate.DEFAULT_POSTFIX.key());
        return template.collect(TagType.VAR);
    }

    private Map<String, Object> prepareTemplateObject(BlueprintPreparationObject source, Map<String, Object> customProperties) throws IOException {
        source.getFixInputs().putAll(customProperties);

        return new BlueprintTemplateModelContextBuilder()
                .withLdap(source.getLdapConfig().orElse(null))
                .withSharedServiceConfigs(source.getSharedServiceConfigs().orElse(null))
                .withComponents(source.getBlueprintView().getComponents())
                .withGateway(source.getGatewayView())
                .withBlueprintView(source.getBlueprintView())
                .withRdsConfigs(source.getRdsConfigs())
                .withFileSystemConfigs(source.getFileSystemConfigurationView().orElse(null))
                .withCustomInputs(source.getCustomInputs())
                .withFixInputs(source.getFixInputs())
                .withGeneralClusterConfigs(source.getGeneralClusterConfigs())
                .withHdfConfigs(source.getHdfConfigs().orElse(null))
                .build();
    }
}
