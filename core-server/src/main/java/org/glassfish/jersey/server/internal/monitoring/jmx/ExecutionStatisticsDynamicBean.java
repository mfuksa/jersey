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
import org.glassfish.jersey.server.internal.monitoring.statistics.ExecutionStatisticsImpl;
import org.glassfish.jersey.server.internal.monitoring.statistics.TimeWindowStatisticsImpl;
import org.glassfish.jersey.server.monitoring.TimeWindowStatistics;

import com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ExecutionStatisticsDynamicBean implements DynamicMBean, Registrable {
    private volatile ExecutionStatisticsImpl executionStatisticsImpl;
    private final String beanName;
    private final Map<String, Value<Object>> attributeValues = Maps.newHashMap();

    private final MBeanInfo mBeanInfo;


    private MBeanInfo initMBeanInfo(final ExecutionStatisticsImpl initialStatistics) {
        final Map<Long, TimeWindowStatistics> statsMap = initialStatistics.getTimeWindowStatistics();
        MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[statsMap.size() * 5];
        int i = 0;
        for (final TimeWindowStatistics stats : statsMap.values()) {
            final long interval = stats.getTimeWindow();
            final String prefix = convertIntervalToString((int) interval);

            String name = prefix + "_MinTime_ms";
            attrs[i++] = new MBeanAttributeInfo(name, "long", "Minimum request processing time in milliseconds in last "
                    + prefix + " seconds.", true, false, false);

            attributeValues.put(name, new Value<Object>() {
                @Override
                public Object get() {
                    return executionStatisticsImpl.getTimeWindowStatistics().get(interval).getMinimumDuration();
                }
            });

            name = prefix + "_MaxTime_ms";
            attrs[i++] = new MBeanAttributeInfo(name, "long", "Minimum request processing time  in milliseconds in last "
                    + prefix + " seconds.", true, false, false);

            attributeValues.put(name, new Value<Object>() {
                @Override
                public Object get() {
                    return executionStatisticsImpl.getTimeWindowStatistics().get(interval).getMaximumDuration();
                }
            });

            name = prefix + "_AverageTime_ms";
            attrs[i++] = new MBeanAttributeInfo(name, "long", "Average request processing time in milliseconds in last "
                    + prefix + " seconds.", true, false, false);

            attributeValues.put(name, new Value<Object>() {
                @Override
                public Object get() {
                    return executionStatisticsImpl.getTimeWindowStatistics().get(interval).getAverageDuration();
                }
            });


            name = prefix + "_RequestsPerSecond";
            attrs[i++] = new MBeanAttributeInfo(name, "double", "Average requests per second in last "
                    + prefix + " seconds.", true, false, false);

            attributeValues.put(name, new Value<Object>() {
                @Override
                public Object get() {
                    return executionStatisticsImpl.getTimeWindowStatistics().get(interval).getRequestsPerSecond();
                }
            });


            name = prefix + "_RequestCount";
            attrs[i++] = new MBeanAttributeInfo(name, "double", "Request count in last "
                    + prefix + " seconds.", true, false, false);

            attributeValues.put(name, new Value<Object>() {
                @Override
                public Object get() {
                    return executionStatisticsImpl.getTimeWindowStatistics().get(interval).getRequestCount();
                }
            });
        }

        return new MBeanInfo(this.getClass().getName(), "Execution statistics", attrs, null, null, null);
    }

    private String convertIntervalToString(int interval) {
        int hours = (int) interval / 3600000;
        interval = interval - hours * 3600000;
        int minutes = (int) interval / 60000;
        interval = interval - minutes * 60000;
        int seconds = (int) interval / 1000;
        StringBuffer sb = new StringBuffer();
        if (hours > 0) {
            sb.append(hours).append("h_");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m_");
        }
        if (seconds > 0) {
            sb.append(seconds).append("s_");
        }
        if (sb.length() == 0) {
            sb.append("total");
        } else {
            sb.setLength(sb.length() - 1);
        }


        return sb.toString();
    }

    public ExecutionStatisticsDynamicBean(ExecutionStatisticsImpl executionStatisticsImpl, String beanName) {
        this.executionStatisticsImpl = executionStatisticsImpl;
        this.beanName = beanName;
        this.mBeanInfo = initMBeanInfo(executionStatisticsImpl);

    }

    public void setExecutionStatisticsImpl(ExecutionStatisticsImpl executionStatisticsImpl) {
        this.executionStatisticsImpl = executionStatisticsImpl;
    }


    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        return attributeValues.get(attribute).get();
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        // TODO: M: implement
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
        return mBeanInfo;
    }

    @Override
    public void register(MBeanExposer mBeanExposer, String parentName) {
        mBeanExposer.registerMBean(this, parentName + ",statistics=" + beanName);
    }
}
