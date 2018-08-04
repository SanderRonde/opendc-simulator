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

package com.atlarge.opendc.model.odc.platform.workload

/**
 * A bag of tasks which are submitted by a [User] to the cloud network.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Job {
    /**
     * A unique identifier of the job.
     */
    val id: Int

    /**
     * The owner of this job.
     */
    val owner: User

    /**
     * The tasks this job consists of.
     */
    val tasks: Set<Task>

    /**
     * A flag to indicate the job has finished.
     */
    val finished: Boolean
        get() = tasks.all { it.finished }
}

/**
 * Create a topological sorting of the tasks in a job.
 *
 * @return The list of tasks within the job topologically sorted.
 */
fun Job.toposort(): List<Task> {
    val res = mutableListOf<Task>()
    val visited = mutableSetOf<Task>()
    val adjacent = mutableMapOf<Task, MutableList<Task>>()

    for (task in tasks) {
        for (dependency in task.dependencies) {
            adjacent.getOrPut(dependency) { mutableListOf() }.add(task)
        }
    }

    fun visit(task: Task) {
        visited.add(task)

        adjacent[task] ?: emptyList<Task>()
            .asSequence()
            .filter { it !in visited }
            .forEach { visit(it) }

        res.add(task)
    }

    tasks
        .asSequence()
        .filter { it !in visited }
        .forEach { visit(it) }
    return res
}
