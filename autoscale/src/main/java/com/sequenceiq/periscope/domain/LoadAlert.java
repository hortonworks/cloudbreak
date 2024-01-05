package com.sequenceiq.periscope.domain;

import static com.sequenceiq.periscope.common.AlertConstants.PARAMETERS;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.converter.db.LoadAlertConfigAttributeConverter;

@Entity
@DiscriminatorValue("LOAD")
@NamedQueries({
        @NamedQuery(name = "LoadAlert.findByCluster", query = "SELECT c FROM LoadAlert c WHERE c.cluster.id= :clusterId AND c.id= :alertId"),
        @NamedQuery(name = "LoadAlert.findAllByCluster", query = "SELECT c FROM LoadAlert c WHERE c.cluster.id= :clusterId")
})
public class LoadAlert extends BaseAlert {

    @Convert(converter = LoadAlertConfigAttributeConverter.class)
    @Column(name = "load_alert_config")
    private LoadAlertConfiguration loadAlertConfiguration;

    public LoadAlertConfiguration getLoadAlertConfiguration() {
        return loadAlertConfiguration;
    }

    public void setLoadAlertConfiguration(LoadAlertConfiguration loadAlertConfiguration) {
        this.loadAlertConfiguration = loadAlertConfiguration;
    }

    public AlertType getAlertType() {
        return AlertType.LOAD;
    }

    public Map<String, String> getTelemetryParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PARAMETERS, loadAlertConfiguration.toString());
        return parameters;
    }
}
