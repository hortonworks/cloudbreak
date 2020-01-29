package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressFBWarnings("SE_BAD_FIELD")
public class PlatformNoSqlTablesResponse implements Serializable {

    private List<PlatformNoSqlTableResponse> noSqlTables = new ArrayList<>();

    public PlatformNoSqlTablesResponse() {
    }

    public PlatformNoSqlTablesResponse(@NotNull List<PlatformNoSqlTableResponse> noSqlTables) {
        this.noSqlTables = noSqlTables;
    }

    public List<PlatformNoSqlTableResponse> getNoSqlTables() {
        return noSqlTables;
    }

    public void setNoSqlTables(List<PlatformNoSqlTableResponse> noSqlTables) {
        this.noSqlTables = noSqlTables;
    }

    @Override
    public String toString() {
        return "PlatformNoSqlTablesResponse{" +
                "noSqlTables=" + noSqlTables +
                '}';
    }
}
