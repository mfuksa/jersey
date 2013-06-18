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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glassfish.jersey.server.internal.monitoring.statistics.ExecutionStatistics;
import org.glassfish.jersey.server.internal.monitoring.statistics.ResourceMethodStatistics;
import org.glassfish.jersey.server.internal.monitoring.statistics.ResourceStatistics;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class ResourceMxBeanImpl implements ResourceMXBean {
    private final String path;
    private ExecutionStatisticsMxBeanImpl resourceExecutionStatisticsMxBean;
    private ExecutionStatisticsMxBeanImpl requestExecutionStatisticsMxBean;
    private final Map<ResourceMethod, ResourceMethodMxBeanImpl> resourceMethods = Maps.newHashMap();
    private final List<ResourceMethodMxBeanImpl> exposedResourceMethods = Lists.newArrayList();
    private final Map<Resource, Map<ResourceMethod, ResourceMethodMxBeanImpl>> childResourceMethods = Maps.newHashMap();


    public ResourceMxBeanImpl(ResourceStatistics resourceStatistics, String path) {
        this.path = path;
        this.resourceExecutionStatisticsMxBean = new ExecutionStatisticsMxBeanImpl(ExecutionStatistics.epmtyStatistics());
        this.requestExecutionStatisticsMxBean = new ExecutionStatisticsMxBeanImpl(ExecutionStatistics.epmtyStatistics());

        for (Map.Entry<ResourceMethod, ResourceMethodStatistics> entry
                : resourceStatistics.getResourceMethods().entrySet()) {
            final ResourceMethod method = entry.getKey();
            final ResourceMethodMxBeanImpl resourceMxBean = new ResourceMethodMxBeanImpl(entry.getValue(), null);
            resourceMethods.put(method, resourceMxBean);
            exposedResourceMethods.add(resourceMxBean);
        }

        for (Map.Entry<Resource, ResourceStatistics> childEntry : resourceStatistics.getChildResources().entrySet()) {
            final Resource childResource = childEntry.getKey();
            final HashMap<ResourceMethod, ResourceMethodMxBeanImpl> childMap = new HashMap<ResourceMethod,
                    ResourceMethodMxBeanImpl>();
            childResourceMethods.put(childResource, childMap);
            for (Map.Entry<ResourceMethod, ResourceMethodStatistics> methodEntry
                    : childEntry.getValue().getResourceMethods().entrySet()) {
                final ResourceMethodMxBeanImpl subResourceMethodMXBean
                        = new ResourceMethodMxBeanImpl(methodEntry.getValue(), childResource.getPath());
                childMap.put(methodEntry.getKey(), subResourceMethodMXBean);
                exposedResourceMethods.add(subResourceMethodMXBean);
            }
        }
    }

    public void setResourceStatistics(ResourceStatistics resourceStatistics) {
        this.resourceExecutionStatisticsMxBean.setExecutionStatistics(resourceStatistics.getResourceExecutionStatistics());
        this.requestExecutionStatisticsMxBean.setExecutionStatistics(resourceStatistics.getRequestExecutionStatistics());
        for (Map.Entry<ResourceMethod, ResourceMethodStatistics> methodEntry
                : resourceStatistics.getResourceMethods().entrySet()) {
            this.resourceMethods.get(methodEntry.getKey()).setResourceMethodStatistics(methodEntry.getValue());
        }

        for (Map.Entry<Resource, ResourceStatistics> childEntry : resourceStatistics.getChildResources().entrySet()) {
            for (Map.Entry<ResourceMethod, ResourceMethodStatistics> subMethodEntry
                    : childEntry.getValue().getResourceMethods().entrySet()) {
                this.childResourceMethods.get(childEntry.getKey()).get(subMethodEntry.getKey())
                        .setResourceMethodStatistics(subMethodEntry.getValue());
            }

        }

    }

    public List<ResourceMethodMxBeanImpl> getResourceMethods() {
        return exposedResourceMethods;
    }

    @Override
    public ExecutionStatisticsMxBean getResourceExecutionStatistics() {
        return resourceExecutionStatisticsMxBean;
    }


    @Override
    public ExecutionStatisticsMxBean getRequestExecutionStatistics() {
        return requestExecutionStatisticsMxBean;
    }

    @Override
    public String getPath() {
        return this.path;
    }
}
