package com.pollapp.grpc;

import com.pollapp.proto.PollServiceGrpc;
import org.springframework.stereotype.Component;

@Component
public class PollGrpcService extends PollServiceGrpc.PollServiceImplBase {
    // RPCs will be added in the next tasks
}