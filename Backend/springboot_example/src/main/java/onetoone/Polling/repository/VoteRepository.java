package onetoone.Polling.repository;

import onetoone.Polling.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    // Generic counts
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.poll.id = :pollId")
    long countTotalByPoll(@Param("pollId") Long pollId);

    // Yes/No using numericValue = 1 (yes) or 0 (no)
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.poll.id = :pollId AND v.numericValue = 1")
    long countYes(@Param("pollId") Long pollId);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.poll.id = :pollId AND v.numericValue = 0")
    long countNo(@Param("pollId") Long pollId);

    // Rating (numericValue)
    @Query("SELECT AVG(v.numericValue) FROM Vote v WHERE v.poll.id = :pollId")
    Double averageNumeric(@Param("pollId") Long pollId);

    @Query("SELECT MAX(v.numericValue) FROM Vote v WHERE v.poll.id = :pollId")
    Integer maxNumeric(@Param("pollId") Long pollId);

    // Multiple choice: returns rows of (choice, count)
    @Query("SELECT v.choice, COUNT(v) FROM Vote v WHERE v.poll.id = :pollId GROUP BY v.choice")
    List<Object[]> countChoices(@Param("pollId") Long pollId);

    // Ranking: Borda-like scoring: SUM(totalOptions - v.rank + 1) grouped by choice
    // Note: JPQL supports arithmetic expressions with parameters in SUM
    @Query("SELECT v.choice, SUM(:totalOptions - v.rank + 1) FROM Vote v WHERE v.poll.id = :pollId GROUP BY v.choice ORDER BY SUM(:totalOptions - v.rank + 1) DESC")
    List<Object[]> rankingScores(@Param("pollId") Long pollId, @Param("totalOptions") int totalOptions);

    // find a single user's vote for a poll
    @Query("SELECT v FROM Vote v WHERE v.poll.id = :pollId AND LOWER(v.userId) = LOWER(:userId)")
    Vote findByPollAndUser(@Param("pollId") Long pollId, @Param("userId") String userId);

    // optional: find all votes for a poll (careful for large polls)
    @Query("SELECT v FROM Vote v WHERE v.poll.id = :pollId")
    List<Vote> findAllByPoll(@Param("pollId") Long pollId);

    // Single-vote method (existing)
    @Query("SELECT v FROM Vote v WHERE v.poll.id = :pollId AND LOWER(v.userId) = LOWER(:userId)")
    Vote findSingleByPollAndUser(@Param("pollId") Long pollId, @Param("userId") String userId);

    // Multi-vote method for ranking polls
    @Query("SELECT v FROM Vote v WHERE v.poll.id = :pollId AND LOWER(v.userId) = LOWER(:userId)")
    List<Vote> findAllByPollAndUser(@Param("pollId") Long pollId, @Param("userId") String userId);

    // rating polls: count votes numerically
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.poll.id = :pollId AND v.numericValue = :rating")
    long countByNumericValue(@Param("pollId") Long pollId, @Param("rating") int rating);

    // ranking polls: count distinct users who voted
    @Query("SELECT COUNT(DISTINCT v.userId) FROM Vote v WHERE v.poll.id = :pollId")
    long countDistinctUsersByPoll(@Param("pollId") Long pollId);

    // total votes casted by a user
    @Query("SELECT COUNT(v) FROM Vote v WHERE LOWER(v.userId) = LOWER(:userId)")
    long countTotalByUser(@Param("userId") String userId);

    // last 5 votes from a user
    List<Vote> findTop5ByUserIdOrderByTimestampDesc(String userId);

}