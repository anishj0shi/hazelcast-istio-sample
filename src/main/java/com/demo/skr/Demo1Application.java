package com.demo.skr;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.hazelcast.cluster.Member;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.HazelcastInstanceProxy;

import reactor.core.publisher.Mono;

@SpringBootApplication
public class Demo1Application implements CommandLineRunner {
	
	public static final Logger LOGGER = LoggerFactory.getLogger(Demo1Application.class);

	public static void main(String[] args) {
		SpringApplication.run(Demo1Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Config config = new Config();
		config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		config.getNetworkConfig().getJoin().getKubernetesConfig().setEnabled(true)
		      .setProperty("namespace", "hazel-test")
		      .setProperty("service-name", "hazel-service");
		Hazelcast.newHazelcastInstance( config );
		
		String members = Hazelcast.getAllHazelcastInstances().stream().flatMap(hz -> retrieveMembers(hz))
				.map(member -> member.getAddress().toString()).collect(Collectors.joining(";"));
		StringBuilder builder = new StringBuilder();
		builder.append("Hazelcast members: " + members);
		LOGGER.warn(builder.toString());
	}

	@Bean
	public RouterFunction<ServerResponse> route() {
		return RouterFunctions.route(RequestPredicates.GET("/hello"), this::hello)
				.andRoute(RequestPredicates.GET("env"), this::env);
	}

	private Mono<ServerResponse> hello(ServerRequest serverRequest) {
		return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN)
				.body(BodyInserters.fromValue("Hello World " + System.getenv("GREETING_NAME") + "!"));
	}

	private Mono<ServerResponse> env(ServerRequest serverRequest) {
		StringBuilder builder = new StringBuilder();
		String ip = "";
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		String dnsIps = "";
		try {
			dnsIps = Arrays.stream(InetAddress.getAllByName("hazelcastservice.for-me.svc.cluster.local"))
					.map(adr -> adr.getHostAddress()).collect(Collectors.joining(";"));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		builder.append("Local IP Address " + ip + "\n");
		builder.append("DNS IP Addresses " + dnsIps + "\n");
		String members = Hazelcast.getAllHazelcastInstances().stream().flatMap(hz -> retrieveMembers(hz))
				.map(member -> member.getAddress().toString()).collect(Collectors.joining(";"));
		builder.append("\n");
		builder.append("Hazelcast members: " + members);
		builder.append("\n");
		builder.append("\n");

//        final Properties properties = System.getProperties();
//        builder.append("System Properties: ");
//        properties.entrySet().stream().forEach(entry -> {
//            builder.append("Key: ").append(entry.getKey()).append(" --> ").append("Value: ").append(entry.getValue()).append("\n");
//        });
//        builder.append("\n");

		final Map<String, String> env = new TreeMap<>(System.getenv());
		builder.append("Environment: ");
		env.entrySet().stream().forEach(entry -> {
			builder.append(entry.getKey()).append(" --> ").append(entry.getValue()).append("\n");
		});
		builder.append("\n");
		return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).body(BodyInserters.fromValue(builder.toString()));
	}

	private Stream<Member> retrieveMembers(HazelcastInstance hz) {
		if (hz instanceof HazelcastInstanceProxy) {
			return hz.getCluster().getMembers().stream();
		} else {
			return hz.getCluster().getMembers().stream();
		}
	}

}
