package com.pollapp.config;

import com.pollapp.grpc.PollGrpcService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class GrpcServerConfig {

    @Value("${grpc.server.port:9090}")
    private int grpcPort;

    private final PollGrpcService pollGrpcService;

    private Server server;

    private static final Logger logger = LoggerFactory.getLogger(GrpcServerConfig.class);

    public GrpcServerConfig(PollGrpcService pollGrpcService) {
        this.pollGrpcService = pollGrpcService;
    }

    @PostConstruct
    public void start() throws IOException {
        server = NettyServerBuilder.forPort(grpcPort)
                .addService(pollGrpcService)
                .build()
                .start();
        logger.info("gRPC server started on port {}", grpcPort);
    }

    @PreDestroy
    public void stop() {
        if (server != null)
            server.shutdown();
    }
}