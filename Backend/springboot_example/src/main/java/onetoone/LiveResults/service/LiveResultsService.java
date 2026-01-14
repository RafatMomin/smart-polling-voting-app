package onetoone.LiveResults.service;

import onetoone.LiveResults.dto.LiveResultsMessage;
import onetoone.LiveResults.dto.LiveResultsMessage.OptionDistribution;
import onetoone.Polling.model.*;

import onetoone.Polling.repository.PollRepository;
import onetoone.Polling.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LiveResultsService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;


    // broadcast updated results to all subscribers, called after vote is cast or deleted
    public void broadcastResults(Long pollId) {
        LiveResultsMessage message = calculatePollResults(pollId);

        // sends to WebSocket topic AKA all subscribers to this poll will get it
        messagingTemplate.convertAndSend("/topic/poll/" + pollId, message);
    }

    // calculates results for poll based on its poll type
    private LiveResultsMessage calculatePollResults(Long pollId) {

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new NoSuchElementException("Poll not found"));

        String status = poll.isActive() ? "active" : "closed";
        String pollType = poll.getType();

        // based on poll type
        List<OptionDistribution> distribution;
        Integer totalVotes;

        switch (pollType) {
            case "YES_NO":
                return buildYesNoResults(pollId, status);

            case "RATING":
                return buildRatingResults(pollId, status);

            case "MULTIPLE_CHOICE":
                MultipleChoicePoll mcp = (MultipleChoicePoll) poll;
                return buildMultipleChoiceResults(pollId, status, mcp.getOptions());

            case "RANKING":
                RankingPoll rp = (RankingPoll) poll;
                return buildRankingResults(pollId, status, rp.getOptions().size());

            default:
                throw new IllegalStateException("Unknown poll type: " + pollType);
        }
    }

    // builds results for Yes No polls (option 0 = No, option 1 = Yes)
    private LiveResultsMessage buildYesNoResults(Long pollId, String status) {
        long yesCount = voteRepository.countYes(pollId);
        long noCount = voteRepository.countNo(pollId);
        long total = yesCount + noCount;

        List<OptionDistribution> distribution = new ArrayList<>();

        // 0 = No
        double noPercent = total > 0 ? (noCount * 100.0 / total) : 0.0;
        distribution.add(new OptionDistribution(0L, (int) noCount, noPercent));

        // 1 = Yes
        double yesPercent = total > 0 ? (yesCount * 100.0 / total) : 0.0;
        distribution.add(new OptionDistribution(1L, (int) yesCount, yesPercent));

        return new LiveResultsMessage(pollId, status, (int) total, distribution);
    }


    // builds results for Rating polls (options 1-5)
    private LiveResultsMessage buildRatingResults(Long pollId, String status) {
        long total = voteRepository.countTotalByPoll(pollId);
        List<OptionDistribution> distribution = new ArrayList<>();

        // counts votes for each rating 1-5
        for (int rating = 1; rating <= 5; rating++) {
            long count = voteRepository.countByNumericValue(pollId, rating);
            double percent = total > 0 ? (count * 100.0 / total) : 0.0;
            distribution.add(new OptionDistribution((long) rating, (int) count, percent));
        }

        return new LiveResultsMessage(pollId, status, (int) total, distribution);
    }

    // builds results for Multiple Choice polls
    private LiveResultsMessage buildMultipleChoiceResults(Long pollId, String status, List<String> options) {
        long total = voteRepository.countTotalByPoll(pollId);

        // get vote counts
        List<Object[]> voteCounts = voteRepository.countChoices(pollId);
        Map<String, Long> countsMap = new HashMap<>();
        for (Object[] row : voteCounts) {
            String choice = (String) row[0];
            Long count = (Long) row[1];
            countsMap.put(choice, count);
        }

        // calculate distribution for each option
        List<OptionDistribution> distribution = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            long count = countsMap.getOrDefault(option, 0L);
            double percent = total > 0 ? (count * 100.0 / total) : 0.0;

            // use index as optionId for easier frontend identification
            distribution.add(new OptionDistribution((long) i, (int) count, percent));
        }

        return new LiveResultsMessage(pollId, status, (int) total, distribution);
    }

    // builds results for Ranking polls, shows Borda count scores for every option
    private LiveResultsMessage buildRankingResults(Long pollId, String status, int totalOptions) {
        // count distnict users who voted since each user submits multiple votes technically
        long distinctVoters = voteRepository.countDistinctUsersByPoll(pollId);

        // get Borda scores
        List<Object[]> scores = voteRepository.rankingScores(pollId, totalOptions);

        List<OptionDistribution> distribution = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            Object[] row = scores.get(i);
            String choice = (String) row[0];
            Number scoreNumber = (Number) row[1];
            int score;
            if (scoreNumber == null) {
                score = 0;
            } else {
                score = scoreNumber.intValue();
            }


            // ranking: shows the score as "count" and calculate a percentage based on max possible score
            // the max possible score per voter = totalOptions points
            // the max total score = distinctVoters * totalOptions
            long maxPossibleScore = distinctVoters * totalOptions;
            double percent = maxPossibleScore > 0 ? (score * 100.0 / maxPossibleScore) : 0.0;

            distribution.add(new OptionDistribution((long) i, score, percent));
        }

        return new LiveResultsMessage(pollId, status, (int) distinctVoters, distribution);
    }

    // gets results without broadcasting (for testing controller)
    public LiveResultsMessage getResultsSnapshot(Long pollId) {
        return calculatePollResults(pollId);
    }
}