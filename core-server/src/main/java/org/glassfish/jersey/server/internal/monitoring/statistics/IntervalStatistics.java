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
        private static final int DEFAULT_UNITS_PER_INTERVAL = 100;
        private static final int MINIMUM_UNIT_SIZE = 1000;
        private final long interval;
        private final long unit;
        private final int unitsPerInterval;

        private final long startTime;


        private final Queue<Unit> unitQueue;
        private long totalCount;
        private final long intervalWithRoundError;

        private long lastUnitEnd;
        private long lastUnitCount;
        private long lastUnitMin = -1;
        private long lastUnitMax = -1;


        private static class Unit {
            private final long count;
            private final long minimumExecutionTime;
            private final long maximumExecutionTime;

            private Unit(long count, long minimumExecutionTime, long maximumExecutionTime) {
                this.count = count;
                this.minimumExecutionTime = minimumExecutionTime;
                this.maximumExecutionTime = maximumExecutionTime;
            }

            private static Unit EMPTY_UNIT = new Unit(0, -1, -1);
        }

        public Builder(long interval) {
            this(interval, System.currentTimeMillis());
        }

        Builder(long interval, long now) {
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
                int n = DEFAULT_UNITS_PER_INTERVAL;
                long u = interval / n;
                if (u < 1000) {
                    n = (int) interval / 1000;
                    u = interval / n;
                }
                this.unit = u;
                this.unitsPerInterval = n;
                intervalWithRoundError = unit * unitsPerInterval;
                this.unitQueue = new LinkedList<Unit>();

                lastUnitEnd = startTime + unit;
            }
        }

        public void addRequest(long requestTime, long executionTime) {
            closePreviousUnitIfNeeded(requestTime);

            lastUnitCount++;

            if (executionTime < lastUnitMin || lastUnitMin == -1) {
                lastUnitMin = executionTime;
            }

            if (executionTime > lastUnitMax || lastUnitMax == -1) {
                lastUnitMax = executionTime;
            }
        }

        private void closePreviousUnitIfNeeded(long requestTime) {
            if (interval != 0) {
                if ((requestTime - lastUnitEnd) > interval + unit) {
                    resetQueue(requestTime);
                }
                if (lastUnitEnd < requestTime) {
                    // close the old unit
                    add(new Unit(lastUnitCount, lastUnitMin, lastUnitMax));

                    totalCount += lastUnitCount;
                    lastUnitEnd += unit;
                    lastUnitCount = 0;
                    lastUnitMin = -1;
                    lastUnitMax = -1;

                    while (lastUnitEnd < requestTime) {
                        add(Unit.EMPTY_UNIT);
                        lastUnitEnd += unit;
                    }
                }
            }
        }

        private void add(Unit unit) {
            unitQueue.add(unit);

            // fill with empty until units
            if (unitQueue.size() > unitsPerInterval) {
                totalCount -= unitQueue.remove().count;
            }
        }

        private void resetQueue(long requestTime) {
            this.unitQueue.clear();
            lastUnitEnd = requestTime + unit;
            lastUnitCount = 0;
            lastUnitMin = -1;
            lastUnitMax = -1;

            // fill with empty unit to keep result consistent
            for (int i = 0; i < unitsPerInterval; i++) {
                unitQueue.add(Unit.EMPTY_UNIT);
            }
        }


        public IntervalStatistics build() {
            return build(System.currentTimeMillis());
        }


        public IntervalStatistics build(long currentTime) {
            if (interval == 0) {
                final long diff = currentTime - startTime;
                if (diff < MINIMUM_UNIT_SIZE) {
                    return new IntervalStatistics(interval, 0, 0, 0, 0);
                } else {
                    int requestsPerSecond = (int) (lastUnitCount / diff);
                    return new IntervalStatistics(interval, requestsPerSecond, lastUnitMin, lastUnitMax, lastUnitCount);
                }
            }

            closePreviousUnitIfNeeded(currentTime);
            long min = -1;
            long max = -1;
            double requestsPerSecond;

            for (final Unit u : this.unitQueue) {
                if ((u.minimumExecutionTime < min && u.minimumExecutionTime != -1) || min == -1) {
                    min = u.minimumExecutionTime;
                }
                if ((u.maximumExecutionTime > max && u.maximumExecutionTime != -1) || max == -1) {
                    max = u.maximumExecutionTime;
                }
            }

            final int size = unitQueue.size();
            if (size >= unitsPerInterval) {
                // intervalWithRoundError is used instead of size * unit for performance reasons
                requestsPerSecond = (double) (1000 * totalCount) / intervalWithRoundError;
            } else {
                requestsPerSecond = size == 0 ? 0d : (double) (1000 * totalCount) / (size * unit);
            }

            return new IntervalStatistics(interval, requestsPerSecond, min, max, totalCount);
        }

        public long getInterval() {
            return interval;
        }
    }

    private final long interval;
    private final double requestsPerSecond;

    private final long minimumExecutionTime;
    private final long maximumExecutionTime;

    private long totalCount;


    private IntervalStatistics(long interval, double requestsPerSecond, long minimumExecutionTime,
                               long maximumExecutionTime, long totalCount) {
        this.interval = interval;
        this.requestsPerSecond = requestsPerSecond;
        this.minimumExecutionTime = minimumExecutionTime;
        this.maximumExecutionTime = maximumExecutionTime;
        this.totalCount = totalCount;
    }


    public long getInterval() {
        return interval;
    }

    public double getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public long getMinimumExecutionTime() {
        return minimumExecutionTime;
    }

    public long getMaximumExecutionTime() {
        return maximumExecutionTime;
    }

    public long getTotalCount() {
        return totalCount;
    }
}
