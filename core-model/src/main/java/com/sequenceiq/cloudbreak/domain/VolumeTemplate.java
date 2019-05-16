package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;

@Entity
public class VolumeTemplate implements ProvisionEntity, WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "volume_template_generator")
    @SequenceGenerator(name = "volume_template_generator", sequenceName = "volume_template_id_seq", allocationSize = 1)
    private Long id;

    private Integer volumeCount;

    private Integer volumeSize;

    private String volumeType;

    @ManyToOne
    private Template template;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVolumeCount() {
        return volumeCount;
    }

    public void setVolumeCount(Integer volumeCount) {
        this.volumeCount = volumeCount;
    }

    public Integer getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(Integer volumeSize) {
        this.volumeSize = volumeSize;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    @Override
    public Workspace getWorkspace() {
        return template.getWorkspace();
    }

    @Override
    public String getName() {
        return id.toString();
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        template.setWorkspace(workspace);
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.STACK;
    }
}
