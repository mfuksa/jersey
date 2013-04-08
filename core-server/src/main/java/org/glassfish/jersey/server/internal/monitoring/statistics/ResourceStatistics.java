package org.glassfish.jersey.server.internal.monitoring.statistics;

import java.util.Date;
import java.util.Map;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class ResourceStatistics {
    private final Resource resource;
    private final Map<ResourceMethod, ResourceMethodStatistics> resourceMethods;
    private final Map<Resource, ResourceStatistics> childResources;
    private final ExecutionStatistics resourceExecutionStatistics;
    private final ExecutionStatistics requestExecutionStatistics;


    public static class Builder {
        private final Resource resource;
        private final ExecutionStatistics.Builder resourceExecutionStatisticsBuilder;
        private final ExecutionStatistics.Builder requestExecutionStatisticsBuilder;
        private final Map<ResourceMethod, ResourceMethodStatistics.Builder> methodsBuilders = Maps.newHashMap();
        private final Map<Resource, ResourceStatistics.Builder> childBuilders = Maps.newHashMap();

        public Builder(Resource resource) {
            this.resource = resource;
            for (ResourceMethod method : resource.getResourceMethods()) {
                methodsBuilders.put(method, new ResourceMethodStatistics.Builder(method));
            }
            for (Resource child : resource.getChildResources()) {
                childBuilders.put(child, new ResourceStatistics.Builder(child));
            }
            this.resourceExecutionStatisticsBuilder = new ExecutionStatistics.Builder();
            this.requestExecutionStatisticsBuilder = new ExecutionStatistics.Builder();
        }

        public ResourceStatistics build() {
            final Map<ResourceMethod, ResourceMethodStatistics> resourceMethods = Maps.newHashMap();
            final Map<Resource, ResourceStatistics> childResources = Maps.newHashMap();
            for (Map.Entry<ResourceMethod, ResourceMethodStatistics.Builder> methodEntry : methodsBuilders.entrySet()) {
                resourceMethods.put(methodEntry.getKey(), methodEntry.getValue().build());
            }

            for (Map.Entry<Resource, ResourceStatistics.Builder> resourceEntry : childBuilders.entrySet()) {
                childResources.put(resourceEntry.getKey(), resourceEntry.getValue().build());
            }
            return new ResourceStatistics(resource, resourceMethods, childResources, resourceExecutionStatisticsBuilder.build(),
                    requestExecutionStatisticsBuilder.build());
        }

        public void addResourceExecution(Resource childResource, ResourceMethod resourceMethod, long methodExecutionTime, Date methodStartTime,
                                         long requestExecutionTime, Date requestStartTime) {
            addResourceExecution(methodExecutionTime, methodStartTime, requestExecutionTime, requestStartTime);
            childBuilders.get(childResource).addResourceExecution(resourceMethod, methodExecutionTime, methodStartTime,
                    requestExecutionTime, requestStartTime);
        }

        public void addResourceExecution(ResourceMethod resourceMethod, long executionTime, Date startTime,
                                         long requestExecutionTime, Date requestStartTime) {
            addResourceExecution(executionTime, startTime, requestExecutionTime, requestStartTime);
            final ResourceMethodStatistics.Builder methodBuilder = methodsBuilders.get(resourceMethod);
            methodBuilder.addResourceMethodExecution(executionTime, startTime, requestExecutionTime, requestStartTime);
        }

        private void addResourceExecution(long executionTime, Date startTime, long requestExecutionTime, Date requestStartTime) {
            resourceExecutionStatisticsBuilder.addExecution(executionTime, startTime);
            requestExecutionStatisticsBuilder.addExecution(requestExecutionTime, requestStartTime);
        }
    }

    private ResourceStatistics(Resource resource, Map<ResourceMethod, ResourceMethodStatistics> resourceMethods,
                               Map<Resource, ResourceStatistics> childResources, ExecutionStatistics executionStatisticsBuilder,
                               ExecutionStatistics requestExecutionStatistics) {
        this.resource = resource;
        this.resourceMethods = resourceMethods;
        this.childResources = childResources;

        this.resourceExecutionStatistics = executionStatisticsBuilder;
        this.requestExecutionStatistics = requestExecutionStatistics;
    }

    public Map<Resource, ResourceStatistics> getChildResources() {
        return childResources;
    }

    public Resource getResource() {
        return resource;
    }

    public Map<ResourceMethod, ResourceMethodStatistics> getResourceMethods() {
        return resourceMethods;
    }

    public ExecutionStatistics getResourceExecutionStatistics() {
        return resourceExecutionStatistics;
    }

    public ExecutionStatistics getRequestExecutionStatistics() {
        return requestExecutionStatistics;
    }
}
