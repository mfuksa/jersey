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

package org.glassfish.jersey.server.internal.monitoring.jmx;

import java.util.Map;

import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ResourceMethodStatistics;
import org.glassfish.jersey.server.monitoring.ResourceStatistics;

import com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ResourceMxBeanImpl implements ResourceMXBean {
    private final String name;
    private ExecutionStatisticsDynamicBean resourceExecutionStatisticsBean;
    private ExecutionStatisticsDynamicBean requestExecutionStatisticsBean;
    private final Map<ResourceMethod, ResourceMethodMXBeanImpl> resourceMethods = Maps.newHashMap();
    private final String resourcePropertyName;
    private final boolean exposeMethodPath;
    private final MBeanExposer mBeanExposer;

    public ResourceMxBeanImpl(ResourceStatistics resourceStatistics, String name, boolean exposeMethodPath, MBeanExposer mBeanExposer,
                              String parentName) {
        this.name = name;
        this.exposeMethodPath = exposeMethodPath;
        this.mBeanExposer = mBeanExposer;
        this.resourcePropertyName = parentName + ",resource=" + name;
        mBeanExposer.registerMBean(this, resourcePropertyName);
        this.resourceExecutionStatisticsBean = new ExecutionStatisticsDynamicBean(
                resourceStatistics.getRequestExecutionStatistics(), mBeanExposer, resourcePropertyName, "ResourceStatistics");
        this.requestExecutionStatisticsBean = new ExecutionStatisticsDynamicBean(
                resourceStatistics.getResourceMethodExecutionStatistics(), mBeanExposer, resourcePropertyName, "RequestStatistics");

        updateResourceStatistics(resourceStatistics);
    }

    public void updateResourceStatistics(ResourceStatistics resourceStatistics) {
        this.resourceExecutionStatisticsBean.updateExecutionStatistics(resourceStatistics.getResourceMethodExecutionStatistics());
        this.requestExecutionStatisticsBean.updateExecutionStatistics(resourceStatistics.getRequestExecutionStatistics());

        for (Map.Entry<ResourceMethod, ResourceMethodStatistics> entry
                : resourceStatistics.getResourceMethodStatistics().entrySet()) {
            final ResourceMethodStatistics methodStats = entry.getValue();
            final ResourceMethod method = entry.getKey();
            ResourceMethodMXBeanImpl methodMXBean = this.resourceMethods.get(method);
            if (methodMXBean == null) {
                methodMXBean = new ResourceMethodMXBeanImpl(methodStats, exposeMethodPath, mBeanExposer, resourcePropertyName);
                resourceMethods.put(method, methodMXBean);
            }
            methodMXBean.updateResourceMethodStatistics(methodStats);
        }
    }


    @Override
    public String getName() {
        return this.name;
    }


}
