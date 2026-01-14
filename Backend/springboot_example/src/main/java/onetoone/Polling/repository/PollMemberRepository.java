package onetoone.Polling.repository;

import onetoone.Polling.model.PollMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PollMemberRepository extends JpaRepository<PollMember, Long> {

    @Query("SELECT COUNT(pm) > 0 FROM PollMember pm WHERE pm.poll.id = :pollId AND pm.user.emailId = :userEmail")
    boolean existsByPollAndUser(@Param("pollId") Long pollId, @Param("userEmail") String userEmail);

    @Query("SELECT pm FROM PollMember pm WHERE pm.poll.id = :pollId")
    List<PollMember> findAllByPollId(@Param("pollId") Long pollId);

    @Query("SELECT pm FROM PollMember pm WHERE pm.user.emailId = :userEmail")
    List<PollMember> findAllByUserEmail(@Param("userEmail") String userEmail);

    // total polls joined by a user
    @Query("SELECT COUNT(pm) FROM PollMember pm WHERE pm.user.emailId = :userEmail")
    long countByUserEmail(@Param("userEmail") String userEmail);

    // poll types a user has joined
    @Query("SELECT pm.poll.type, COUNT(pm) FROM PollMember pm " +
            "WHERE pm.user.emailId = :userEmail GROUP BY pm.poll.type")
    List<Object[]> countByPollTypeForUser(@Param("userEmail") String userEmail);

    // last 5 polls joined by a user
    List<PollMember> findTop5ByUserEmailIdOrderByJoinedAtDesc(String emailId);

}
