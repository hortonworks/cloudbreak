package com.sequenceiq.cloudbreak.template;

public interface ClusterDefinitionUpdater {
    String getClusterDefinitionText(TemplatePreparationObject source);

    String getVariant();
}
