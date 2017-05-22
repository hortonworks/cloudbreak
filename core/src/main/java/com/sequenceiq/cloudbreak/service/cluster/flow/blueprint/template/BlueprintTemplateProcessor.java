package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.template;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;

@Component
public class BlueprintTemplateProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintTemplateProcessor.class);

    private final MustacheFactory mustacheFactory = new DefaultMustacheFactory();

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    public String process(String blueprintText, Cluster cluster, Set<RDSConfig> rdsConfigs) throws IOException {
        String generateBlueprint = generateBlueprintWithParameters(blueprintText, cluster, rdsConfigs);
        LOGGER.info("The blueprint for ambari install was successfully generated with mustache, the generated blueprint is: {}", generateBlueprint);
        return generateBlueprint;
    }

    private String generateBlueprintWithParameters(String blueprintText, Cluster cluster, Set<RDSConfig> rdsConfigs) throws IOException {
        Writer writer = new StringWriter();
        Mustache mustache = mustacheFactory.compile(new StringReader(blueprintText), "bp");
        mustache.execute(writer, prepareTemplateObject(cluster.getBlueprintInputs().get(Map.class), cluster, rdsConfigs));
        return writer.toString();
    }

    private Map<String, Object> prepareTemplateObject(Map<String, Object> blueprintInputs, Cluster cluster, Set<RDSConfig> rdsConfigs) {
        if (blueprintInputs == null) {
            blueprintInputs = new HashMap<>();
        }

        Map<String, Object> result = new BlueprintTemplateModelContextBuilder()
                .withAmbariDatabase(clusterComponentConfigProvider.getAmbariDatabase(cluster.getId()))
                .withClusterName(cluster.getName())
                .withLdap(cluster.getLdapConfig())
                .withRdsConfigs(rdsConfigs)
                .withCustomProperties(blueprintInputs)
                .build();

        return result;
    }

}
