package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class PollingData implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pollingdata_generator")
    @SequenceGenerator(name = "pollingdata_generator", sequenceName = "pollingdata_table")
    private Long id;

    private Long stackId;
    private String pollingMessage;
    private Long delay;
    private Integer numberOfPolls;
    private String status;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getPollingMessage() {
        return pollingMessage;
    }

    public void setPollingMessage(String pollingMessage) {
        this.pollingMessage = pollingMessage;
    }

    public Long getDelay() {
        return delay;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public Integer getNumberOfPolls() {
        return numberOfPolls;
    }

    public void setNumberOfPolls(Integer numberOfPolls) {
        this.numberOfPolls = numberOfPolls;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
