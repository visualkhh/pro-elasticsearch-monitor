package com.khh.esm.config.properties;


import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Component
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "project")
public class ProjectProperties {
    private Map<String, String> properties = new HashMap<>();
}

