package com.sequenceiq.it.cloudbreak.log;

class Step {
    private String step;

    private String description;

    private String json;

    Step(String step, String description) {
        this(step, description, null);
    }

    Step(String step, String description, String json) {
        this.step = step;
        this.description = description;
        this.json = json;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
