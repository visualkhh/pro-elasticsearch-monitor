//package com.khh.esm.config;
//
//import org.elasticsearch.client.Client;
//import org.elasticsearch.client.transport.TransportClient;
//import org.elasticsearch.common.settings.Settings;
////import org.elasticsearch.client.transport.TransportClient;
////import org.elasticsearch.common.transport.TransportAddress;
//import org.elasticsearch.common.transport.TransportAddress;
//import org.elasticsearch.transport.client.PreBuiltTransportClient;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.elasticsearch.config.ElasticsearchConfigurationSupport;
//import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
//import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
//import org.springframework.scheduling.annotation.EnableScheduling;
//
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//
//@Configuration
//@EnableElasticsearchRepositories
//public class TransportClientConfig extends ElasticsearchConfigurationSupport {
//
//    @Bean
//    public Client elasticsearchClient() throws UnknownHostException {
//        Settings settings = Settings.builder().put("cluster.name", "elasticsearch").build();
//        TransportClient client = new PreBuiltTransportClient(settings);
//        client.addTransportAddress(new TransportAddress(InetAddress.getByName("119.206.205.181"), 9200));
//        return client;
//    }
//
//    @Bean(name = { "elasticsearchOperations", "elasticsearchTemplate" })
//    public ElasticsearchTemplate elasticsearchTemplate() throws UnknownHostException {
//        return new ElasticsearchTemplate(elasticsearchClient());
//    }
//}
