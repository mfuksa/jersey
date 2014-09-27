package org.glassfish.jersey.examples.monitoring;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.server.monitoring.MonitoringStatistics;

/**
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
@Path("monitoring")
public class MonitoringResource {
    @Inject
    private Provider<MonitoringStatistics> stats;

    @GET
    public String get() {
        return "Total exception mappings: "
                + stats.get().getExceptionMapperStatistics().getTotalMappings();
    }
}
