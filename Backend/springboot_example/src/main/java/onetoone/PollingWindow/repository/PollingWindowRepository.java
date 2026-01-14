package onetoone.PollingWindow.repository;

import onetoone.PollingWindow.model.PollingWindow;
import onetoone.Users.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PollingWindowRepository extends JpaRepository<PollingWindow, Long> {

    // Find all windows for a specific poll
    List<PollingWindow> findByPollId(Long pollId);

    // Find active windows for a specific poll
    @Query("SELECT pw FROM PollingWindow pw WHERE pw.poll.id = :pollId AND pw.isActive = true")
    List<PollingWindow> findActiveWindowsByPollId(@Param("pollId") Long pollId);

    // Find the currently active window for a poll (should be only one at a time)
    @Query("SELECT pw FROM PollingWindow pw WHERE pw.poll.id = :pollId AND pw.isActive = true")
    Optional<PollingWindow> findCurrentActiveWindow(@Param("pollId") Long pollId);

    // Find windows created by a specific user (pass Users entity)
    List<PollingWindow> findByCreator(Users creator);

    // Find windows by creator email (pass String email)
    @Query("SELECT pw FROM PollingWindow pw WHERE pw.creator.emailId = :creatorEmail")
    List<PollingWindow> findByCreatorEmail(@Param("creatorEmail") String creatorEmail);
}