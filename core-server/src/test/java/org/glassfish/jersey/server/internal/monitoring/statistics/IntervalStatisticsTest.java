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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class IntervalStatisticsTest {

    private static final double DELTA = 0.0001;

    @Test
    public void test() {
        final long now = System.currentTimeMillis();
        IntervalStatistics.Builder builder = new IntervalStatistics.Builder(1000, now);
        builder.addRequest(now, 30);
        builder.addRequest(now + 300, 100);
        builder.addRequest(now + 600, 153);
        builder.addRequest(now + 800, 15);
        builder.addRequest(now + 999, 60);
        builder.addRequest(now + 1000, 99);

        check(builder, now + 1000, 0, -1, -1, 0);
        builder.addRequest(now + 1001, 999);

        check(builder, now + 1001, 6, 15, 153, 6);
    }

    @Test
    public void test10() {
        final long now = System.currentTimeMillis();
        IntervalStatistics.Builder builder = new IntervalStatistics.Builder(10000, now);
        builder.addRequest(now, 30);
        builder.addRequest(now + 300, 100);
        builder.addRequest(now + 600, 153);
        builder.addRequest(now + 800, 15);
        builder.addRequest(now + 999, 60);
        builder.addRequest(now + 1000, 99);
        builder.addRequest(now + 8001, 600);
        check(builder, now + 8001, 6, 15, 153, 0.75);
        builder.addRequest(now + 10380, 5175);
        check(builder, now + 10100, 7, 15, 600, 0.7);
    }

    @Test
    public void test3s() {
        final long now = 0;
        IntervalStatistics.Builder builder = new IntervalStatistics.Builder(3000, now);
        builder.addRequest(now, 99);
        builder.addRequest(now + 300, 98);
        builder.addRequest(now + 600, 1);
        builder.addRequest(now + 1000, 96);
        builder.addRequest(now + 1500, 95);
        builder.addRequest(now + 2500, 3);
        // ... above should be ignored ignored

        builder.addRequest(now + 3500, 90);
        builder.addRequest(now + 3900, 4);
        builder.addRequest(now + 3900, 80);
        builder.addRequest(now + 4200, 91);
        builder.addRequest(now + 4900, 15);
        builder.addRequest(now + 5300, 7);
        builder.addRequest(now + 5600, 50);

        // this should be again ignored
        builder.addRequest(now + 6100, 999999);

        check(builder, now + 6050, 7, 4, 91, 2.333333);

        builder.addRequest(now + 6300, 40);
        builder.addRequest(now + 7000, 30);
    }

    @Test
    public void testLongPause() {
        final long now = 0;
        IntervalStatistics.Builder builder = new IntervalStatistics.Builder(1000 * 60, now);
        builder.addRequest(now, 99);
        final long time = now + 1000 * 60 * 60 * 23;
        builder.addRequest(time, 98);
        builder.addRequest(time + 5, 5);
        check(builder, time + 20000, 2, 5, 98, 0.03333);
    }

    private void check(IntervalStatistics.Builder builder, long buildTime, int totalCount, int minimumExecTime,
                       int maximumExecTime, double requestsPerSecond) {
        IntervalStatistics stat = builder.build(buildTime);

        Assert.assertEquals(totalCount, stat.getTotalCount());
        Assert.assertEquals(minimumExecTime, stat.getMinimumDuration());
        Assert.assertEquals(maximumExecTime, stat.getMaximumDuration());
        Assert.assertEquals(requestsPerSecond, stat.getRequestsPerSecond(), DELTA);
    }

    @Test
    public void testGeneric() {
        IntervalStatistics.Builder builder = new IntervalStatistics.Builder(1000 * 10, 0);
        for (int i = 0; i < 100; i++) {
            final int requestTime = i * 10000;
            builder.addRequest(requestTime + 1, i);
            for (int j = 11; j < 100; j++) {
                try {
                    IntervalStatistics stat = builder.build(requestTime + j * 100);
                    System.out.println(stat.getRequestsPerSecond());
                    Assert.assertEquals(1, stat.getTotalCount());
                    Assert.assertEquals(i, stat.getMinimumDuration());
                    Assert.assertEquals(i, stat.getMaximumDuration());
                } catch (AssertionError e) {
                    System.out.println(i + " / " + j);
                    throw e;
                }
            }
        }
    }

}
