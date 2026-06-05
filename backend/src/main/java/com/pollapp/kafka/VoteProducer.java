package com.pollapp.kafka;

import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollapp.model.VoteEvent;

@Component
public class VoteProducer {

    private static final String TOPIC = "vote-events";
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public VoteProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish a VoteEvent as JSON to the vote-events topic.
     * Key = voterId (ensures ordering per voter on a single partition).
     * 
     * @throws JsonProcessingException
     */
    public void send(VoteEvent event) throws KafkaException, JsonProcessingException {
        String pollEventJson = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(TOPIC, event.getVoterId(), pollEventJson);
    }
}
