package org.glassfish.jersey.server.internal.monitoring.jmx;

import java.util.Map;

import org.glassfish.jersey.server.internal.monitoring.statistics.ExceptionMapperStatistics;

import com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ExceptionMapperMXBeanImpl implements ExceptionMapperMXBean, Registrable {
    private volatile ExceptionMapperStatistics mapperStatistics;
    private volatile Map<String, Long> mapperExcecutions = Maps.newHashMap();

    public void setNewStatistics(ExceptionMapperStatistics mapperStatistics) {
        this.mapperStatistics = mapperStatistics;
        for (Map.Entry<Class<?>, Long> entry : mapperStatistics.getExceptionMapperExecutionCount().entrySet()) {
            mapperExcecutions.put(entry.getKey().getName(), entry.getValue());
        }

    }

    @Override
    public Map<String, Long> getExceptionMapperCount() {
        return mapperExcecutions;
    }

    @Override
    public long getSuccessfulMappings() {
        return mapperStatistics.getSuccessfulMappings();
    }

    @Override
    public long getUnsuccessfulMappings() {
        return mapperStatistics.getUnsuccessfulMappings();
    }

    @Override
    public long getTotalMappings() {
        return mapperStatistics.getTotalMappings();
    }

    @Override
    public void register(MBeanExposer mBeanExposer, String parentName) {
        mBeanExposer.registerMBean(this, parentName + "type=ExceptionMapper");
    }

}
