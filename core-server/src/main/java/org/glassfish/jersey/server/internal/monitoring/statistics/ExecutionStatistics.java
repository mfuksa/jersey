/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.server.internal.monitoring.statistics;

import java.util.Date;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class ExecutionStatistics {
    private final long executionCount;
    private final long minimumExecutionTimeInMilliseconds;
    private final long maximumExecutionTimeInMilliseconds;
    private final long averageExecutionTimeInMilliseconds;
    private final long totalExecutionTimeInMilliseconds;
    private final Date lastStartTime;

    private ExecutionStatistics(long executionCount, long minimumExecutionTimeInMilliseconds,
                                long maximumExecutionTimeInMilliseconds, long averageExecutionTimeInMilliseconds,
                                long totalExecutionTimeInMilliseconds,
                                Date lastStartTime) {
        this.executionCount = executionCount;
        this.minimumExecutionTimeInMilliseconds = minimumExecutionTimeInMilliseconds;
        this.maximumExecutionTimeInMilliseconds = maximumExecutionTimeInMilliseconds;
        this.averageExecutionTimeInMilliseconds = averageExecutionTimeInMilliseconds;
        this.lastStartTime = lastStartTime;
        this.totalExecutionTimeInMilliseconds = totalExecutionTimeInMilliseconds;
    }

    public long getAverageExecutionTimeInMilliseconds() {
        return averageExecutionTimeInMilliseconds;
    }

    public long getExecutionCount() {
        return executionCount;
    }

    public long getMaximumExecutionTimeInMilliseconds() {
        return maximumExecutionTimeInMilliseconds;
    }

    public long getMinimumExecutionTimeInMilliseconds() {
        return minimumExecutionTimeInMilliseconds;
    }

    public Date getLastStartTime() {
        return lastStartTime;
    }

    public long getTotalExecutionTimeInMilliseconds() {
        return totalExecutionTimeInMilliseconds;
    }

    public static class Builder {
        private long executionCount;
        private long minimumExecutionTimeInMilliseconds;
        private long maximumExecutionTimeInMilliseconds;
        private long totalExecutionTimeInMilliseconds;
        private Date lastStartTime;

        public Builder(ExecutionStatistics executionStatistics) {
            this.executionCount = executionStatistics.getExecutionCount();
            this.minimumExecutionTimeInMilliseconds = executionStatistics.getMinimumExecutionTimeInMilliseconds();
            this.maximumExecutionTimeInMilliseconds = executionStatistics.getMaximumExecutionTimeInMilliseconds();
            this.lastStartTime = executionStatistics.getLastStartTime();
            this.totalExecutionTimeInMilliseconds = executionStatistics.getTotalExecutionTimeInMilliseconds();
        }

        public Builder() {

        }

        public void addExecution(long executionTime, Date startTime) {
            this.totalExecutionTimeInMilliseconds += executionTime;
            if (executionCount > 0) {
                this.minimumExecutionTimeInMilliseconds = executionTime >= minimumExecutionTimeInMilliseconds
                        ? executionTime : minimumExecutionTimeInMilliseconds;
                this.maximumExecutionTimeInMilliseconds = executionTime >= maximumExecutionTimeInMilliseconds
                        ? executionTime : maximumExecutionTimeInMilliseconds;

                this.totalExecutionTimeInMilliseconds += executionTime;
            }
            this.executionCount++;
            this.lastStartTime = startTime;
        }

        public ExecutionStatistics build() {
            return new ExecutionStatistics(executionCount, minimumExecutionTimeInMilliseconds,
                    maximumExecutionTimeInMilliseconds, executionCount == 0 ? 0 : totalExecutionTimeInMilliseconds / executionCount,
                    totalExecutionTimeInMilliseconds, lastStartTime);
        }


    }

    public static ExecutionStatistics emtpyStatistics() {
        return new Builder().build();
    }
}
