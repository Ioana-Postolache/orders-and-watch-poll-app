package com.pollapp.grpc;

import com.pollapp.model.Poll;
import com.pollapp.model.PollStatus;
import com.pollapp.proto.CreatePollRequest;
import com.pollapp.proto.CreatePollResponse;
import com.pollapp.proto.PollServiceGrpc;
import com.pollapp.state.PollStore;

import io.grpc.stub.StreamObserver;

import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class PollGrpcService extends PollServiceGrpc.PollServiceImplBase {
    private final PollStore pollStore;
    private final Random random = new Random();

    public PollGrpcService(PollStore pollStore) {
        this.pollStore = pollStore;
    }

    @Override
    public void createPoll(
            CreatePollRequest request,
            StreamObserver<CreatePollResponse> responseObserver) {

        StringBuilder roomCodeSb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            roomCodeSb.append((char) ('A' + random.nextInt(26)));
        }
        String roomCode = roomCodeSb.toString();
        String pollId = UUID.randomUUID().toString();

        Poll poll = new Poll(pollId, request.getQuestion(),
                request.getOptionsList(), roomCode, PollStatus.OPEN);

        pollStore.setActivePoll(poll);
        CreatePollResponse response = CreatePollResponse.newBuilder()
                .setPollId(pollId)
                .setRoomCode(roomCode)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}