package com.sequenceiq.cloudbreak.datalakedr.converter;

public enum OperationEnum {

    STOP_SERVICES("Stop services"),
    START_SERVICES("Start services"),
    HBASE("HBase"),
    HBASE_ATLAS_JANUS(HBASE + "HBase atlasJanus"),
    HBASE_ATLAS_AUDIT(HBASE + "HBase atlasEntityAudit"),
    SOLR("Solr"),
    SOLR_DELETE("Solr delete"),
    SOLR_EDGE_INDEX("Solr edge_index"),
    SOLR_FULLTEXT_INDEX("Solr fulltext_index"),
    SOLR_RANGER_AUDITS("Solr ranger_audits"),
    SOLR_VERTEX_INDEX("Solr vertex_index"),
    SOLR_EDGE_INDEX_DELETE("Solr delete edge_index"),
    SOLR_FULLTEXT_INDEX_DELETE("Solr delete fulltext_index"),
    SOLR_RANGER_AUDITS_DELETE("Solr delete ranger_audits"),
    SOLR_VERTEX_INDEX_DELETE("Solr delete vertex_index"),
    DATABASE("Database");

    private final String description;

    OperationEnum(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
