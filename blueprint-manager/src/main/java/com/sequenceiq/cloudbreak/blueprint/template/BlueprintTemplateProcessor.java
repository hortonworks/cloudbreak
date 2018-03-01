package com.sequenceiq.cloudbreak.blueprint.template;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class BlueprintTemplateProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintTemplateProcessor.class);

    public String process(String blueprintText, Cluster cluster, Iterable<RDSConfig> rdsConfigs, AmbariDatabase ambariDatabase) throws IOException {
        long started = System.currentTimeMillis();
        String generateBlueprint = generateBlueprintWithParameters(blueprintText, cluster, rdsConfigs, ambariDatabase);
        long generationTime = System.currentTimeMillis() - started;
        LOGGER.info("The blueprint text processed successfully by the EL based template processor under {} ms, the text after processing is: {}",
                generationTime,
                JsonUtil.minify(generateBlueprint));
        return generateBlueprint;
    }

    private String generateBlueprintWithParameters(String blueprintText, Cluster cluster, Iterable<RDSConfig> rdsConfigs, AmbariDatabase ambariDatabase)
            throws IOException {
        Handlebars handlebars = new Handlebars();
        handlebars.with(EscapingStrategy.NOOP);
        handlebars.registerHelperMissing((context, options) -> options.fn.text());
        Template template = handlebars.compileInline(blueprintText, "{{{", "}}}");
        Map modelContext = prepareTemplateObject(cluster.getBlueprintInputs().get(Map.class), cluster, rdsConfigs, ambariDatabase);
        return template.apply(modelContext);
    }

    private Map<String, Object> prepareTemplateObject(Map<String, Object> blueprintInputs, Cluster cluster, Iterable<RDSConfig> rdsConfigs,
            AmbariDatabase ambariDatabase) {
        if (blueprintInputs == null) {
            blueprintInputs = new HashMap<>();
        }

        return new BlueprintTemplateModelContextBuilder()
                .withAmbariDatabase(ambariDatabase)
                .withClusterName(cluster.getName())
                .withLdap(cluster.getLdapConfig())
                .withGateway(cluster.getGateway())
                .withRdsConfigs(rdsConfigs)
                .withCustomProperties(blueprintInputs)
                .build();
    }
}
