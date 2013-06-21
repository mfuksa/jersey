package org.glassfish.jersey.server.internal.monitoring.statistics;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.ext.ExceptionMapper;

import com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ExceptionMapperStatistics {
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




        public ExceptionMapperStatistics build() {
            return new ExceptionMapperStatistics(Collections.unmodifiableMap(exceptionMapperExecutionCount),
                    successfulMappings, unsuccessfulMappings, totalMappings);
        }
    }



    private final Map<Class<?>, Long> exceptionMapperExecutionCount;
    private final long successfulMappings;
    private final long unsuccessfulMappings;
    private final long totalMappings;


    public ExceptionMapperStatistics(Map<Class<?>, Long> exceptionMapperExecutionCount,
                                     long successfulMappings, long unsuccessfulMappings, long totalMappings) {
        this.exceptionMapperExecutionCount = exceptionMapperExecutionCount;
        this.successfulMappings = successfulMappings;
        this.unsuccessfulMappings = unsuccessfulMappings;
        this.totalMappings = totalMappings;
    }

    public Map<Class<?>, Long> getExceptionMapperExecutionCount() {
        return exceptionMapperExecutionCount;
    }


    public long getSuccessfulMappings() {
        return successfulMappings;
    }

    public long getUnsuccessfulMappings() {
        return unsuccessfulMappings;
    }

    public long getTotalMappings() {
        return totalMappings;
    }
}
