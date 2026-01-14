package onetoone.Polling.repository;

import onetoone.Polling.model.PollModerator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PollModeratorRepository extends JpaRepository<PollModerator, Long> {

    // find all moderators for a poll
    @Query("SELECT pm FROM PollModerator pm WHERE pm.poll.id = :pollId")
    List<PollModerator> findByPollId(@Param("pollId") Long pollId);

    // find all currently active moderators for a poll
    @Query("SELECT pm FROM PollModerator pm WHERE pm.poll.id = :pollId " +
            "AND pm.isActive = true " +
            "AND (pm.expiresAt IS NULL OR pm.expiresAt > CURRENT_TIMESTAMP)")
    List<PollModerator> findActiveModerators(@Param("pollId") Long pollId);

    // find a specific moderator by poll and user email
    @Query("SELECT pm FROM PollModerator pm WHERE pm.poll.id = :pollId " +
            "AND pm.moderator.emailId = :moderatorEmail")
    Optional<PollModerator> findByPollAndModerator(
            @Param("pollId") Long pollId,
            @Param("moderatorEmail") String moderatorEmail
    );

    // check if a user is an active moderator for a poll
    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END " +
            "FROM PollModerator pm WHERE pm.poll.id = :pollId " +
            "AND pm.moderator.emailId = :moderatorEmail " +
            "AND pm.isActive = true " +
            "AND (pm.expiresAt IS NULL OR pm.expiresAt > CURRENT_TIMESTAMP)")
    boolean isActiveModerator(
            @Param("pollId") Long pollId,
            @Param("moderatorEmail") String moderatorEmail
    );

    // find all polls where a use is a moderator
    @Query("SELECT pm FROM PollModerator pm WHERE pm.moderator.emailId = :moderatorEmail " +
            "AND pm.isActive = true " +
            "AND (pm.expiresAt IS NULL OR pm.expiresAt > CURRENT_TIMESTAMP)")
    List<PollModerator> findPollsModeratedByUser(@Param("moderatorEmail") String moderatorEmail);

    // delete all moderators for a poll
    @Modifying
    @Query("DELETE FROM PollModerator pm WHERE pm.poll.id = :pollId")
    void deleteByPollId(@Param("pollId") Long pollId);

}
