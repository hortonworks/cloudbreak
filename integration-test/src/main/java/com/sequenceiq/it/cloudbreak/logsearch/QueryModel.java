package com.sequenceiq.it.cloudbreak.logsearch;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryModel {

    private Long id;

    private String name;

    private String label;

    private String value;

    @JsonProperty(value = "isExclude")
    private boolean excluded;

    private QueryModel(AtomicLong idSequence, QueryType nameType, String value) {
        this.id = idSequence.incrementAndGet();
        this.name = nameType.getName();
        this.label = encodeUrlString(nameType.getLabel());
        this.value = encodeUrlString(value);
    }

    private QueryModel(Long id, QueryType nameType, String value) {
        this.id = id;
        this.name = nameType.getName();
        this.label = encodeUrlString(nameType.getLabel());
        this.value = encodeUrlString(value);
        this.excluded = false;
    }

    private static String encodeUrlString(String urlString) {
        return URLEncoder.encode(urlString, StandardCharsets.UTF_8).
                replace("+", "%20");
    }

    public static QueryModel create(AtomicLong idSequence, QueryType nameType, String value) {
        return new QueryModel(idSequence, nameType, value);
    }

    public static QueryModel create(Long id, QueryType nameType, String value) {
        return new QueryModel(id, nameType, value);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isExcluded() {
        return excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
    }
}
