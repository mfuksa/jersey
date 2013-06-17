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
public class AvgRequestCount {



    static class Builder {
        private final int interval;
        private final int unit;
        private final int unitsPerInterval;

        private int requestsPerSecond;
        private final Queue<Integer> unitQueue;
        private long lastUnitEnd;
        private int lastUnitCount;
        private int totalCount;
        private int intervalWithRoundError;
        private int size;

        public Builder(int interval) {
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
            this.unitQueue = new LinkedList<Integer>();
            lastUnitEnd = System.currentTimeMillis() + unit;
        }

        public void addRequest(long requestTime) {
            final long now = System.currentTimeMillis();
            while (lastUnitEnd < now) {
                unitQueue.add(lastUnitCount);
                totalCount += lastUnitCount;
                if (size >= unitsPerInterval) {
                    totalCount -= unitQueue.remove();
                    // intervalWithRoundError is used instead of size * unit for performance reasons
                    requestsPerSecond = totalCount / intervalWithRoundError;
                } else {
                    size++;
                    requestsPerSecond = totalCount / (size * unit);
                }
                lastUnitCount = 0;
                lastUnitEnd += unit;
            }

            lastUnitCount++;
        }

        public int getInterval() {
            return interval;
        }
    }

    private final int interval;
    private final int requestsPerSecond;

    private AvgRequestCount(int interval, int requestsPerSecond) {
        this.interval = interval;
        this.requestsPerSecond = requestsPerSecond;
    }


    public int getInterval() {
        return interval;
    }

    public int getRequestsPerSecond() {
        return requestsPerSecond;
    }
}
