package com.sequenceiq.cloudbreak.domain;

import java.time.Duration;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class ShowTerminatedClustersPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "showterminatedclusterpreferences_generator")
    @SequenceGenerator(name = "showterminatedclusterpreferences_generator", sequenceName = "showterminatedclusterspreferences_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "show_terminated")
    private Boolean active;

    @Convert(converter = DurationToLongConverter.class)
    @Column(name = "show_terminated_timeout_millisecs")
    private Duration timeoutMillisecs = Duration.ZERO;

    public ShowTerminatedClustersPreferences() {
    }

    public ShowTerminatedClustersPreferences(Boolean active, Long timeoutMillisecs) {
        this.active = active;
        this.timeoutMillisecs = Duration.ofMillis(timeoutMillisecs);
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setTimeout(Duration timeout) {
        this.timeoutMillisecs = timeout;
    }

    public Boolean isActive() {
        return active != null
                && timeoutMillisecs != null
                && active
                && timeoutMillisecs.toSeconds() > 0;
    }

    public Duration getTimeout() {
        return timeoutMillisecs != null ? timeoutMillisecs : Duration.ZERO;
    }
}
