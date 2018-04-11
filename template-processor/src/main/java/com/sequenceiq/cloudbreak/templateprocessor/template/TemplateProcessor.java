package com.sequenceiq.cloudbreak.templateprocessor.template;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.sequenceiq.cloudbreak.templateprocessor.processor.PreparationObject;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

import static com.sequenceiq.cloudbreak.api.model.ExecutorType.CONTAINER;

@Component
public class TemplateProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateProcessor.class);

    private final Handlebars handlebars = HandlebarUtils.handlebars();

    public String process(String sourceTemplate, PreparationObject source, Map<String, Object> customProperties) throws IOException {
        long started = System.currentTimeMillis();
        String generateBlueprint = generateBlueprintWithParameters(sourceTemplate, source, customProperties);
        long generationTime = System.currentTimeMillis() - started;
        LOGGER.info("The template text processed successfully by the EL based template processor under {} ms, the text after processing is: {}",
                generationTime,
                JsonUtil.minify(generateBlueprint));
        return generateBlueprint;
    }

    private String generateBlueprintWithParameters(String sourceTemplate, PreparationObject source, Map<String, Object> customProperties)
            throws IOException {
        Template template = handlebars.compileInline(sourceTemplate, "{{{", "}}}");
        return template.apply(prepareTemplateObject(source, customProperties));
    }

    private Map<String, Object> prepareTemplateObject(PreparationObject source, Map<String, Object> customProperties) throws IOException {


        Map<String, Object> blueprintInputs = source.getBlueprintView().getBlueprintInputs();
        blueprintInputs.putAll(customProperties);

        return new TemplateModelContextBuilder()
                .withClusterAdminFirstname(source.getGeneralClusterConfigs().getUserName())
                .withClusterAdminLastname(source.getGeneralClusterConfigs().getUserName())
                .withClusterAdminPassword(source.getGeneralClusterConfigs().getPassword())
                .withLlapNodeCounts(source.getGeneralClusterConfigs().getNodeCount() - 1)
                .withContainerExecutor(CONTAINER.equals(source.getGeneralClusterConfigs().getExecutorType()))
                .withEnableKnoxGateway(source.getGatewayView() == null ? false : source.getGatewayView().getEnableGateway())
                .withAdminEmail(source.getGeneralClusterConfigs().getIdentityUserEmail())
                .withClusterName(source.getGeneralClusterConfigs().getClusterName())
                .withLdap(source.getLdapConfig().orElse(null))
                .withGateway(source.getGatewayView())
                .withStackType(source.getBlueprintView().getType())
                .withStackVersion(source.getBlueprintView().getVersion())
                .withRdsConfigs(source.getRdsConfigs())
                .withFileSystemConfigs(source.getFileSystemConfigurationView().orElse(null))
                .withCustomProperties(blueprintInputs)
                .withHdfConfigs(source.getHdfConfigs())
                .build();
    }
}
