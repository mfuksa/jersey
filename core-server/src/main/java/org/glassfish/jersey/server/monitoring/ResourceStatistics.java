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

package org.glassfish.jersey.server.monitoring;

import java.util.Map;

import org.glassfish.jersey.server.model.ResourceMethod;

/**
 * Monitoring statistics of the resource. The resource is a set of resource methods with specific characteristics
 * that is not defined by this interface. The resource can be set of resource methods that are accessible on
 * the same URI (in the case of {@link org.glassfish.jersey.server.monitoring.MonitoringStatistics#getUriStatistics()})
 * or the set of resource method defined in one {@link Class} (in case
 * of {@link org.glassfish.jersey.server.monitoring.MonitoringStatistics#getResourceClassStatistics()}).
 * <p/>
 * The principles of using statistics
 * is similar to principles of using {@link MonitoringStatistics}.
 *
 * @see MonitoringStatistics See monitoring statistics for more details.
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public interface ResourceStatistics {
    /**
     * Get {@link ExecutionStatistics execution statistics} that contain measurements of times only for
     * execution of resource methods. Durations average time, minimum time and maximum time
     * measure only time of execution of resource methods code. It does not involve other request processing
     * phases.
     *
     * @return Execution statistics of all resource method in this resource.
     */
    public ExecutionStatistics getResourceMethodExecutionStatistics();

    /**
     * Get {@link ExecutionStatistics execution statistics} that contain measurements of times for
     * whole processing from time when request comes into the Jersey application until the response
     * is written to the underlying IO container. The statistics involves only requests that were matched
     * to resource methods defined in {@link #getResourceMethodStatistics()}.
     *
     * @return Execution statistics of entire request processing for all resource method from this resource.
     */
    public ExecutionStatistics getRequestExecutionStatistics();

    /**
     * Return the statistics for resource method.
     *
     * @return
     */
    public Map<ResourceMethod, ResourceMethodStatistics> getResourceMethodStatistics();

    public ResourceStatistics snapshot();

}
