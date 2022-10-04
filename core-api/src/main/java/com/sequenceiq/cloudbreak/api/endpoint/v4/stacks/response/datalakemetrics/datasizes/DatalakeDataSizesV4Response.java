package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.datalakemetrics.datasizes;

public class DatalakeDataSizesV4Response {
    private Long databaseSizeInBytes;

    private Long hbaseAtlasEntityAuditEventsTableSizeInBytes;

    private Long hbaseAtlasJanusTableSizeInBytes;

    private Long solrVertexIndexCollectionSizeInBytes;

    private Long solrFullextIndexCollectionSizeInBytes;

    private Long solrEdgeIndexCollectionSizeInBytes;

    private Long solrRangerAuditsCollectionSizeInBytes;

    public Long getDatabaseSizeInBytes() {
        return databaseSizeInBytes;
    }

    public void setDatabaseSizeInBytes(Long databaseSizeInBytes) {
        this.databaseSizeInBytes = databaseSizeInBytes;
    }

    public Long getHbaseAtlasEntityAuditEventsTableSizeInBytes() {
        return hbaseAtlasEntityAuditEventsTableSizeInBytes;
    }

    public void setHbaseAtlasEntityAuditEventsTableSizeInBytes(Long hbaseAtlasEntityAuditEventsTableSizeInBytes) {
        this.hbaseAtlasEntityAuditEventsTableSizeInBytes = hbaseAtlasEntityAuditEventsTableSizeInBytes;
    }

    public Long getHbaseAtlasJanusTableSizeInBytes() {
        return hbaseAtlasJanusTableSizeInBytes;
    }

    public void setHbaseAtlasJanusTableSizeInBytes(Long hbaseAtlasJanusTableSizeInBytes) {
        this.hbaseAtlasJanusTableSizeInBytes = hbaseAtlasJanusTableSizeInBytes;
    }

    public Long getSolrVertexIndexCollectionSizeInBytes() {
        return solrVertexIndexCollectionSizeInBytes;
    }

    public void setSolrVertexIndexCollectionSizeInBytes(Long solrVertexIndexCollectionSizeInBytes) {
        this.solrVertexIndexCollectionSizeInBytes = solrVertexIndexCollectionSizeInBytes;
    }

    public Long getSolrFullextIndexCollectionSizeInBytes() {
        return solrFullextIndexCollectionSizeInBytes;
    }

    public void setSolrFullextIndexCollectionSizeInBytes(Long solrFullextIndexCollectionSizeInBytes) {
        this.solrFullextIndexCollectionSizeInBytes = solrFullextIndexCollectionSizeInBytes;
    }

    public Long getSolrEdgeIndexCollectionSizeInBytes() {
        return solrEdgeIndexCollectionSizeInBytes;
    }

    public void setSolrEdgeIndexCollectionSizeInBytes(Long solrEdgeIndexCollectionSizeInBytes) {
        this.solrEdgeIndexCollectionSizeInBytes = solrEdgeIndexCollectionSizeInBytes;
    }

    public Long getSolrRangerAuditsCollectionSizeInBytes() {
        return solrRangerAuditsCollectionSizeInBytes;
    }

    public void setSolrRangerAuditsCollectionSizeInBytes(Long solrRangerAuditsCollectionSizeInBytes) {
        this.solrRangerAuditsCollectionSizeInBytes = solrRangerAuditsCollectionSizeInBytes;
    }

    @Override
    public String toString() {
        return "DatalakeDataSizesV4Response {" +
                "databaseSizeInBytes='" + databaseSizeInBytes + '\'' +
                ", hbaseAtlasEntityAuditEventsTableSizeInBytes='" + hbaseAtlasEntityAuditEventsTableSizeInBytes + '\'' +
                ", hbaseAtlasJanusTableSizeInBytes='" + hbaseAtlasJanusTableSizeInBytes + '\'' +
                ", solrVertexIndexCollectionSizeInBytes='" + solrVertexIndexCollectionSizeInBytes + '\'' +
                ", solrFullextIndexCollectionSizeInBytes='" + solrFullextIndexCollectionSizeInBytes + '\'' +
                ", solrEdgeIndexCollectionSizeInBytes='" + solrEdgeIndexCollectionSizeInBytes + '\'' +
                ", solrRangerAuditsCollectionSizeInBytes='" + solrRangerAuditsCollectionSizeInBytes + '\'' +
                '}';
    }
}
