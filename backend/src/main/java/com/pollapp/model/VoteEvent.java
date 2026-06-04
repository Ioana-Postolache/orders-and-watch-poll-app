package com.pollapp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VoteEvent {
    @JsonProperty("pollId")
    private String pollId;
    @JsonProperty("voterId")
    private String voterId;
    @JsonProperty("option")
    private String option;
    @JsonProperty("timestamp")
    private long timestamp;

    public VoteEvent() {
    }

    public VoteEvent(String pollId, String voterId, String option, long timestamp) {
        this.pollId = pollId;
        this.voterId = voterId;
        this.option = option;
        this.timestamp = timestamp;
    }

    public String getPollId() {
        return pollId;
    }

    public String getVoterId() {
        return voterId;
    }

    public String getOption() {
        return option;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
