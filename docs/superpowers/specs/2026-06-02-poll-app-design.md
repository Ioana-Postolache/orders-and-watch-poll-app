# Poll App Design — "What should we order / watch?"

Date: 2026-06-02

## Purpose

A LAN poll app for learning Spring Boot, gRPC, Kafka, React, Zustand, and Material UI.
The host creates a poll with custom options; voters join via room code from their phones;
a host screen shows live animated result bars.

---

## Constraints & decisions

- One active poll at a time (no multi-room)
- Voters join via a 4-letter room code (e.g. "KIWI") — no login
- Voters can change their vote; the latest vote wins
- Host can close the poll (freezes results, disconnects all streams)
- Voter identity: UUID generated client-side, stored in `sessionStorage`
- Persistence: in-memory only (learning project — no database)
- Build tool: Gradle (backend), Vite (frontend)

---

## gRPC API

Single `.proto` file at `proto/poll.proto` — shared source of truth for backend and frontend codegen.

```protobuf
service PollService {
  rpc CreatePoll(CreatePollRequest)         returns (CreatePollResponse);
  rpc CastVote(CastVoteRequest)             returns (CastVoteResponse);
  rpc ClosePoll(ClosePollRequest)           returns (ClosePollResponse);
  rpc StreamResults(StreamResultsRequest)   returns (stream TallyUpdate);
}

message CreatePollRequest {
  string          question = 1;
  repeated string options  = 2;
}
message CreatePollResponse {
  string poll_id   = 1;
  string room_code = 2;
}

message CastVoteRequest {
  string poll_id  = 1;
  string option   = 2;
  string voter_id = 3;
}
message CastVoteResponse { bool accepted = 1; }

message ClosePollRequest  { string poll_id = 1; }
message ClosePollResponse { bool closed   = 1; }

message StreamResultsRequest { string poll_id = 1; }
message TallyUpdate {
  map<string, int32> counts      = 1;
  bool               poll_closed = 2;
}
```

grpc-web supports unary and server-streaming only — this API fits that constraint exactly.

---

## System architecture

```
[React (Voter)]  [React (Host)]
       \               /
        grpc-web :8080
              |
        [Envoy Proxy]          ← Docker, grpc_web filter
              |
         gRPC :9090
              |
      [Spring Boot]
       /           \
  Kafka producer   Kafka consumer
       \           /
    [Kafka :9092]               ← Docker
    topic: vote-events
```

**Flow:**
1. Voter taps option → `CastVote` (grpc-web) → Envoy → Spring Boot
2. Spring Boot publishes `VoteEvent` to Kafka topic `vote-events`
3. `VoteConsumer` receives event, updates `PollStore`, recomputes tallies
4. Recomputed tallies are broadcast to all open `StreamResults` observers via `onNext`
5. Zustand store updates → React re-renders → MUI `LinearProgress` bars animate

---

## Backend structure

```
backend/src/main/java/com/pollapp/
├── grpc/PollGrpcService.java      # PollServiceGrpc.PollServiceImplBase
├── kafka/VoteProducer.java        # KafkaTemplate.send("vote-events", event)
├── kafka/VoteConsumer.java        # @KafkaListener — tallies + broadcasts
├── model/Poll.java                # question, options, roomCode, status
├── model/VoteEvent.java           # pollId, voterId, option, timestamp
└── state/PollStore.java           # @Component singleton: active poll + voterMap
```

**`PollStore`** holds:
- `Poll activePoll`
- `Map<String, String> voterMap` — voterId → option (latest vote)

**Tally recomputation** (in `VoteConsumer` after each event):
```
voterMap.put(voterId, option)
tallies = voterMap.values().stream()
            .collect(groupingBy(identity(), counting()))
broadcast TallyUpdate(tallies) to all StreamObservers
```

**`PollGrpcService`** holds:
- `CopyOnWriteArrayList<StreamObserver<TallyUpdate>> observers`

On `StreamResults` connect:
1. Send `TallyUpdate(currentTallies)` immediately (snapshot)
2. Add observer to `observers` list (tail)

On `ClosePoll`:
1. Set `activePoll.status = CLOSED`
2. Send `TallyUpdate(tallies, poll_closed=true)` to all observers
3. Call `onCompleted()` on each, clear the list

**Kafka event (JSON):**
```json
{ "pollId": "abc-123", "voterId": "uuid", "option": "Pizza", "timestamp": 1717350000000 }
```

---

## Frontend structure

```
frontend/src/
├── proto/                    # generated stubs (poll_pb.js, poll_grpc_web_pb.js)
├── grpc/pollClient.ts        # thin wrapper: createPoll(), castVote(), streamResults(), closePoll()
├── store/pollStore.ts        # Zustand store
├── pages/
│   ├── HostPage.tsx          # create form → room code display → live bars → close button
│   └── VotePage.tsx          # room code entry → option buttons → "your vote: X"
├── components/
│   ├── ResultBar.tsx         # MUI LinearProgress + label, reads from tallies
│   └── OptionButton.tsx      # MUI Button, highlighted when option === myVote
└── App.tsx                   # React Router: /host → HostPage, /vote → VotePage
```

**Zustand store:**
```ts
interface PollStore {
  pollId:   string | null
  question: string
  options:  string[]
  roomCode: string | null
  tallies:  Record<string, number>
  myVote:   string | null
  closed:   boolean

  setPoll:    (id, question, options, roomCode) => void
  setTallies: (tallies: Record<string, number>) => void
  setMyVote:  (option: string) => void
  setClosed:  () => void
}
```

- `streamResults()` opens the server-stream and calls `setTallies` on every message
- `myVote` is mirrored to `sessionStorage` so a page refresh restores it
- `voterId` = `sessionStorage.getItem('voterId') ?? crypto.randomUUID()`

---

## Infrastructure

```
orders-and-watch-poll-app/
├── proto/poll.proto
├── backend/              (Spring Boot, Gradle)
├── frontend/             (React, Vite)
├── envoy/envoy.yaml      (grpc_web filter → localhost:9090)
└── docker-compose.yml    (Kafka + Zookeeper + Envoy)
```

| Process     | How                   | Port |
|-------------|-----------------------|------|
| Spring Boot | `./gradlew bootRun`   | 9090 |
| React       | `npm run dev`         | 5173 |
| Envoy       | Docker Compose        | 8080 |
| Kafka       | Docker Compose        | 9092 |
| Zookeeper   | Docker Compose        | 2181 |

---

## What's intentionally out of scope

- Database persistence (in-memory is fine for learning)
- Kafka offset tracking for replay (snapshot from `PollStore` is sufficient)
- Authentication beyond the room code
- Multiple simultaneous polls
- Voter names / identities beyond a session UUID

---

## Learning goals by section

| Task area | Technologies practiced |
|---|---|
| Write `poll.proto` | Protocol Buffers |
| Configure Gradle protobuf plugin | Gradle, gRPC Java codegen |
| Implement `PollGrpcService` | Spring Boot, gRPC Java |
| Wire Kafka producer | Spring Kafka, KafkaTemplate |
| Wire Kafka consumer + tally logic | Spring Kafka, @KafkaListener |
| Scaffold React app + React Router | React, Vite |
| Generate grpc-web stubs | protoc, grpc-web plugin |
| Build Zustand store | Zustand |
| Build VotePage + HostPage | React, Material UI |
| Animate result bars | MUI LinearProgress, CSS transitions |
| Configure Envoy | Envoy, grpc_web filter |
| Write docker-compose | Docker Compose |
