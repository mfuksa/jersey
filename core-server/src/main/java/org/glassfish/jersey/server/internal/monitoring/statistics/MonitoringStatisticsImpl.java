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

import java.util.Map;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;

import com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class MonitoringStatisticsImpl {
    private final ExecutionStatisticsImpl requestStatistics;
    private final Map<Resource, ResourceStatisticsImpl> rootResourceStatistics;
    private final ResponseStatistics responseStatistics;
    private final ApplicationStatistics applicationStatistics;
    private final ExceptionMapperStatistics exceptionMapperStatistics;

    public static class Builder {

        private ExecutionStatisticsImpl.Builder requestStatisticsBuilder;
        private final Map<Resource, ResourceStatisticsImpl.Builder> rootResourceStatistics = Maps.newHashMap();
        private final ResponseStatistics.Builder responseStatisticsBuilder;
        private ApplicationStatistics applicationStatistics;
        private ExceptionMapperStatistics.Builder exceptionMapperStatisticsBuilder;


        public Builder(ResourceModel resourceModel) {
            this.requestStatisticsBuilder = new ExecutionStatisticsImpl.Builder();
            this.responseStatisticsBuilder = new ResponseStatistics.Builder();
            for (Resource resource : resourceModel.getRootResources()) {
                final ResourceStatisticsImpl.Builder builder = new ResourceStatisticsImpl.Builder(resource);
                rootResourceStatistics.put(resource, builder);
            }
            this.exceptionMapperStatisticsBuilder = new ExceptionMapperStatistics.Builder();
        }



        public ExecutionStatisticsImpl.Builder getRequestStatisticsBuilder() {
            return requestStatisticsBuilder;
        }

        public Map<Resource, ResourceStatisticsImpl.Builder> getRootResourceStatistics() {
            return rootResourceStatistics;
        }

        public ExceptionMapperStatistics.Builder getExceptionMapperStatisticsBuilder() {
            return exceptionMapperStatisticsBuilder;
        }

        public void addResponseCode(int responseCode) {
            responseStatisticsBuilder.addResponseCode(responseCode);
        }


        public void setApplicationStatistics(ApplicationStatistics applicationStatistics) {
            this.applicationStatistics = applicationStatistics;
        }

        public MonitoringStatisticsImpl build() {
            final Map<Resource, ResourceStatisticsImpl> builtResourceStatistics = Maps.newHashMap();
            for (Map.Entry<Resource, ResourceStatisticsImpl.Builder> entry : rootResourceStatistics.entrySet()) {
                builtResourceStatistics.put(entry.getKey(), entry.getValue().build());
            }

            return new MonitoringStatisticsImpl(requestStatisticsBuilder.build(), builtResourceStatistics,
                    responseStatisticsBuilder.build(), applicationStatistics,
                    exceptionMapperStatisticsBuilder.build());
        }
    }

    private MonitoringStatisticsImpl(ExecutionStatisticsImpl requestStatistics,
                                     Map<Resource, ResourceStatisticsImpl> rootResourceStatistics,
                                     ResponseStatistics responseStatistics,
                                     ApplicationStatistics applicationStatistics, ExceptionMapperStatistics exceptionMapperStatistics) {
        this.requestStatistics = requestStatistics;
        this.rootResourceStatistics = rootResourceStatistics;
        this.responseStatistics = responseStatistics;
        this.applicationStatistics = applicationStatistics;
        this.exceptionMapperStatistics = exceptionMapperStatistics;
    }

    public ExecutionStatisticsImpl getRequestStatistics() {
        return requestStatistics;
    }

    public Map<Resource, ResourceStatisticsImpl> getRootResourceStatistics() {
        return rootResourceStatistics;
    }

    public ResponseStatistics getResponseStatistics() {
        return responseStatistics;
    }


    public ApplicationStatistics getApplicationStatistics() {
        return applicationStatistics;
    }

    public ExceptionMapperStatistics getExceptionMapperStatistics() {
        return exceptionMapperStatistics;
    }
}
