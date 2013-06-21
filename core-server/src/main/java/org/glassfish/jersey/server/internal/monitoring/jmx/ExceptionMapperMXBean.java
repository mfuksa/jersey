package org.glassfish.jersey.server.internal.monitoring.jmx;

import java.util.Map;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public interface ExceptionMapperMXBean {
    public Map<String, Long> getExceptionMapperCount();
    public long getSuccessfulMappings();
    public long getUnsuccessfulMappings();
    public long getTotalMappings();

}
