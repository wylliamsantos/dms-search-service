package br.com.dms.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PrometheusConfiguration {

    private final String applicationName;

    PrometheusConfiguration(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer() {
        return registry -> registry.config().commonTags("application", applicationName);
    }
}
