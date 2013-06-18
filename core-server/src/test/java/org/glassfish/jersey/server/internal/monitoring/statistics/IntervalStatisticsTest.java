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
    @Test
    public void test() {
        final long now = System.currentTimeMillis();
        IntervalStatistics.Builder builder = new IntervalStatistics.Builder(1000, now);
        builder.addRequest(now, 30);
        builder.addRequest(now+300, 100);
        builder.addRequest(now+600, 153);
        builder.addRequest(now+800, 15);
        builder.addRequest(now+999, 60);
        builder.addRequest(now+1000, 99);
        IntervalStatistics stat = builder.build();
        Assert.assertEquals(0, stat.getTotalCount());
        Assert.assertEquals(0, stat.getRequestsPerSecond());
        Assert.assertEquals(-1, stat.getMaximumExecutionTime());
        Assert.assertEquals(-1, stat.getMinimumExecutionTime());
        builder.addRequest(now+1001, 999);

        stat = builder.build();
        Assert.assertEquals(6, stat.getTotalCount());
        Assert.assertEquals(6, stat.getRequestsPerSecond());
        Assert.assertEquals(153, stat.getMaximumExecutionTime());
        Assert.assertEquals(15, stat.getMinimumExecutionTime());
    }

    @Test
    public void test10() {
        final long now = System.currentTimeMillis();
        IntervalStatistics.Builder builder = new IntervalStatistics.Builder(10000, now);
        builder.addRequest(now, 30);
        builder.addRequest(now+300, 100);
        builder.addRequest(now+600, 153);
        builder.addRequest(now+800, 15);
        builder.addRequest(now+999, 60);
        builder.addRequest(now+1000, 99);
        builder.addRequest(now+8001, 600);
        IntervalStatistics stat = builder.build();
        Assert.assertEquals(0, stat.getTotalCount());
        Assert.assertEquals(0, stat.getRequestsPerSecond());
        Assert.assertEquals(-1, stat.getMaximumExecutionTime());
        Assert.assertEquals(-1, stat.getMinimumExecutionTime());
        builder.addRequest(now+10380, 5175);

        stat = builder.build();
        Assert.assertEquals(7, stat.getTotalCount());
        Assert.assertEquals(7, stat.getRequestsPerSecond());
        Assert.assertEquals(153, stat.getMaximumExecutionTime());
        Assert.assertEquals(15, stat.getMinimumExecutionTime());
    }
}
