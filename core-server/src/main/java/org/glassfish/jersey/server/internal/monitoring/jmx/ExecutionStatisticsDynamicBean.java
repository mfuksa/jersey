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

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.server.internal.monitoring.statistics.ExecutionStatistics;
import org.glassfish.jersey.server.internal.monitoring.statistics.IntervalStatistics;

import com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ExecutionStatisticsDynamicBean implements DynamicMBean, Registrable {
    private ExecutionStatistics executionStatistics;
    private final String beanName;

    private final MBeanInfo mBeanInfo;


    private MBeanInfo getStatisticsMBeanInfo(final ExecutionStatistics executionStatistics) {
        final Map<Integer, IntervalStatistics> statsMap = executionStatistics.getIntervalStatistics();
        final Map<String, Value<Object>> attributeValues = Maps.newHashMap();
        MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[statsMap.size()];
        int i = 0;
        for (final IntervalStatistics statistics : statsMap.values()) {
            final long interval = statistics.getInterval();
            final String prefix = interval / 1000 + "s";
            final String name = prefix + "MinimumTime";
            attrs[i++] = new MBeanAttributeInfo(name, "long", "Minimum request processing time in last "
                    + prefix + ".", true, false, false);
            attributeValues.put(name, new Value<Object>() {
                @Override
                public Object get() {
                    return statistics.getMinimumExecutionTime();
                }
            });

        }

        return new MBeanInfo(this.getClass().getName(), "Execution statistics", attrs, null, null, null);
    }

    public ExecutionStatisticsDynamicBean(ExecutionStatistics executionStatistics, String beanName) {
        this.executionStatistics = executionStatistics;
        this.beanName = beanName;
        this.mBeanInfo = getMBeanInfo();

    }

    public void setExecutionStatistics(ExecutionStatistics executionStatistics) {
        this.executionStatistics = executionStatistics;
    }




    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        return null;
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        return null;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return null;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return null;
    }

    @Override
    public void register(MBeanExposer mBeanExposer, String parentName) {
        mBeanExposer.registerMBean(this, parentName + ",statistics=" + beanName);
    }
}
