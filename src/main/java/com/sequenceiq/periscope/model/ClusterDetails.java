package com.sequenceiq.periscope.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.periscope.registry.ClusterState;

@Entity
public class ClusterDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "template_generator")
    @SequenceGenerator(name = "template_generator", sequenceName = "sequence_table")
    private long id;
    @OneToOne(cascade = CascadeType.ALL)
    private Ambari ambari;
    private boolean appMovementAllowed;
    private ClusterState state = ClusterState.RUNNING;
    @OneToMany(cascade = CascadeType.ALL)
    private List<Alarm> alarms = new ArrayList<>();
    private int minSize;
    private int maxSize;
    private int coolDown;

    public ClusterDetails() {
    }

    public ClusterDetails(Ambari ambari) {
        this.ambari = ambari;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Ambari getAmbari() {
        return ambari;
    }

    public void setAmbari(Ambari ambari) {
        this.ambari = ambari;
    }

    public ClusterState getState() {
        return state;
    }

    public void setState(ClusterState state) {
        this.state = state;
    }

    public List<Alarm> getAlarms() {
        return alarms;
    }

    public void addAlarms(List<Alarm> alarms) {
        this.alarms.addAll(alarms);
    }

    public int getMinSize() {
        return minSize;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getCoolDown() {
        return coolDown;
    }

    public void setCoolDown(int coolDown) {
        this.coolDown = coolDown;
    }

    public boolean isAppMovementAllowed() {
        return appMovementAllowed;
    }

    public void setAppMovementAllowed(boolean appMovementAllowed) {
        this.appMovementAllowed = appMovementAllowed;
    }
}
