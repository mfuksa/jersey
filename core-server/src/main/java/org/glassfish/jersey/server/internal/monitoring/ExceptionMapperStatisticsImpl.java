package org.glassfish.jersey.server.internal.monitoring;

import java.util.Collections;
import java.util.Map;

import org.glassfish.jersey.server.monitoring.ExceptionMapperStatistics;

import com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ExceptionMapperStatisticsImpl implements ExceptionMapperStatistics {


    public static class Builder {
        private Map<Class<?>, Long> exceptionMapperExecutionCount = Maps.newHashMap();
        private long successfulMappings;
        private long unsuccessfulMappings;
        private long totalMappings;


        public void addMapping(boolean success, int count) {
            totalMappings++;
            if (success) {
                successfulMappings +=count;
            } else {
                unsuccessfulMappings  +=count;
            }
        }

        public void addExceptionMapperExecution(Class<?> mapper, int count) {
            Long cnt = exceptionMapperExecutionCount.get(mapper);
            cnt = cnt == null ? count : cnt + count;
            exceptionMapperExecutionCount.put(mapper, cnt);
        }




        public ExceptionMapperStatisticsImpl build() {
            return new ExceptionMapperStatisticsImpl(Collections.unmodifiableMap(exceptionMapperExecutionCount),
                    successfulMappings, unsuccessfulMappings, totalMappings);
        }
    }



    private final Map<Class<?>, Long> exceptionMapperExecutionCount;
    private final long successfulMappings;
    private final long unsuccessfulMappings;
    private final long totalMappings;


    public ExceptionMapperStatisticsImpl(Map<Class<?>, Long> exceptionMapperExecutionCount,
                                         long successfulMappings, long unsuccessfulMappings, long totalMappings) {
        this.exceptionMapperExecutionCount = exceptionMapperExecutionCount;
        this.successfulMappings = successfulMappings;
        this.unsuccessfulMappings = unsuccessfulMappings;
        this.totalMappings = totalMappings;
    }


    @Override
    public Map<Class<?>, Long> getExceptionMapperExecutions() {
        return exceptionMapperExecutionCount;
    }

    @Override
    public long getSuccessfulMappings() {
        return successfulMappings;
    }

    @Override
    public long getUnsuccessfulMappings() {
        return unsuccessfulMappings;
    }

    @Override
    public long getTotalMappings() {
        return totalMappings;
    }

    @Override
    public ExceptionMapperStatistics snapshot() {
        // snapshot functionality not yet implemented
        return this;
    }


}
