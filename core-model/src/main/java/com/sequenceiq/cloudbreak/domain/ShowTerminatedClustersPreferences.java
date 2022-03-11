package com.sequenceiq.cloudbreak.domain;

import java.time.Duration;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotNull;

@Entity
public class ShowTerminatedClustersPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "showterminatedclusterpreferences_generator")
    @SequenceGenerator(name = "showterminatedclusterpreferences_generator", sequenceName = "showterminatedclusterspreferences_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    @Column(name = "show_terminated")
    private Boolean active;

    @Convert(converter = DurationToLongConverter.class)
    @Column(name = "show_terminated_timeout_millisecs")
    private Duration timeout = Duration.ZERO;

    public ShowTerminatedClustersPreferences() {
    }

    public ShowTerminatedClustersPreferences(Boolean active, Long timeout) {
        this.active = active;
        this.timeout = Duration.ofMillis(timeout);
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Boolean isActive() {
        return active
                && timeout != null
                && timeout.toSeconds() > 0;
    }

    public Duration getTimeout() {
        return timeout != null ? timeout : Duration.ZERO;
    }

    @Override
    public String toString() {
        return "ShowTerminatedClustersPreferences{" +
                "id=" + id +
                ", active=" + active +
                ", timeout=" + timeout +
                '}';
    }
}
