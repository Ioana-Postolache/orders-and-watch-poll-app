package com.pollapp.model;
import java.util.List;

public class Poll {
    private String id;
    private String question;
    private List<String> options;
    private String roomCode;
    private PollStatus status;

    public Poll(String id, String question, List<String> options, String roomCode, PollStatus status) {
        this.id = id;
        this.question = question;
        this.options = options;
        this.roomCode = roomCode;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public PollStatus getStatus() {
        return status;
    }

    public void setStatus(PollStatus status) {
        this.status = status;
    }

}