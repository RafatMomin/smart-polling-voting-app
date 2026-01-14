package onetoone.LiveDiscussion.controller;

import onetoone.LiveDiscussion.dto.*;
import onetoone.LiveDiscussion.model.*;
import onetoone.LiveDiscussion.service.DiscussionService;
import onetoone.Users.model.Users;
import onetoone.Users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/discussions")
@CrossOrigin(origins = "*")
public class DiscussionController {

    @Autowired
    private DiscussionService discussionService;

    @Autowired
    private UserRepository userRepository;

    // Helper method to extract user from auth token
    private Users getUserFromToken(String authToken) {
        if (authToken == null || authToken.isEmpty()) {
            throw new IllegalArgumentException("Invalid token");
        }
        Users user = userRepository.findByauthtoken(authToken);
        if (user == null) {
            throw new IllegalArgumentException("Invalid token");
        }
        return user;
    }

    /**
     * 1. Create Poll Discussion
     * POST /discussions/poll/{pollId}
     */
    @PostMapping("/poll/{pollId}")
    public ResponseEntity<?> createPollDiscussion(
            @PathVariable Long pollId,
            @RequestBody CreateDiscussionRequest request,
            @RequestHeader("Authorization") String authToken) {
        try {
            Users user = getUserFromToken(authToken);
            Discussion discussion = discussionService.createPollDiscussion(
                    pollId, request.getTitle(), request.getDescription(), user.getEmailId());

            DiscussionResponse response = buildDiscussionResponse(discussion);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create discussion: " + e.getMessage()));
        }
    }

    /**
     * 2. Create General Discussion
     * POST /discussions/general
     */
    @PostMapping("/general")
    public ResponseEntity<?> createGeneralDiscussion(
            @RequestBody CreateGeneralDiscussionRequest request,
            @RequestHeader("Authorization") String authToken) {
        try {
            Users user = getUserFromToken(authToken);
            Discussion discussion = discussionService.createGeneralDiscussion(
                    request.getTitle(), request.getDescription(),
                    request.getTags(), user.getEmailId());

            DiscussionResponse response = buildDiscussionResponse(discussion);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create discussion: " + e.getMessage()));
        }
    }

    /**
     * 3. Get All Discussions
     * GET /discussions
     */
    @GetMapping
    public ResponseEntity<?> getAllDiscussions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long pollId,
            @RequestParam(required = false) String tags) {
        try {
            DiscussionType discussionType = type != null ? DiscussionType.valueOf(type) : null;
            List<Discussion> discussions = discussionService.getAllDiscussions(
                    discussionType, active, pollId, tags);

            List<DiscussionSummaryResponse> responses = discussions.stream()
                    .map(this::buildDiscussionSummaryResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve discussions: " + e.getMessage()));
        }
    }

    /**
     * 4. Get Discussion Details
     * GET /discussions/{discussionId}
     */
    @GetMapping("/{discussionId}")
    public ResponseEntity<?> getDiscussionDetails(@PathVariable Long discussionId) {
        try {
            Discussion discussion = discussionService.getDiscussionById(discussionId);
            List<DiscussionParticipant> participants =
                    discussionService.getParticipants(discussionId);

            DiscussionDetailResponse response = buildDiscussionDetailResponse(
                    discussion, participants);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Discussion not found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve discussion: " + e.getMessage()));
        }
    }

    /**
     * 5. Join Discussion
     * POST /discussions/{discussionId}/join
     */
    @PostMapping("/{discussionId}/join")
    public ResponseEntity<?> joinDiscussion(
            @PathVariable Long discussionId,
            @RequestHeader("Authorization") String authToken) {
        try {
            Users user = getUserFromToken(authToken);
            discussionService.joinDiscussion(discussionId, user.getEmailId());

            long participantCount = discussionService.getParticipantCount(discussionId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully joined discussion");
            response.put("discussionId", discussionId);
            response.put("websocketEndpoint", "ws://coms-3090-036.class.las.iastate.edu:8080/discussion/" + discussionId);
            response.put("participantCount", participantCount);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to join discussion: " + e.getMessage()));
        }
    }

    /**
     * 6. Leave Discussion
     * POST /discussions/{discussionId}/leave
     */
    @PostMapping("/{discussionId}/leave")
    public ResponseEntity<?> leaveDiscussion(
            @PathVariable Long discussionId,
            @RequestHeader("Authorization") String authToken) {
        try {
            Users user = getUserFromToken(authToken);
            discussionService.leaveDiscussion(discussionId, user.getEmailId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully left discussion");
            response.put("discussionId", discussionId);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to leave discussion: " + e.getMessage()));
        }
    }

    /**
     * 7. Get Discussion Messages
     * GET /discussions/{discussionId}/messages
     */
    @GetMapping("/{discussionId}/messages")
    public ResponseEntity<?> getDiscussionMessages(
            @PathVariable Long discussionId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestHeader("Authorization") String authToken) {
        try {
            Users user = getUserFromToken(authToken);

            // Check if user is a participant
            if (!discussionService.isParticipant(discussionId, user.getEmailId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Not a participant in this discussion"));
            }

            List<DiscussionMessage> messages = discussionService.getMessages(
                    discussionId, limit, before, after);

            int messageLimit = (limit != null && limit <= 200) ? limit : 50;
            boolean hasMore = messages.size() >= messageLimit;
            Long nextCursor = (!messages.isEmpty()) ?
                    messages.get(messages.size() - 1).getId() : null;

            Map<String, Object> response = new HashMap<>();
            response.put("discussionId", discussionId);
            response.put("messages", messages.stream()
                    .map(this::buildMessageResponse)
                    .collect(Collectors.toList()));
            response.put("hasMore", hasMore);
            response.put("nextCursor", nextCursor);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve messages: " + e.getMessage()));
        }
    }

    /**
     * 8. Get Discussion Participants
     * GET /discussions/{discussionId}/participants
     */
    @GetMapping("/{discussionId}/participants")
    public ResponseEntity<?> getParticipants(@PathVariable Long discussionId) {
        try {
            List<DiscussionParticipant> participants =
                    discussionService.getParticipants(discussionId);

            Map<String, Object> response = new HashMap<>();
            response.put("discussionId", discussionId);
            response.put("participantCount", participants.size());
            response.put("participants", participants.stream()
                    .map(this::buildParticipantResponse)
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve participants: " + e.getMessage()));
        }
    }

    /**
     * 9. Close Discussion
     * PUT /discussions/{discussionId}/close
     */
    @PutMapping("/{discussionId}/close")
    public ResponseEntity<?> closeDiscussion(
            @PathVariable Long discussionId,
            @RequestHeader("Authorization") String authToken) {
        try {
            Users user = getUserFromToken(authToken);
            discussionService.closeDiscussion(discussionId, user.getEmailId());

            Discussion discussion = discussionService.getDiscussionById(discussionId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Discussion closed successfully");
            response.put("discussionId", discussionId);
            response.put("closedAt", discussion.getClosedAt());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to close discussion: " + e.getMessage()));
        }
    }

    /**
     * 10. Delete Discussion
     * DELETE /discussions/{discussionId}
     */
    @DeleteMapping("/{discussionId}")
    public ResponseEntity<?> deleteDiscussion(
            @PathVariable Long discussionId,
            @RequestHeader("Authorization") String authToken) {
        try {
            Users user = getUserFromToken(authToken);
            discussionService.deleteDiscussion(discussionId, user.getEmailId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Discussion deleted successfully");
            response.put("discussionId", discussionId);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete discussion: " + e.getMessage()));
        }
    }

    /**
     * 11. Get User's Discussions
     * GET /discussions/my
     */
    @GetMapping("/my")
    public ResponseEntity<?> getUserDiscussions(
            @RequestParam(required = false, defaultValue = "ALL") String role,
            @RequestParam(required = false) Boolean active,
            @RequestHeader("Authorization") String authToken) {
        try {
            Users user = getUserFromToken(authToken);
            Map<String, List<Discussion>> userDiscussions =
                    discussionService.getUserDiscussions(user.getEmailId(), role, active);

            Map<String, Object> response = new HashMap<>();

            if (userDiscussions.containsKey("hosted")) {
                response.put("hosted", userDiscussions.get("hosted").stream()
                        .map(this::buildDiscussionSummaryResponse)
                        .collect(Collectors.toList()));
            }

            if (userDiscussions.containsKey("participating")) {
                response.put("participating", userDiscussions.get("participating").stream()
                        .map(this::buildDiscussionSummaryResponse)
                        .collect(Collectors.toList()));
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve discussions: " + e.getMessage()));
        }
    }

    /**
     * 12. Search Discussions
     * GET /discussions/search
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchDiscussions(
            @RequestParam String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tags) {
        try {
            DiscussionType discussionType = type != null ? DiscussionType.valueOf(type) : null;
            List<Discussion> results = discussionService.searchDiscussions(q, discussionType, tags);

            List<DiscussionSummaryResponse> responses = results.stream()
                    .map(this::buildDiscussionSummaryResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to search discussions: " + e.getMessage()));
        }
    }

    // Helper methods to build response objects

    private DiscussionResponse buildDiscussionResponse(Discussion discussion) {
        Map<String, String> hostUser = new HashMap<>();
        hostUser.put("emailId", discussion.getHostUser().getEmailId());
        hostUser.put("name", discussion.getHostUser().getName());

        Long pollId = discussion.getPoll() != null ? discussion.getPoll().getId() : null;

        long participantCount = discussionService.getParticipantCount(discussion.getId());
        long messageCount = discussionService.getMessageCount(discussion.getId());

        return new DiscussionResponse(
                discussion.getId(),
                discussion.getTitle(),
                discussion.getDescription(),
                discussion.getType().name(),
                pollId,
                hostUser,
                discussion.getCreatedAt(),
                discussion.isActive(),
                participantCount,
                messageCount,
                "ws://coms-3090-036.class.las.iastate.edu:8080/discussion/" + discussion.getId(),
                discussion.getTags()
        );
    }

    private DiscussionSummaryResponse buildDiscussionSummaryResponse(Discussion discussion) {
        Map<String, String> hostUser = new HashMap<>();
        hostUser.put("emailId", discussion.getHostUser().getEmailId());
        hostUser.put("name", discussion.getHostUser().getName());

        Long pollId = discussion.getPoll() != null ? discussion.getPoll().getId() : null;

        long participantCount = discussionService.getParticipantCount(discussion.getId());
        long messageCount = discussionService.getMessageCount(discussion.getId());

        return new DiscussionSummaryResponse(
                discussion.getId(),
                discussion.getTitle(),
                discussion.getType().name(),
                pollId,
                hostUser,
                discussion.getCreatedAt(),
                discussion.isActive(),
                participantCount,
                messageCount,
                discussion.getLastActivity(),
                discussion.getTags()
        );
    }

    private DiscussionDetailResponse buildDiscussionDetailResponse(
            Discussion discussion, List<DiscussionParticipant> participants) {
        Map<String, String> hostUser = new HashMap<>();
        hostUser.put("emailId", discussion.getHostUser().getEmailId());
        hostUser.put("name", discussion.getHostUser().getName());

        Long pollId = discussion.getPoll() != null ? discussion.getPoll().getId() : null;

        List<ParticipantResponse> participantResponses = participants.stream()
                .map(this::buildParticipantResponse)
                .collect(Collectors.toList());

        long messageCount = discussionService.getMessageCount(discussion.getId());

        return new DiscussionDetailResponse(
                discussion.getId(),
                discussion.getTitle(),
                discussion.getDescription(),
                discussion.getType().name(),
                pollId,
                discussion.getPoll(),
                hostUser,
                participantResponses,
                discussion.getCreatedAt(),
                discussion.isActive(),
                participants.size(),
                messageCount,
                discussion.getLastActivity(),
                "ws://coms-3090-036.class.las.iastate.edu:8080/discussion/" + discussion.getId()
        );
    }

    private ParticipantResponse buildParticipantResponse(DiscussionParticipant participant) {
        return new ParticipantResponse(
                participant.getUser().getEmailId(),
                participant.getUser().getName(),
                participant.getJoinedAt(),
                participant.isHost(),
                participant.isOnline(),
                participant.getLastSeen()
        );
    }

    private MessageResponse buildMessageResponse(DiscussionMessage message) {
        Map<String, String> user = new HashMap<>();
        user.put("emailId", message.getSender().getEmailId());
        user.put("name", message.getSender().getName());

        return new MessageResponse(
                message.getId(),
                user,
                message.getContent(),
                message.getMessageType().name(),
                message.getRecipientEmail(),
                message.getTimestamp(),
                message.isEdited()
        );
    }
}