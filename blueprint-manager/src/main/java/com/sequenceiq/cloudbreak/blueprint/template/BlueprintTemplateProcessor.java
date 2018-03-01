package com.sequenceiq.cloudbreak.blueprint.template;

import static com.sequenceiq.cloudbreak.api.model.ExecutorType.CONTAINER;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.domain.Cluster;
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

    private String generateBlueprintWithParameters(String sourceTemplate, BlueprintPreparationObject source, Map<String, Object> customProperties)
            throws IOException {
        Template template = handlebars.compileInline(sourceTemplate, "{{{", "}}}");
        return template.apply(prepareTemplateObject(source, customProperties));
    }

    private Map<String, Object> prepareTemplateObject(BlueprintPreparationObject source, Map<String, Object> customProperties) throws IOException {
        Cluster cluster = source.getCluster();

        Map blueprintInputs = cluster.getBlueprintInputs().get(Map.class);
        if (blueprintInputs == null) {
            blueprintInputs = new HashMap<>();
        }
        blueprintInputs.putAll(customProperties);

        return new BlueprintTemplateModelContextBuilder()
                .withAmbariDatabase(source.getAmbariDatabase())
                .withClusterAdminFirstname(cluster.getUserName())
                .withClusterAdminLastname(cluster.getUserName())
                .withClusterAdminPassword(cluster.getPassword())
                .withLlapNodeCounts(cluster.getClusterNodeCount() - 1)
                .withContainerExecutor(CONTAINER.equals(cluster.getExecutorType()))
                .withEnableKnoxGateway(cluster.getGateway().getEnableGateway())
                .withAdminEmail(source.getIdentityUser().getUsername())
                .withClusterName(cluster.getName())
                .withLdap(cluster.getLdapConfig())
                .withGateway(cluster.getGateway())
                .withStackType(source.getBlueprintStackInfo().getType())
                .withStackVersion(source.getBlueprintStackInfo().getVersion())
                .withRdsConfigs(source.getRdsConfigs())
                .withCustomProperties(blueprintInputs)
                .withNifiTargets(source.getHdfConfigs().getNodeEntities())
                .build();
    }
}
