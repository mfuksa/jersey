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

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.monitoring.ApplicationStatistics;
import org.glassfish.jersey.server.monitoring.ExceptionMapperStatistics;
import org.glassfish.jersey.server.monitoring.ExecutionStatistics;
import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.server.monitoring.ResourceStatistics;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class MonitoringStatisticsImpl implements MonitoringStatistics {

    public static class Builder {

        private ExecutionStatisticsImpl.Builder requestStatisticsBuilder;
        private final ResponseStatisticsImpl.Builder responseStatisticsBuilder;
        private ApplicationStatisticsImpl applicationStatisticsImpl;
        private ExceptionMapperStatisticsImpl.Builder exceptionMapperStatisticsBuilder;
        private SortedMap<String, ResourceStatisticsImpl.Builder> uriStatistics = Maps.newTreeMap();
        private SortedMap<Class<?>, ResourceStatisticsImpl.Builder> resourceClassStatistics
                = Maps.newTreeMap(new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });


        public Builder() {
            this.requestStatisticsBuilder = new ExecutionStatisticsImpl.Builder();
            this.responseStatisticsBuilder = new ResponseStatisticsImpl.Builder();
            this.exceptionMapperStatisticsBuilder = new ExceptionMapperStatisticsImpl.Builder();
        }

        public Builder(ResourceModel resourceModel) {
            this();
            for (Resource resource : resourceModel.getRootResources()) {
                processResource(resource, "");
                for (Resource child : resource.getChildResources()) {
                    processResource(child, "/" + resource.getPath());
                }
            }

        }

        private void processResource(Resource resource, String pathPrefix) {
            this.uriStatistics.put(pathPrefix + "/" + resource.getPath(), new ResourceStatisticsImpl.Builder(resource));
            for (ResourceMethod resourceMethod : resource.getResourceMethods()) {
                ResourceStatisticsImpl.Builder builder = getOrCreateResourceBuilder(resourceMethod);
                builder.addMethod(resourceMethod);
            }
        }

        private ResourceStatisticsImpl.Builder getOrCreateResourceBuilder(ResourceMethod resourceMethod) {
            final Class<?> clazz = resourceMethod.getInvocable().getHandler().getHandlerClass();
            ResourceStatisticsImpl.Builder builder = resourceClassStatistics.get(clazz);
            if (builder == null) {
                builder = new ResourceStatisticsImpl.Builder();
                resourceClassStatistics.put(clazz, builder);
            }
            return builder;
        }

        public ExecutionStatisticsImpl.Builder getRequestStatisticsBuilder() {
            return requestStatisticsBuilder;
        }

        public ExceptionMapperStatisticsImpl.Builder getExceptionMapperStatisticsBuilder() {
            return exceptionMapperStatisticsBuilder;
        }

        public void addExecution(String uri, ResourceMethod resourceMethod,
                                 long methodTime, long methodDuration,
                                 long requestTime, long requestDuration) {
            ResourceStatisticsImpl.Builder uriStatsBuilder = uriStatistics.get(uri);
            if (uriStatsBuilder == null) {
                uriStatsBuilder = new ResourceStatisticsImpl.Builder(resourceMethod.getParent());
                uriStatistics.put(uri, uriStatsBuilder);
            }
            uriStatsBuilder.addExecution(resourceMethod, methodTime, methodDuration,
                    requestTime, requestDuration);

            ResourceStatisticsImpl.Builder resourceClassBuilder = getOrCreateResourceBuilder(resourceMethod);
            resourceClassBuilder.addExecution(resourceMethod, methodTime, methodDuration,
                    requestTime, requestDuration);
        }

        public void addResponseCode(int responseCode) {
            responseStatisticsBuilder.addResponseCode(responseCode);
        }


        public void setApplicationStatisticsImpl(ApplicationStatisticsImpl applicationStatisticsImpl) {
            this.applicationStatisticsImpl = applicationStatisticsImpl;
        }

        public MonitoringStatisticsImpl build() {
            final Function<ResourceStatisticsImpl.Builder, ResourceStatistics> buildingFunction
                    = new Function<ResourceStatisticsImpl.Builder, ResourceStatistics>() {
                @Override
                public ResourceStatistics apply(ResourceStatisticsImpl.Builder builder) {
                    return builder.build();
                }
            };

            Map<String, ResourceStatistics> uriStats = Collections.unmodifiableMap(
                    Maps.transformValues(uriStatistics, buildingFunction));
            Map<Class<?>, ResourceStatistics> classStats = Collections.unmodifiableMap(
                    Maps.transformValues(this.resourceClassStatistics,
                            buildingFunction));

            return new MonitoringStatisticsImpl(
                    uriStats, classStats,
                    requestStatisticsBuilder.build(),
                    responseStatisticsBuilder.build(),
                    applicationStatisticsImpl,
                    exceptionMapperStatisticsBuilder.build());
        }
    }

    private final ExecutionStatistics requestStatistics;
    private final ResponseStatisticsImpl responseStatisticsImpl;
    private final ApplicationStatistics applicationStatistics;
    private final ExceptionMapperStatistics exceptionMapperStatistics;
    private final Map<String, ResourceStatistics> uriStatistics;
    private final Map<Class<?>, ResourceStatistics> resourceClassStatistics;


    public MonitoringStatisticsImpl(Map<String, ResourceStatistics> uriStatistics,
                                    Map<Class<?>, ResourceStatistics> resourceClassStatistics,
                                    ExecutionStatistics requestStatistics,
                                    ResponseStatisticsImpl responseStatistics,
                                    ApplicationStatistics applicationStatistics,
                                    ExceptionMapperStatistics exceptionMapperStatistics) {
        this.uriStatistics = uriStatistics;
        this.resourceClassStatistics = resourceClassStatistics;
        this.requestStatistics = requestStatistics;
        this.responseStatisticsImpl = responseStatistics;
        this.applicationStatistics = applicationStatistics;
        this.exceptionMapperStatistics = exceptionMapperStatistics;
    }


    @Override
    public ExecutionStatistics getRequestExecutionStatistics() {
        return requestStatistics;
    }


    @Override
    public ResponseStatisticsImpl getResponseStatistics() {
        return responseStatisticsImpl;
    }


    @Override
    public Map<String, ResourceStatistics> getUriStatistics() {
        return uriStatistics;
    }

    @Override
    public Map<Class<?>, ResourceStatistics> getResourceClassStatistics() {
        return resourceClassStatistics;
    }

    @Override
    public ApplicationStatistics getApplicationStatistics() {
        return applicationStatistics;
    }

    @Override
    public ExceptionMapperStatistics getExceptionMapperStatistics() {
        return exceptionMapperStatistics;
    }
}
