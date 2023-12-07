package com.sequenceiq.mock.model;

public class YarnRecommendation {

    private RecommendationType recommendationType;

    private Long lastRecommendedTs;

    public YarnRecommendation(RecommendationType recommendationType, Long lastRecommendedTs) {
        this.recommendationType = recommendationType;
        this.lastRecommendedTs = lastRecommendedTs;
    }

    public RecommendationType getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(RecommendationType recommendationType) {
        this.recommendationType = recommendationType;
    }

    public Long getLastRecommendedTs() {
        return lastRecommendedTs;
    }

    public void setLastRecommendedTs(Long lastRecommendedTs) {
        this.lastRecommendedTs = lastRecommendedTs;
    }
}
