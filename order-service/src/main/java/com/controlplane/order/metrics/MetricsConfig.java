package com.controlplane.order.metrics;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer instrumentation config.
 *
 * Registers:
 *   - Common tags (service, env, version) on every metric
 *   - @Timed  aspect  → timing any method annotated with @Timed
 *   - @Counted aspect → counting any method annotated with @Counted
 *   - JVM metrics: GC, memory, threads
 *   - System: CPU
 */
@Configuration
public class MetricsConfig {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${app.env:local}")
    private String appEnv;

    @Value("${app.version:unknown}")
    private String appVersion;

    /**
     * Common tags applied to every metric exported from this service.
     * These appear as labels in Prometheus: service="order-service", env="prod"
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return registry -> registry.config()
                .commonTags(
                    "service", appName,
                    "env",     appEnv,
                    "version", appVersion
                )
                .meterFilter(MeterFilter.deny(id ->
                    id.getName().startsWith("jvm.classes")));  // reduce cardinality
    }

    /** Enables @Timed on any Spring bean method */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /** Enables @Counted on any Spring bean method */
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }

    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }
}
