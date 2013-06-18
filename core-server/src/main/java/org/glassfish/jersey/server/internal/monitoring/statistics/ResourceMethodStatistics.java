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

import org.glassfish.jersey.server.model.ResourceMethod;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class ResourceMethodStatistics {
    private final ExecutionStatistics resourceMethodExecutionStatistics;
    private final ExecutionStatistics requestExecutionStatistics;
    private final ResourceMethod resourceMethod;

    public static class Builder {
        private final ExecutionStatistics.Builder resourceMethodExecutionStatisticsBuilder;
        private final ExecutionStatistics.Builder requestExecutionStatisticsBuilder;
        private final ResourceMethod resourceMethod;


        public Builder(ResourceMethod resourceMethod) {
            this.resourceMethod = resourceMethod;
            this.resourceMethodExecutionStatisticsBuilder = new ExecutionStatistics.Builder();
            this.requestExecutionStatisticsBuilder = new ExecutionStatistics.Builder();
        }

        public ResourceMethodStatistics build() {
            return new ResourceMethodStatistics(resourceMethod, resourceMethodExecutionStatisticsBuilder.build(),
                    requestExecutionStatisticsBuilder.build());
        }

        public ExecutionStatistics.Builder getResourceMethodExecutionStatisticsBuilder() {
            return resourceMethodExecutionStatisticsBuilder;
        }

        public void addResourceMethodExecution(long methodExecutionTime, long methodStartTime,
                                               long requestExecutionTime, long requestStartTime) {
            resourceMethodExecutionStatisticsBuilder.addExecution(methodExecutionTime, methodStartTime);
            requestExecutionStatisticsBuilder.addExecution(requestExecutionTime, requestStartTime);
        }
    }

    public ResourceMethodStatistics(ResourceMethod resourceMethod, ExecutionStatistics resourceMethodExecutionStatistics,
                                    ExecutionStatistics requestExecutionStatistics) {
        this.resourceMethodExecutionStatistics = resourceMethodExecutionStatistics;
        this.resourceMethod = resourceMethod;
        this.requestExecutionStatistics = requestExecutionStatistics;
    }

    public ExecutionStatistics getResourceMethodExecutionStatistics() {
        return resourceMethodExecutionStatistics;
    }

    public ExecutionStatistics getRequestExecutionStatistics() {
        return requestExecutionStatistics;
    }

    public ResourceMethod getResourceMethod() {
        return resourceMethod;
    }
}
