package com.dc.elastic;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.dc.elastic.model")
public class EsConfig {

	private TransportClient client;

	@Bean
	public TransportClient client() throws UnknownHostException {

		Settings settings = Settings.builder().put("cluster.name", "elasticsearch_dwai1714").build();
		TransportClient client = new PreBuiltTransportClient(settings).addTransportAddress
				(new TransportAddress(InetAddress.getByName("127.0.0.1"), 9300));
//		TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
//		        .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300));


		return client;
	}
	

    
	// Add transport addresses and do something with the client...
	// @Bean
	// public ElasticsearchOperations elasticsearchTemplate() throws Exception {
	// return new ElasticsearchTemplate(client());
	// }

	// Embedded Elasticsearch Server
	/*
	 * @Bean public ElasticsearchOperations elasticsearchTemplate() { return new
	 * ElasticsearchTemplate(nodeBuilder().local(true).node().client()); }
	 */

}