/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.atlarge.opendc.model.odc.platform

import com.atlarge.opendc.model.odc.integration.jpa.schema.ExperimentState
import com.atlarge.opendc.model.odc.integration.jpa.transaction
import com.atlarge.opendc.simulator.platform.Experiment
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import com.atlarge.opendc.model.odc.integration.jpa.schema.Experiment as InternalExperiment

/**
 * A manager for [Experiment]s received from a JPA database.
 *
 * @property factory The JPA entity manager factory to create [EntityManager]s to retrieve entities from the database
 * 					 from.
 * @property collectMachineStates Flag to indicate machine states will be collected.
 * @property collectTaskStates Flag to indicate task states will be collected.
 * @property collectStageMeasurements Flag to indicate stage measurements will be collected.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class JpaExperimentManager(private val factory: EntityManagerFactory,
                           private val collectMachineStates: Boolean = true,
                           private val collectTaskStates: Boolean = true,
                           private val collectStageMeasurements: Boolean = false) : AutoCloseable {
    /**
     * The entity manager for this experiment.
     */
    private var manager: EntityManager = factory.createEntityManager()

    /**
     * The amount of experiments in the queue. This property is not guaranteed to run in constant time.
     */
    val size: Int
        get() {
            return manager.createQuery("SELECT COUNT(e.id) FROM experiments e WHERE e.state = :s",
                java.lang.Long::class.java)
                .setParameter("s", ExperimentState.QUEUED)
                .singleResult.toInt()
        }

    /**
     * Poll an [Experiment] from the database and claim it.
     *
     * @return The experiment that has been polled from the database or `null` if there are no experiments in the
     * 		   queue.
     */
    fun poll(): JpaExperiment? {
        var result: JpaExperiment? = null
        manager.transaction {
            var experiment: InternalExperiment? = null
            val results = manager.createQuery("SELECT e FROM experiments e WHERE e.state = :s",
                InternalExperiment::class.java)
                .setParameter("s", ExperimentState.QUEUED)
                .setMaxResults(1)
                .resultList


            if (!results.isEmpty()) {
                experiment = results.first()
                experiment!!.state = ExperimentState.CLAIMED
            }
            result = experiment?.let { JpaExperiment(manager, it, collectMachineStates, collectTaskStates, collectStageMeasurements) }
        }
        manager = factory.createEntityManager()
        return result
    }

    /**
     * Close this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * `try`-with-resources statement.*
     *
     * @throws Exception if this resource cannot be closed
     */
    override fun close() = manager.close()
}
