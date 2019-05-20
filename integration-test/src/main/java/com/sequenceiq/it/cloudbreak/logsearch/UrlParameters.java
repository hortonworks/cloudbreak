package com.sequenceiq.it.cloudbreak.logsearch;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.collections.Maps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;

public class UrlParameters {

    private static final Logger LOGGER = LoggerFactory.getLogger(UrlParameters.class);

    private String timeRangeType = "LAST";

    private String timeRangeUnit = "h";

    private String timeRangeInterval;

    private String sortingKey = "logTime";

    private String sortingType = "desc";

    private Integer pageSize = 100;

    private Integer page = 0;

    private String query;

    private String components;

    private UrlParameters(String timeRangeInterval, String query, String components) {
        this.timeRangeInterval = timeRangeInterval;
        this.query = query;
        this.components = components;
    }

    public static Optional<String> getUrlQueryString(List<QueryModel> queryModels, String timeRangeInterval,
            String components, ObjectMapper objectMapper) {
        Map<String, Object> parametersMap = createParametersMap(queryModels, timeRangeInterval, components, objectMapper);
        if (!parametersMap.isEmpty()) {
            parametersMap.values().removeIf(Objects::isNull);
            Joiner.MapJoiner mapJoiner = Joiner.on(';').withKeyValueSeparator("=");
            return Optional.of(mapJoiner.join(parametersMap));
        }
        return Optional.empty();
    }

    public static Map<String, Object> createParametersMap(List<QueryModel> queryModels, String timeRangeInterval,
            String components, ObjectMapper objectMapper) {
        try {
            UrlParameters urlParameters = new UrlParameters(timeRangeInterval, objectMapper.writeValueAsString(queryModels), components);
            return objectMapper.convertValue(urlParameters, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            LOGGER.info("Error during json convert:", e);
            return Maps.newHashMap();
        }
    }

    public String getTimeRangeType() {
        return timeRangeType;
    }

    public void setTimeRangeType(String timeRangeType) {
        this.timeRangeType = timeRangeType;
    }

    public String getTimeRangeUnit() {
        return timeRangeUnit;
    }

    public void setTimeRangeUnit(String timeRangeUnit) {
        this.timeRangeUnit = timeRangeUnit;
    }

    public String getTimeRangeInterval() {
        return timeRangeInterval;
    }

    public void setTimeRangeInterval(String timeRangeInterval) {
        this.timeRangeInterval = timeRangeInterval;
    }

    public String getSortingKey() {
        return sortingKey;
    }

    public void setSortingKey(String sortingKey) {
        this.sortingKey = sortingKey;
    }

    public String getSortingType() {
        return sortingType;
    }

    public void setSortingType(String sortingType) {
        this.sortingType = sortingType;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getComponents() {
        return components;
    }

    public void setComponents(String components) {
        this.components = components;
    }
}
