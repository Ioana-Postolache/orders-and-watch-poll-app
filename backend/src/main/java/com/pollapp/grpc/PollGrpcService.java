package com.pollapp.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pollapp.kafka.VoteProducer;
import com.pollapp.model.Poll;
import com.pollapp.model.PollStatus;
import com.pollapp.model.VoteEvent;
import com.pollapp.proto.CastVoteRequest;
import com.pollapp.proto.CastVoteResponse;
import com.pollapp.proto.CreatePollRequest;
import com.pollapp.proto.CreatePollResponse;
import com.pollapp.proto.PollServiceGrpc;
import com.pollapp.state.PollStore;

import io.grpc.stub.StreamObserver;

import java.util.Random;
import java.util.UUID;

import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Component;

@Component
public class PollGrpcService extends PollServiceGrpc.PollServiceImplBase {
    private final PollStore pollStore;
    private final VoteProducer voteProducer;
    private final Random random = new Random();

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PollGrpcService.class);

    public PollGrpcService(PollStore pollStore, VoteProducer voteProducer) {
        this.pollStore = pollStore;
        this.voteProducer = voteProducer;
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

    @Override
    public void castVote(
            CastVoteRequest request,
            StreamObserver<CastVoteResponse> responseObserver) {
        CastVoteResponse response;

        Poll activePoll = pollStore.getActivePoll();

        if (activePoll == null
                || activePoll.getStatus() == PollStatus.CLOSED) {

            response = CastVoteResponse.newBuilder().setAccepted(false).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        VoteEvent voteEvent = new VoteEvent(request.getPollId(), request.getVoterId(),
                request.getOption(), System.currentTimeMillis());

        try {
            voteProducer.send(voteEvent);
        } catch (KafkaException | JsonProcessingException e) {
            logger.error("Failed to send vote event", e);
            responseObserver.onNext(CastVoteResponse.newBuilder().setAccepted(false).build());
            responseObserver.onCompleted();
            return;
        } 
        response = CastVoteResponse.newBuilder().setAccepted(true).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}