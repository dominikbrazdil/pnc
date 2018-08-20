/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014-2018 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.datastore.repositories;

import org.jboss.pnc.datastore.repositories.internal.AbstractRepository;
import org.jboss.pnc.datastore.repositories.internal.BuildConfigSetRecordSpringRepository;
import org.jboss.pnc.model.BuildConfigSetRecord;
import org.jboss.pnc.model.BuildRecord;
import org.jboss.pnc.spi.datastore.predicates.BuildConfigSetRecordPredicates;
import org.jboss.pnc.spi.datastore.repositories.BuildConfigSetRecordRepository;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

@Stateless
public class BuildConfigSetRecordRepositoryImpl extends AbstractRepository<BuildConfigSetRecord, Integer> implements
        BuildConfigSetRecordRepository {

    EntityManager manager;
    /**
     * @deprecated Created for CDI.
     */
    @Deprecated
    public BuildConfigSetRecordRepositoryImpl() {
        super(null, null);
    }

    @Inject
    public BuildConfigSetRecordRepositoryImpl(BuildConfigSetRecordSpringRepository buildConfigSetRecordSpringRepository, EntityManager manager) {
        super(buildConfigSetRecordSpringRepository, buildConfigSetRecordSpringRepository);
        this.manager = manager;
    }

    @Override
    public List<BuildConfigSetRecord> findTemporaryBuildConfigSetRecordsOlderThan(Date date) {
        return queryWithPredicates(
                BuildConfigSetRecordPredicates.temporaryBuild(),
                BuildConfigSetRecordPredicates.buildFinishedBefore(date)
        );
    }

    @Override
    public BuildConfigSetRecord getNewestRecordForBuildConfigurationSet(Integer buildConfigSetId) {
        return manager.createQuery(
                "select bcsr from BuildConfigSetRecord bcsr "
                        + "where bcsr.id = "
                        +   "(select max(bcsr1.id) from BuildConfigSetRecord bcsr1 "
                        +   "where bcsr1.buildConfigurationSet.id = :buildConfigSetId)", BuildConfigSetRecord.class)
                .setParameter("buildConfigSetId",buildConfigSetId).getSingleResult();
    }
}
