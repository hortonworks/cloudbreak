package com.sequenceiq.cloudbreak.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import com.google.common.base.Objects;
import com.sequenceiq.cloudbreak.util.CustomRevisionListener;

@Entity
@Table(name = "revision_info")
@RevisionEntity(CustomRevisionListener.class)
public class Revision implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "revision_generator")
    @SequenceGenerator(name = "revision_generator", sequenceName = "revision_id_seq", allocationSize = 1)
    @RevisionNumber
    @Column(name = "rev")
    private int id;

    @RevisionTimestamp
    private long timestamp;

    @Transient
    public Date getRevisionDate() {
        return new Date(timestamp);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Revision revision = (Revision) o;
        return id == revision.id &&
                timestamp == revision.timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, timestamp);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Revision{");
        sb.append("id=").append(id);
        sb.append(", timestamp=").append(timestamp);
        sb.append('}');
        return sb.toString();
    }
}
