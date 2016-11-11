package com.sequenceiq.periscope.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PrometheusResponse {

    @JsonProperty("status")
    private String status;
    @JsonProperty("data")
    private Data data;

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("data")
    public Data getData() {
        return data;
    }

    @JsonProperty("data")
    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

        @JsonProperty("resultType")
        private String resultType;
        @JsonProperty("result")
        private List<Result> result;

        @JsonProperty("resultType")
        public String getResultType() {
            return resultType;
        }

        @JsonProperty("resultType")
        public void setResultType(String resultType) {
            this.resultType = resultType;
        }

        @JsonProperty("result")
        public void setResult(List<Result> result) {
            this.result = result;
        }

        @JsonProperty("result")
        public List<Result> getResult() {
            return result;
        }
    }

    public static class Result {

        @JsonProperty("metric")
        private Metric metric;
        @JsonProperty("values")
        private List<List<Object>> values;

        @JsonProperty("metric")
        public Metric getMetric() {
            return metric;
        }

        @JsonProperty("metric")
        public void setMetric(Metric metric) {
            this.metric = metric;
        }

        @JsonProperty("values")
        public List<List<Object>> getValues() {
            return values;
        }

        @JsonProperty("values")
        public void setValues(List<List<Object>> values) {
            this.values = values;
        }
    }

    public static class Metric {

        @JsonProperty("__name__")
        private String name;
        @JsonProperty("alertname")
        private String alertname;
        @JsonProperty("alertstate")
        private String alertstate;
        @JsonProperty("severity")
        private String severity;

        @JsonProperty("__name__")
        public String getName() {
            return name;
        }

        @JsonProperty("__name__")
        public void setName(String name) {
            this.name = name;
        }

        @JsonProperty("alertname")
        public String getAlertname() {
            return alertname;
        }

        @JsonProperty("alertname")
        public void setAlertname(String alertname) {
            this.alertname = alertname;
        }

        @JsonProperty("alertstate")
        public String getAlertstate() {
            return alertstate;
        }

        @JsonProperty("alertstate")
        public void setAlertstate(String alertstate) {
            this.alertstate = alertstate;
        }

        @JsonProperty("severity")
        public String getSeverity() {
            return severity;
        }

        @JsonProperty("severity")
        public void setSeverity(String severity) {
            this.severity = severity;
        }
    }
}
