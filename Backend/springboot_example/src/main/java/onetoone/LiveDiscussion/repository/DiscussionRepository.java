package onetoone.LiveDiscussion.repository;

import onetoone.LiveDiscussion.model.Discussion;
import onetoone.LiveDiscussion.model.DiscussionParticipant;
import onetoone.LiveDiscussion.model.DiscussionMessage;
import onetoone.LiveDiscussion.model.DiscussionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Unified repository for all Discussion-related entities
 * Handles Discussion, DiscussionParticipant, and DiscussionMessage queries
 */
@Repository
public interface DiscussionRepository extends JpaRepository<Discussion, Long> {

    // ==================== DISCUSSION QUERIES ====================

    // Find discussion by poll ID (one-to-one relationship)
    @Query("SELECT d FROM Discussion d WHERE d.poll.id = :pollId")
    Optional<Discussion> findByPollId(@Param("pollId") Long pollId);

    // Add this new method
    @Query("SELECT d FROM Discussion d " +
            "LEFT JOIN FETCH d.poll " +
            "LEFT JOIN FETCH d.hostUser " +
            "WHERE d.id = :discussionId")
    Optional<Discussion> findByIdWithDetails(@Param("discussionId") Long discussionId);

    // Update existing method
    @Query("SELECT dp FROM DiscussionParticipant dp " +
            "JOIN FETCH dp.user " +
            "WHERE dp.discussion.id = :discussionId AND dp.leftAt IS NULL")
    List<DiscussionParticipant> findActiveParticipantsByDiscussionId(@Param("discussionId") Long discussionId);

    // Find discussions by type
    List<Discussion> findByType(DiscussionType type);

    // Find discussions by active status
    List<Discussion> findByActive(boolean active);

    // Find by type and active status
    List<Discussion> findByTypeAndActive(DiscussionType type, boolean active);

    // Find discussions hosted by a specific user
    @Query("SELECT d FROM Discussion d WHERE d.hostUser.emailId = :hostEmail")
    List<Discussion> findByHostUserEmail(@Param("hostEmail") String hostEmail);

    // Search discussions by title or description
    @Query("SELECT d FROM Discussion d WHERE LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Discussion> searchDiscussions(@Param("query") String query);

    // Find discussions containing a specific tag
    @Query("SELECT d FROM Discussion d JOIN d.tags t WHERE t = :tag")
    List<Discussion> findByTag(@Param("tag") String tag);

    // Find inactive discussions for cleanup
    @Query("SELECT d FROM Discussion d WHERE d.active = true AND d.lastActivity < :cutoffDate")
    List<Discussion> findInactiveDiscussions(@Param("cutoffDate") LocalDateTime cutoffDate);

    // ==================== PARTICIPANT QUERIES ====================

    // Find participant by discussion ID and user email
    @Query("SELECT dp FROM DiscussionParticipant dp WHERE dp.discussion.id = :discussionId " +
            "AND dp.user.emailId = :userEmail AND dp.leftAt IS NULL")
    Optional<DiscussionParticipant> findParticipantByDiscussionAndUser(
            @Param("discussionId") Long discussionId,
            @Param("userEmail") String userEmail);

    // Find all participants (including those who left)
    @Query("SELECT dp FROM DiscussionParticipant dp WHERE dp.discussion.id = :discussionId")
    List<DiscussionParticipant> findAllParticipantsByDiscussionId(@Param("discussionId") Long discussionId);

    // Count active participants in a discussion
    @Query("SELECT COUNT(dp) FROM DiscussionParticipant dp WHERE dp.discussion.id = :discussionId " +
            "AND dp.leftAt IS NULL")
    long countActiveParticipants(@Param("discussionId") Long discussionId);

    // Find all active discussions a user is participating in
    @Query("SELECT dp FROM DiscussionParticipant dp WHERE dp.user.emailId = :userEmail " +
            "AND dp.leftAt IS NULL")
    List<DiscussionParticipant> findActiveParticipantsByUserEmail(@Param("userEmail") String userEmail);

    // Check if user is host of a discussion
    @Query("SELECT dp.isHost FROM DiscussionParticipant dp WHERE dp.discussion.id = :discussionId " +
            "AND dp.user.emailId = :userEmail AND dp.leftAt IS NULL")
    Boolean isUserHostOfDiscussion(@Param("discussionId") Long discussionId, @Param("userEmail") String userEmail);

    // Find stale online participants (for cleanup)
    @Query("SELECT dp FROM DiscussionParticipant dp WHERE dp.isOnline = true " +
            "AND dp.lastSeen < :threshold")
    List<DiscussionParticipant> findStaleOnlineParticipants(@Param("threshold") LocalDateTime threshold);

    // Count online participants in a discussion
    @Query("SELECT COUNT(dp) FROM DiscussionParticipant dp WHERE dp.discussion.id = :discussionId " +
            "AND dp.isOnline = true AND dp.leftAt IS NULL")
    long countOnlineParticipants(@Param("discussionId") Long discussionId);

    // Save a participant
    @Query(value = "INSERT INTO discussion_participants (discussion_id, user_email, joined_at, " +
            "left_at, is_host, last_seen, is_online) VALUES (:discussionId, :userEmail, :joinedAt, " +
            ":leftAt, :isHost, :lastSeen, :isOnline)", nativeQuery = true)
    void saveParticipant(@Param("discussionId") Long discussionId,
                         @Param("userEmail") String userEmail,
                         @Param("joinedAt") LocalDateTime joinedAt,
                         @Param("leftAt") LocalDateTime leftAt,
                         @Param("isHost") boolean isHost,
                         @Param("lastSeen") LocalDateTime lastSeen,
                         @Param("isOnline") boolean isOnline);

    // Update participant status
    @Query("UPDATE DiscussionParticipant dp SET dp.isOnline = :isOnline, dp.lastSeen = :lastSeen " +
            "WHERE dp.discussion.id = :discussionId AND dp.user.emailId = :userEmail")
    void updateParticipantStatus(@Param("discussionId") Long discussionId,
                                 @Param("userEmail") String userEmail,
                                 @Param("isOnline") boolean isOnline,
                                 @Param("lastSeen") LocalDateTime lastSeen);

    // ==================== MESSAGE QUERIES ====================

    // Find messages by discussion ID, ordered by timestamp descending
    @Query("SELECT m FROM DiscussionMessage m WHERE m.discussion.id = :discussionId " +
            "ORDER BY m.timestamp DESC")
    List<DiscussionMessage> findMessagesByDiscussionId(@Param("discussionId") Long discussionId);

    // Find messages before a specific message ID (pagination)
    @Query("SELECT m FROM DiscussionMessage m WHERE m.discussion.id = :discussionId " +
            "AND m.id < :beforeId ORDER BY m.timestamp DESC")
    List<DiscussionMessage> findMessagesBeforeId(
            @Param("discussionId") Long discussionId,
            @Param("beforeId") Long beforeId);

    // Find messages after a specific message ID (pagination)
    @Query("SELECT m FROM DiscussionMessage m WHERE m.discussion.id = :discussionId " +
            "AND m.id > :afterId ORDER BY m.timestamp ASC")
    List<DiscussionMessage> findMessagesAfterId(
            @Param("discussionId") Long discussionId,
            @Param("afterId") Long afterId);

    // Count total messages in a discussion
    @Query("SELECT COUNT(m) FROM DiscussionMessage m WHERE m.discussion.id = :discussionId")
    long countMessagesByDiscussionId(@Param("discussionId") Long discussionId);

    // Find direct messages between two users in a discussion
    @Query("SELECT m FROM DiscussionMessage m WHERE m.discussion.id = :discussionId " +
            "AND m.messageType = 'DIRECT_MESSAGE' " +
            "AND ((m.sender.emailId = :user1Email AND m.recipientEmail = :user2Email) " +
            "OR (m.sender.emailId = :user2Email AND m.recipientEmail = :user1Email)) " +
            "ORDER BY m.timestamp DESC")
    List<DiscussionMessage> findDirectMessagesBetween(
            @Param("discussionId") Long discussionId,
            @Param("user1Email") String user1Email,
            @Param("user2Email") String user2Email);

    // Delete all messages in a discussion
    @Query("DELETE FROM DiscussionMessage m WHERE m.discussion.id = :discussionId")
    void deleteAllMessagesInDiscussion(@Param("discussionId") Long discussionId);

    // Save a message (using native query for insert)
    @Query(value = "INSERT INTO discussion_messages (discussion_id, sender_email, content, " +
            "message_type, recipient_email, timestamp, edited, edited_at) " +
            "VALUES (:discussionId, :senderEmail, :content, :messageType, :recipientEmail, " +
            ":timestamp, :edited, :editedAt)", nativeQuery = true)
    void saveMessage(@Param("discussionId") Long discussionId,
                     @Param("senderEmail") String senderEmail,
                     @Param("content") String content,
                     @Param("messageType") String messageType,
                     @Param("recipientEmail") String recipientEmail,
                     @Param("timestamp") LocalDateTime timestamp,
                     @Param("edited") boolean edited,
                     @Param("editedAt") LocalDateTime editedAt);
}