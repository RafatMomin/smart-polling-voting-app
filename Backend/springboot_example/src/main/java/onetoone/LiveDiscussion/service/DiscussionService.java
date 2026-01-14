package onetoone.LiveDiscussion.service;

import onetoone.LiveDiscussion.model.*;
import onetoone.LiveDiscussion.repository.DiscussionRepository;
import onetoone.Polling.model.Poll;
import onetoone.Polling.repository.PollRepository;
import onetoone.Users.model.Users;
import onetoone.Users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiscussionService {

    private static final Logger log = LoggerFactory.getLogger(DiscussionService.class);

    @Autowired
    private DiscussionRepository discussionRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final int MAX_PARTICIPANTS = 100;
    private static final int MAX_MESSAGE_LENGTH = 2000;

    /**
     * Create a poll-specific discussion
     */
    @Transactional
    public Discussion createPollDiscussion(Long pollId, String title, String description, String userEmail) {
        // Validate poll exists
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));

        // Check if discussion already exists for this poll
        Optional<Discussion> existing = discussionRepository.findByPollId(pollId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Discussion already exists for this poll");
        }

        // Get user
        Users user = userRepository.findByEmailId(userEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // Create discussion
        Discussion discussion = new Discussion(title, description, DiscussionType.POLL_DISCUSSION, user);
        discussion.setPoll(poll);
        discussion = discussionRepository.save(discussion);

        // Add host as participant using EntityManager
        DiscussionParticipant hostParticipant = new DiscussionParticipant(discussion, user, true);
        entityManager.persist(hostParticipant);
        entityManager.flush();

        log.info("Created poll discussion {} for poll {}", discussion.getId(), pollId);
        return discussion;
    }

    /**
     * Create a general discussion
     */
    @Transactional
    public Discussion createGeneralDiscussion(String title, String description, List<String> tags, String userEmail) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        Users user = userRepository.findByEmailId(userEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Discussion discussion = new Discussion(title, description, DiscussionType.GENERAL_DISCUSSION, user);
        if (tags != null && !tags.isEmpty()) {
            discussion.setTags(tags);
        }
        discussion = discussionRepository.save(discussion);

        // Add host as participant
        DiscussionParticipant hostParticipant = new DiscussionParticipant(discussion, user, true);
        entityManager.persist(hostParticipant);
        entityManager.flush();

        log.info("Created general discussion {} by user {}", discussion.getId(), userEmail);
        return discussion;
    }

    /**
     * Get all discussions with optional filters
     */
    public List<Discussion> getAllDiscussions(DiscussionType type, Boolean active, Long pollId, String tags) {
        if (pollId != null) {
            return discussionRepository.findByPollId(pollId).map(List::of).orElse(Collections.emptyList());
        }

        if (type != null && active != null) {
            return discussionRepository.findByTypeAndActive(type, active);
        }

        if (type != null) {
            return discussionRepository.findByType(type);
        }

        if (active != null) {
            return discussionRepository.findByActive(active);
        }

        if (tags != null && !tags.isEmpty()) {
            String[] tagArray = tags.split(",");
            Set<Discussion> result = new HashSet<>();
            for (String tag : tagArray) {
                result.addAll(discussionRepository.findByTag(tag.trim()));
            }
            return new ArrayList<>(result);
        }

        return discussionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Discussion getDiscussionById(Long discussionId) {
        return discussionRepository.findByIdWithDetails(discussionId)
                .orElseThrow(() -> new IllegalArgumentException("Discussion not found"));
    }

    @Transactional(readOnly = true)
    public List<DiscussionParticipant> getParticipants(Long discussionId) {
        return discussionRepository.findActiveParticipantsByDiscussionId(discussionId);
    }
    /**
     * Join a discussion
     */
    @Transactional
    public void joinDiscussion(Long discussionId, String userEmail) {
        Discussion discussion = getDiscussionById(discussionId);

        if (!discussion.isActive()) {
            throw new IllegalStateException("Discussion is closed");
        }

        Users user = userRepository.findByEmailId(userEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // Check if already a participant
        Optional<DiscussionParticipant> existing =
                discussionRepository.findParticipantByDiscussionAndUser(discussionId, userEmail);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Already joined this discussion");
        }

        // Check participant limit
        long participantCount = discussionRepository.countActiveParticipants(discussionId);
        if (participantCount >= MAX_PARTICIPANTS) {
            throw new IllegalStateException("Discussion has reached maximum participant limit");
        }

        // Add participant
        DiscussionParticipant participant = new DiscussionParticipant(discussion, user, false);
        entityManager.persist(participant);

        // Update last activity
        discussion.setLastActivity(LocalDateTime.now());
        discussionRepository.save(discussion);

        entityManager.flush();

        log.info("User {} joined discussion {}", userEmail, discussionId);
    }

    /**
     * Leave a discussion
     */
    @Transactional
    public void leaveDiscussion(Long discussionId, String userEmail) {
        DiscussionParticipant participant = discussionRepository
                .findParticipantByDiscussionAndUser(discussionId, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Not a participant in this discussion"));

        // Prevent host from leaving
        if (participant.isHost()) {
            throw new IllegalStateException("Host cannot leave the discussion. Please close or delete the discussion instead.");
        }

        participant.setLeftAt(LocalDateTime.now());
        participant.setOnline(false);
        entityManager.merge(participant);
        entityManager.flush();

        log.info("User {} left discussion {}", userEmail, discussionId);
    }

    /**
     * Get messages in a discussion with pagination
     */
    public List<DiscussionMessage> getMessages(Long discussionId, Integer limit, Long before, Long after) {
        if (limit == null) {
            limit = 50;
        }
        if (limit > 200) {
            limit = 200;
        }

        List<DiscussionMessage> messages;

        if (before != null) {
            messages = discussionRepository.findMessagesBeforeId(discussionId, before);
        } else if (after != null) {
            messages = discussionRepository.findMessagesAfterId(discussionId, after);
        } else {
            messages = discussionRepository.findMessagesByDiscussionId(discussionId);
        }

        // Apply limit
        return messages.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Save a message (called from WebSocket handler)
     */
    @Transactional
    public DiscussionMessage saveMessage(Long discussionId, String senderEmail,
                                         String content, MessageType messageType,
                                         String recipientEmail) {
        Discussion discussion = getDiscussionById(discussionId);
        Users sender = userRepository.findByEmailId(senderEmail);

        if (sender == null) {
            throw new IllegalArgumentException("Sender not found");
        }

        // Validate sender is a participant
        Optional<DiscussionParticipant> participant =
                discussionRepository.findParticipantByDiscussionAndUser(discussionId, senderEmail);
        if (participant.isEmpty()) {
            throw new IllegalArgumentException("User is not a participant in this discussion");
        }

        // Validate content length
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message exceeds maximum length of " + MAX_MESSAGE_LENGTH);
        }

        // Create message
        DiscussionMessage message = new DiscussionMessage(discussion, sender, content, messageType);
        if (messageType == MessageType.DIRECT_MESSAGE && recipientEmail != null) {
            message.setRecipientEmail(recipientEmail);
        }
        entityManager.persist(message);

        // Update discussion last activity
        discussion.setLastActivity(LocalDateTime.now());
        discussionRepository.save(discussion);

        // Update participant last seen
        DiscussionParticipant p = participant.get();
        p.setLastSeen(LocalDateTime.now());
        entityManager.merge(p);

        entityManager.flush();

        log.info("Message {} saved in discussion {} by user {}",
                message.getId(), discussionId, senderEmail);

        return message;
    }

    /**
     * Close a discussion (only host can close)
     */
    @Transactional
    public void closeDiscussion(Long discussionId, String userEmail) {
        Discussion discussion = getDiscussionById(discussionId);

        // Check if user is host
        Boolean isHostResult = discussionRepository.isUserHostOfDiscussion(discussionId, userEmail);
        boolean isHost = isHostResult != null && isHostResult;

        if (!isHost) {
            throw new IllegalArgumentException("Only the host can close this discussion");
        }

        if (!discussion.isActive()) {
            throw new IllegalStateException("Discussion already closed");
        }

        discussion.setActive(false);
        discussion.setClosedAt(LocalDateTime.now());
        discussionRepository.save(discussion);

        log.info("Discussion {} closed by host {}", discussionId, userEmail);
    }

    /**
     * Delete a discussion (only host can delete)
     */
    @Transactional
    public void deleteDiscussion(Long discussionId, String userEmail) {
        Discussion discussion = getDiscussionById(discussionId);

        // Check if user is host
        Boolean isHostResult = discussionRepository.isUserHostOfDiscussion(discussionId, userEmail);
        boolean isHost = isHostResult != null && isHostResult;

        if (!isHost) {
            throw new IllegalArgumentException("Only the host can delete this discussion");
        }

        // Delete all messages
        List<DiscussionMessage> messages = discussionRepository.findMessagesByDiscussionId(discussionId);
        for (DiscussionMessage msg : messages) {
            entityManager.remove(msg);
        }

        // Delete all participants
        List<DiscussionParticipant> participants = discussionRepository.findAllParticipantsByDiscussionId(discussionId);
        for (DiscussionParticipant p : participants) {
            entityManager.remove(p);
        }

        // Delete discussion
        discussionRepository.delete(discussion);
        entityManager.flush();

        log.info("Discussion {} deleted by host {}", discussionId, userEmail);
    }

    /**
     * Get discussions by user (hosted or participating)
     */
    public Map<String, List<Discussion>> getUserDiscussions(String userEmail, String role, Boolean active) {
        Map<String, List<Discussion>> result = new HashMap<>();

        if (role == null || role.equals("HOST") || role.equals("ALL")) {
            List<Discussion> hosted = discussionRepository.findByHostUserEmail(userEmail);
            if (active != null) {
                hosted = hosted.stream()
                        .filter(d -> d.isActive() == active)
                        .collect(Collectors.toList());
            }
            result.put("hosted", hosted);
        }

        if (role == null || role.equals("PARTICIPANT") || role.equals("ALL")) {
            List<DiscussionParticipant> participants = discussionRepository.findActiveParticipantsByUserEmail(userEmail);
            List<Discussion> participating = participants.stream()
                    .map(DiscussionParticipant::getDiscussion)
                    .filter(d -> !d.getHostUser().getEmailId().equals(userEmail)) // Exclude hosted
                    .collect(Collectors.toList());

            if (active != null) {
                participating = participating.stream()
                        .filter(d -> d.isActive() == active)
                        .collect(Collectors.toList());
            }
            result.put("participating", participating);
        }

        return result;
    }

    /**
     * Search discussions
     */
    public List<Discussion> searchDiscussions(String query, DiscussionType type, String tags) {
        List<Discussion> results = discussionRepository.searchDiscussions(query);

        if (type != null) {
            results = results.stream()
                    .filter(d -> d.getType() == type)
                    .collect(Collectors.toList());
        }

        if (tags != null && !tags.isEmpty()) {
            String[] tagArray = tags.split(",");
            Set<String> searchTags = new HashSet<>(Arrays.asList(tagArray));
            results = results.stream()
                    .filter(d -> d.getTags().stream().anyMatch(searchTags::contains))
                    .collect(Collectors.toList());
        }

        return results;
    }

    /**
     * Update participant online status
     */
    @Transactional
    public void updateParticipantStatus(Long discussionId, String userEmail, boolean online) {
        Optional<DiscussionParticipant> participantOpt =
                discussionRepository.findParticipantByDiscussionAndUser(discussionId, userEmail);

        if (participantOpt.isPresent()) {
            DiscussionParticipant participant = participantOpt.get();
            participant.setOnline(online);
            participant.setLastSeen(LocalDateTime.now());
            entityManager.merge(participant);
            entityManager.flush();
        }
    }

    /**
     * Get participant count for a discussion
     */
    public long getParticipantCount(Long discussionId) {
        return discussionRepository.countActiveParticipants(discussionId);
    }

    /**
     * Get message count for a discussion
     */
    public long getMessageCount(Long discussionId) {
        return discussionRepository.countMessagesByDiscussionId(discussionId);
    }

    /**
     * Check if user is participant
     */
    public boolean isParticipant(Long discussionId, String userEmail) {
        return discussionRepository.findParticipantByDiscussionAndUser(discussionId, userEmail).isPresent();
    }

    /**
     * Check if user is host
     */
    public boolean isHost(Long discussionId, String userEmail) {
        Boolean result = discussionRepository.isUserHostOfDiscussion(discussionId, userEmail);
        return result != null && result;
    }

    /**
     * Cleanup stale online statuses (called periodically)
     */
    @Transactional
    public void cleanupStaleOnlineStatuses() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<DiscussionParticipant> staleParticipants =
                discussionRepository.findStaleOnlineParticipants(threshold);

        for (DiscussionParticipant participant : staleParticipants) {
            participant.setOnline(false);
            entityManager.merge(participant);
        }

        if (!staleParticipants.isEmpty()) {
            entityManager.flush();
            log.info("Marked {} participants as offline due to inactivity", staleParticipants.size());
        }
    }
}