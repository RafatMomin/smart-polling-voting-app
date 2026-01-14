package onetoone.AI.repository;

import onetoone.AI.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Find all messages from a specific user by email
    @Query("SELECT c FROM ChatMessage c WHERE c.user.emailId = :email ORDER BY c.timestamp DESC")
    List<ChatMessage> findByUserEmailOrderByTimestampDesc(@Param("email") String email);

    // Count total messages for a user
    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.user.emailId = :email")
    long countByUserEmail(@Param("email") String email);

    // Find messages within a time range
    List<ChatMessage> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Find messages that referenced a specific poll
    @Query("SELECT c FROM ChatMessage c JOIN c.referencedPolls p WHERE p.id = :pollId ORDER BY c.timestamp DESC")
    List<ChatMessage> findByReferencedPoll(@Param("pollId") Long pollId);

    // Find user's messages that referenced a specific poll
    @Query("SELECT c FROM ChatMessage c JOIN c.referencedPolls p " +
            "WHERE c.user.emailId = :email AND p.id = :pollId ORDER BY c.timestamp DESC")
    List<ChatMessage> findByUserAndPoll(@Param("email") String email, @Param("pollId") Long pollId);

    // Get most recent N messages for a user
    @Query(value = "SELECT c FROM ChatMessage c WHERE c.user.emailId = :email ORDER BY c.timestamp DESC")
    List<ChatMessage> findTopNByUserEmail(@Param("email") String email);
}
