package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class BlueprintTemplateProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintTemplateProcessor.class);

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    public String process(String blueprintText, Cluster cluster, Iterable<RDSConfig> rdsConfigs) throws IOException {
        long started = System.currentTimeMillis();
        String generateBlueprint = generateBlueprintWithParameters(blueprintText, cluster, rdsConfigs);
        long generationTime = System.currentTimeMillis() - started;
        LOGGER.info("The blueprint was generated successfully under {} ms, the generated blueprint is: {}", generationTime, JsonUtil.minify(generateBlueprint));
        return generateBlueprint;
    }

    private String generateBlueprintWithParameters(String blueprintText, Cluster cluster, Iterable<RDSConfig> rdsConfigs) throws IOException {
        Handlebars handlebars = new Handlebars();
        handlebars.with(EscapingStrategy.NOOP);
        handlebars.registerHelperMissing((context, options) -> options.fn.text());
        Template template = handlebars.compileInline(blueprintText, "{{{", "}}}");

        return template.apply(prepareTemplateObject(cluster.getBlueprintInputs().get(Map.class), cluster, rdsConfigs));
    }

    private Map<String, Object> prepareTemplateObject(Map<String, Object> blueprintInputs, Cluster cluster, Iterable<RDSConfig> rdsConfigs) {
        if (blueprintInputs == null) {
            blueprintInputs = new HashMap<>();
        }

        return new BlueprintTemplateModelContextBuilder()
                .withAmbariDatabase(clusterComponentConfigProvider.getAmbariDatabase(cluster.getId()))
                .withClusterName(cluster.getName())
                .withLdap(cluster.getLdapConfig())
                .withGateway(cluster.getGateway())
                .withRdsConfigs(rdsConfigs)
                .withCustomProperties(blueprintInputs)
                .build();
    }
}
