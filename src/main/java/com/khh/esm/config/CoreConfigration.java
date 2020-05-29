package com.khh.esm.config;

import com.khh.esm.config.properties.ProjectProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


import java.net.InetAddress;

@Configuration
@Slf4j
@EnableConfigurationProperties(ProjectProperties.class)
//@EnableElasticsearchRepositories
@EnableScheduling
public class CoreConfigration {

    @Autowired
    ProjectProperties projectProperties;


//
}
