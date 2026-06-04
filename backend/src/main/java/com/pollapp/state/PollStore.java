package com.pollapp.state;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.pollapp.model.Poll;
import com.pollapp.model.PollStatus;

@Component
public class PollStore {

    private Poll activePoll;
    // voterMap: voterId → option (their current vote)
    private final Map<String, String> voterMap = new ConcurrentHashMap<>();

    public synchronized void setActivePoll(Poll poll) {
        activePoll = poll;
        clearVotes();
    }

    public synchronized Poll getActivePoll() {
        return activePoll;
    }

    public synchronized Poll getActivePollByRoomCode(String roomCode) {
        if (activePoll == null) {
            return null;
        }
        if (activePoll.getRoomCode().equals(roomCode)) {
            return activePoll;
        }
        return null;
    }

    public synchronized boolean recordVote(String voterId, String option) {
        if (activePoll == null || activePoll.getStatus() == PollStatus.CLOSED) {
            return false;
        }
        voterMap.put(voterId, option);
        return true;
    }

    public synchronized Map<String, Integer> computeTallies() {
        Map<String, Integer> tallies = new HashMap<>();

        voterMap.values()
                .forEach(option -> tallies.put(option,
                        tallies.getOrDefault(option, 0) + 1));
        return tallies;
    }

    public synchronized void closePoll() {
        activePoll.setStatus(PollStatus.CLOSED);
    }

    public synchronized void clearVotes() {
        voterMap.clear();
    }
}