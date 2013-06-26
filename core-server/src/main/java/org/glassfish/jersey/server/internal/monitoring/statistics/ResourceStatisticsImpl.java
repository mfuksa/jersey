package org.glassfish.jersey.server.internal.monitoring.statistics;

import java.util.Collections;
import java.util.Map;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ExecutionStatistics;
import org.glassfish.jersey.server.monitoring.ResourceMethodStatistics;
import org.glassfish.jersey.server.monitoring.ResourceStatistics;

import com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ResourceStatisticsImpl implements ResourceStatistics {


    public static class Builder {
        private final ExecutionStatisticsImpl.Builder resourceExecutionStatisticsBuilder;
        private final ExecutionStatisticsImpl.Builder requestExecutionStatisticsBuilder;
        private final Map<ResourceMethod, ResourceMethodStatisticsImpl.Builder> methodsBuilders = Maps.newHashMap();

        public Builder(Resource resource) {
            for (ResourceMethod method : resource.getResourceMethods()) {
                methodsBuilders.put(method, new ResourceMethodStatisticsImpl.Builder(method));
            }
            this.resourceExecutionStatisticsBuilder = new ExecutionStatisticsImpl.Builder();
            this.requestExecutionStatisticsBuilder = new ExecutionStatisticsImpl.Builder();
        }

        public ResourceStatisticsImpl build() {
            final Map<ResourceMethod, ResourceMethodStatistics> resourceMethods = Maps.newHashMap();
            for (Map.Entry<ResourceMethod, ResourceMethodStatisticsImpl.Builder> methodEntry : methodsBuilders.entrySet()) {
                resourceMethods.put(methodEntry.getKey(), methodEntry.getValue().build());
            }

            return new ResourceStatisticsImpl(
                    Collections.unmodifiableMap(resourceMethods),
                    resourceExecutionStatisticsBuilder.build(),
                    requestExecutionStatisticsBuilder.build());
        }

        public void addResourceExecution(ResourceMethod resourceMethod, long methodStartTime, long methodDuration,
                                         long requestStartTime, long requestDuration) {
            resourceExecutionStatisticsBuilder.addExecution(methodStartTime, methodDuration);
            requestExecutionStatisticsBuilder.addExecution(requestStartTime, requestDuration);

            final ResourceMethodStatisticsImpl.Builder methodBuilder = methodsBuilders.get(resourceMethod);
            methodBuilder.addResourceMethodExecution(methodDuration, methodStartTime, requestDuration, requestStartTime);
        }

    }

    private final Map<ResourceMethod, ResourceMethodStatistics> resourceMethods;
    private final ExecutionStatistics resourceExecutionStatistics;
    private final ExecutionStatistics requestExecutionStatistics;


    public ResourceStatisticsImpl(Map<ResourceMethod, ResourceMethodStatistics> resourceMethods,
                                  ExecutionStatistics resourceExecutionStatistics, ExecutionStatistics requestExecutionStatistics) {
        this.resourceMethods = resourceMethods;
        this.resourceExecutionStatistics = resourceExecutionStatistics;
        this.requestExecutionStatistics = requestExecutionStatistics;
    }


    public Map<ResourceMethod, ResourceMethodStatistics> getResourceMethods() {
        return resourceMethods;
    }

    @Override
    public ExecutionStatistics getResourceExecutionStatistics() {
        return resourceExecutionStatistics;
    }

    @Override
    public ExecutionStatistics getRequestExecutionStatistics() {
        return requestExecutionStatistics;
    }

    @Override
    public Map<ResourceMethod, ResourceMethodStatistics> getResourceMethodStatistics() {
        return resourceMethods;
    }
}
