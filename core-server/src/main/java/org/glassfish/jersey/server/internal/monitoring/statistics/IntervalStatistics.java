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

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class IntervalStatistics {


    static class Builder {
        private final int interval;
        private final int unit;
        private final int unitsPerInterval;

        private final long startTime;


        private final Queue<Unit> unitQueue;
        private int totalCount;
        private final int intervalWithRoundError;

        private long lastUnitEnd;
        private int lastUnitCount;
        private int lastUnitMin = -1;
        private int lastUnitMax = -1;


        private static class Unit {
            private final int count;
            private final int minumumExecutionTime;
            private final int maximumExecutionTime;

            private Unit(int count, int minumumExecutionTime, int maximumExecutionTime) {
                this.count = count;
                this.minumumExecutionTime = minumumExecutionTime;
                this.maximumExecutionTime = maximumExecutionTime;
            }

            private static Unit EMPTY_UNIT = new Unit(0, -1, -1);
        }

        public Builder(int interval) {
            this(interval, System.currentTimeMillis());
        }

        Builder(int interval, long now) {
            startTime = now;
            if (interval == 0) {
                // unlimited interval
                unit = 0;
                unitsPerInterval = 0;
                intervalWithRoundError = 0;
                unitQueue = null;
                this.interval = interval;
            } else {

                this.interval = interval;
                int n = 20;
                int u = interval / n;
                if (u < 1000) {
                    n = interval / 1000;
                    u = interval / n;
                }
                this.unit = u;
                this.unitsPerInterval = n;
                intervalWithRoundError = unit * unitsPerInterval;
                this.unitQueue = new LinkedList<Unit>();

                lastUnitEnd = startTime + unit;
            }
        }

        public void addRequest(long requestTime, int executionTime) {
            if (interval == 0) {

            } else {

                if ( (requestTime - lastUnitEnd) > interval + unit) {
                    resetQueue(requestTime);
                }
                if (lastUnitEnd < requestTime) {
                    // close the old unit
                    unitQueue.add(new Unit(lastUnitCount, lastUnitMin, lastUnitMax));

                    totalCount += lastUnitCount;
                    lastUnitEnd += unit;
                    lastUnitCount = 0;
                    lastUnitMin = -1;
                    lastUnitMax = -1;

                    while (lastUnitEnd < requestTime) {
                        // fill with empty until units
                        if (unitQueue.size()  >= unitsPerInterval) {
                            totalCount -= unitQueue.remove().count;
                        }
                        unitQueue.add(Unit.EMPTY_UNIT);
                        lastUnitEnd += unit;
                    }
                }
            }

            lastUnitCount++;

            if (executionTime < lastUnitMin || lastUnitMin == -1) {
                lastUnitMin = executionTime;
            }

            if (executionTime > lastUnitMax || lastUnitMax == -1) {
                lastUnitMax = executionTime;
            }
        }

        private void resetQueue(long requestTime) {
            this.unitQueue.clear();
            lastUnitEnd = requestTime + unit;
            lastUnitCount = 0;
            lastUnitMin = -1;
            lastUnitMax = -1;

        }


        public IntervalStatistics build() {
            if (interval == 0) {
                int requestsPerSecond = (int) (lastUnitCount / (startTime - System.currentTimeMillis()));
                return new IntervalStatistics(interval, requestsPerSecond, lastUnitMin, lastUnitMax, lastUnitCount);
            }

            int min = -1;
            int max = -1;
            int requestsPerSecond;

            for (Unit u : this.unitQueue) {
                if (u.minumumExecutionTime < min || min == -1) {
                    min = u.minumumExecutionTime;
                }
                if (u.maximumExecutionTime < max || max == -1) {
                    max = u.maximumExecutionTime;
                }
            }

            final int size = unitQueue.size();
            if (size >= unitsPerInterval) {
                // intervalWithRoundError is used instead of size * unit for performance reasons
                requestsPerSecond = 1000 * totalCount / intervalWithRoundError;
            } else {
                requestsPerSecond = size == 0 ? 0 : 1000 * totalCount / (size * unit);
            }

            return new IntervalStatistics(interval, requestsPerSecond, min, max, totalCount);
        }

        public int getInterval() {
            return interval;
        }
    }

    private final int interval;
    private final int requestsPerSecond;

    private final int minimumExecutionTime;
    private final int maximumExecutionTime;

    private int totalCount;


    private IntervalStatistics(int interval, int requestsPerSecond, int minimumExecutionTime,
                               int maximumExecutionTime, int totalCount) {
        this.interval = interval;
        this.requestsPerSecond = requestsPerSecond;
        this.minimumExecutionTime = minimumExecutionTime;
        this.maximumExecutionTime = maximumExecutionTime;
        this.totalCount = totalCount;
    }


    public int getInterval() {
        return interval;
    }

    public int getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public int getMinimumExecutionTime() {
        return minimumExecutionTime;
    }

    public int getMaximumExecutionTime() {
        return maximumExecutionTime;
    }

    public int getTotalCount() {
        return totalCount;
    }
}
