package com.sequenceiq.periscope.model.yarn;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class NewNodeManagerCandidates {

    @JsonIgnore
    private String recommendActionTime;

    private List<Candidate> candidates = new ArrayList<>(1);

    public String getRecommendActionTime() {
        return recommendActionTime;
    }

    public void setRecommendActionTime(String recommendActionTime) {
        this.recommendActionTime = recommendActionTime;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    @Override
    public String toString() {
        return "NewNodeManagerCandidates{" +
                "Candidates=" + candidates +
                '}';
    }

    public static class Candidate {
        private String modelName;

        private Integer count;

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        @Override
        public String toString() {
            return "Candidate{" +
                    "modelName='" + modelName + '\'' +
                    ", count=" + count +
                    '}';
        }
    }
}
