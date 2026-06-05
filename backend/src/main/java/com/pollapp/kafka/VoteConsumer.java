package com.pollapp.kafka;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollapp.grpc.PollGrpcService;
import com.pollapp.model.VoteEvent;
import com.pollapp.state.PollStore;

@Component
public class VoteConsumer {

    private final ObjectMapper objectMapper;
    private final PollStore pollStore;
    private final PollGrpcService pollGrpcService;
    private static final Logger logger = LoggerFactory.getLogger(VoteConsumer.class);

    public VoteConsumer(ObjectMapper objectMapper, PollStore pollStore, PollGrpcService pollGrpcService) {
        this.objectMapper = objectMapper;
        this.pollStore = pollStore;
        this.pollGrpcService = pollGrpcService;
    }

    @KafkaListener(topics = "vote-events", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        VoteEvent voteEvent;
        try {
            voteEvent = objectMapper.readValue(message, VoteEvent.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to read vote event", e);
            return;
        }
        pollStore.recordVote(voteEvent.getVoterId(), voteEvent.getOption());

        Map<String, Integer> tallies = pollStore.computeTallies();
        pollGrpcService.broadcastTallies(tallies);
    }
}
