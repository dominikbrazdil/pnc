/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The audited record of a build configuration. Each change to the build configuration table is recorded in the audit table.
 * This class provides access to a specific version of a build configuration.
 *
 */
@Entity
@Table(name = "buildconfiguration_aud")
public class BuildConfigurationAudited implements GenericEntity<IdRev> {

    private static final long serialVersionUID = 0L;

    /**
     * The id of the build configuration this record is associated with
     */
    @Column(insertable = false, updatable = false)
    private Integer id;

    /**
     * The table revision which identifies version of the build config
     */
    @Column(insertable = false, updatable = false)
    private Integer rev;

    @EmbeddedId
    private IdRev idRev;

    @NotNull
    private String name;

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String buildScript;

    @NotNull
    @ManyToOne(optional = false)
    @ForeignKey(name = "fk_buildconfiguration_aud_repositoryconfiguration")
    @Index(name="idx_buildconfiguration_aud_repositoryconfiguration")
    @Getter
    @Setter
    private RepositoryConfiguration repositoryConfiguration;

    private String scmRevision;

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String description;

    @NotNull
    @ManyToOne
    @ForeignKey(name = "fk_buildconfiguration_aud_project")
    @Index(name="idx_buildconfiguration_aud_project")
    private Project project;

    @NotNull
    @ManyToOne
    @ForeignKey(name = "fk_buildconfiguration_aud_buildenvironment")
    @Index(name="idx_buildconfiguration_aud_buildenvironment")
    private BuildEnvironment buildEnvironment;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "buildConfigurationAudited")
    private Set<BuildRecord> buildRecords;

    /**
     * Instantiates a new project build configuration.
     */
    public BuildConfigurationAudited() {
    }

    public IdRev getIdRev() {
        return idRev;
    }

    public void setIdRev(IdRev idRev) {
        this.idRev = idRev;
    }

    /**
     * @return the id
     */
    @Override
    public IdRev getId() {
        return idRev;
    }

    @Override
    public void setId(IdRev idRev) {
        throw new UnsupportedOperationException("Not supported in audited entity");
    }

    /**
     * @param id the id to set
     */
    public void setBuildRecordId(Integer id) {
        this.id = id;
    }

    /**
     * @return the revision number generated by hibernate envers.
     */
    public Integer getRev() {
        return rev;
    }

    /**
     * @param rev the revision number of this entity
     */
    public void setRev(Integer rev) {
        this.rev = rev;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the buildScript
     */
    public String getBuildScript() {
        return buildScript;
    }

    /**
     * @param buildScript the buildScript to set
     */
    public void setBuildScript(String buildScript) {
        this.buildScript = buildScript;
    }

    /**
     * @return the scmRevision
     */
    public String getScmRevision() {
        return scmRevision;
    }

    /**
     * @param scmRevision the scmRevision to set
     */
    public void setScmRevision(String scmRevision) {
        this.scmRevision = scmRevision;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the project
     */
    public Project getProject() {
        return project;
    }

    /**
     * @param project the project to set
     */
    public void setProject(Project project) {
        this.project = project;
    }

    public BuildEnvironment getBuildEnvironment() {
        return buildEnvironment;
    }

    public void setBuildEnvironment(BuildEnvironment buildEnvironment) {
        this.buildEnvironment = buildEnvironment;
    }

    public Set<BuildRecord> getBuildRecords() {
        return buildRecords;
    }

    public void setBuildRecords(Set<BuildRecord> buildRecords) {
        this.buildRecords = buildRecords;
    }

    @Getter
    @Setter
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "build_configuration_parameters_aud", joinColumns = {
            @JoinColumn(name = "buildconfiguration_id", referencedColumnName = "id"),
            @JoinColumn(name = "rev", referencedColumnName = "rev")
    })
    @MapKeyColumn(length = 50, name = "key", nullable = false)
    @Column(name = "value", nullable = false, length = 8192)
    private Map<String, String> genericParameters = new HashMap<>();

    @Override
    public String toString() {
        return "BuildConfigurationAudit [project=" + project + ", name=" + name + ", id=" + id + ", rev=" + rev + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        BuildConfigurationAudited that = (BuildConfigurationAudited) o;

        return (idRev != null ? idRev.equals(that.idRev) : false);
    }

    @Override
    public int hashCode() {
        return idRev != null ? idRev.hashCode() : 0;
    }

    public static class Builder {
        private BuildConfiguration buildConfiguration;
        private Integer id;
        private Integer rev;
        private RepositoryConfiguration repositoryConfiguration;

        public static Builder newBuilder() {
            return new Builder();
        }

        public BuildConfigurationAudited build() {
            BuildConfigurationAudited configurationAudited = new BuildConfigurationAudited();
            configurationAudited.setBuildRecords(buildConfiguration.getBuildRecords());
            configurationAudited.setBuildScript(buildConfiguration.getBuildScript());
            configurationAudited.setDescription(buildConfiguration.getDescription());
            configurationAudited.setBuildEnvironment(buildConfiguration.getBuildEnvironment());
            configurationAudited.setName(buildConfiguration.getName());
            configurationAudited.setDescription(buildConfiguration.getDescription());
            configurationAudited.setScmRevision(buildConfiguration.getScmRevision());
            configurationAudited.setRepositoryConfiguration(repositoryConfiguration);
            configurationAudited.setRev(rev);
            configurationAudited.setIdRev(new IdRev(id, rev));
            return configurationAudited;
        }

        public Builder buildConfiguration(BuildConfiguration buildConfiguration) {
            this.buildConfiguration = buildConfiguration;
            return this;
        }

        public Builder rev(Integer rev) {
            this.rev = rev;
            return this;
        }

        public Builder buildRecord(Integer id) {
            this.id = id;
            return this;
        }

        public Builder repositoryConfiguration(RepositoryConfiguration repositoryConfiguration) {
            this.repositoryConfiguration = repositoryConfiguration;
            return this;
        }
    }
}
