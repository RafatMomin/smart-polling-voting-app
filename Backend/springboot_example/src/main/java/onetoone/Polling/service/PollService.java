package onetoone.Polling.service;

import onetoone.AI.model.SentenceTransformerEmbeddingModel;
import onetoone.LiveResults.service.LiveResultsService;
import onetoone.Polling.model.*;
import onetoone.Polling.repository.PollMemberRepository;
import onetoone.Polling.repository.PollRepository;
import onetoone.PollingWindow.repository.PollingWindowRepository;
import onetoone.PollingWindow.model.PollingWindow;
import onetoone.Polling.repository.VoteRepository;
import onetoone.Users.model.Users;
import onetoone.Users.repository.UserRepository;
import onetoone.Polling.repository.PollModeratorRepository;
import onetoone.Polling.model.PollModerator;
import onetoone.Polling.dto.ModeratorAssignmentDTO;
import onetoone.Users.service.MailingService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.math3.linear.ArrayRealVector;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PollService {

    @Autowired
    private PollRepository pollRepository;
    @Autowired
    private VoteRepository voteRepository;
    @Autowired
    private LiveResultsService liveResultsService;
    @Autowired
    private SentenceTransformerEmbeddingModel embeddingModel;
    @Autowired
    private PollingWindowRepository pollingWindowRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PollMemberRepository pollMemberRepository;

    @Autowired
    private MailingService mailingService;

    @Autowired
    private PollModeratorRepository pollModeratorRepository;

    private static final Logger log = LoggerFactory.getLogger(PollService.class);
    private final Gson gson = new Gson();
    private final Type floatArrayType = new TypeToken<float[]>() {
    }.getType();

    /* ------------------------------------------------------------------ */
    /*  CREATE + EMBED + VISIBILITY                                                   */
    /* ------------------------------------------------------------------ */
    @Transactional
    public Poll createPoll(Poll poll, String ownerEmail) {
        Poll saved = pollRepository.save(poll);
        try {
            String textRep = saved.getTextRepresentation();
            log.info("Creating embedding for poll {}: {}", saved.getId(), textRep);

            float[] vec = embeddingModel.embed(textRep);
            log.info("Generated embedding vector of length: {}", vec.length);

            String embeddingJson = gson.toJson(vec);
            saved.setEmbedding(embeddingJson);
        } catch (Exception e) {
            log.error("Embedding failed for poll {}: {}", saved.getId(), e.getMessage(), e);
            throw new RuntimeException("Embedding failed", e);
        }

        // set admin
        Users owner = null;
        if (ownerEmail != null && !ownerEmail.isEmpty()) {
            Optional<Users> ownerOpt = userRepository.findById(ownerEmail);
            if (ownerOpt.isPresent()) {
                owner = ownerOpt.get();
                saved.setOwner(owner);
            }
        }

        // private poll
        String accessCode = null;
        if (saved.getVisibility() == Poll.Visibility.PRIVATE) {
            accessCode = generateAccessCode();
            saved.setAccessCode(accessCode);
            saved.setCreatedAt(LocalDateTime.now());
        }

        // adds owner as member (Many-to-Many relationship)
        if (saved.getOwner() != null) {
            PollMember ownerMember = new PollMember(saved, saved.getOwner());
            pollMemberRepository.save(ownerMember);
        }

        // send email with access code for private polls
        if (saved.getVisibility() == Poll.Visibility.PRIVATE && accessCode != null && owner != null) {
            try {
                mailingService.sendPollAccessCodeEmail(
                        owner.getEmailId(),
                        saved.getTitle(),
                        accessCode
                );
            } catch (Exception e) {
                System.err.println("Failed to send access code email: " + e.getMessage());
            }
        }
        return pollRepository.save(saved);
    }

    /* ------------------------------------------------------------------ */
    /*  SIMILARITY SEARCH (RAG)                                           */
    /* ------------------------------------------------------------------ */
    @Transactional(readOnly = true)
    public List<Poll> findSimilarPolls(float[] queryVector, int topK) {
        if (queryVector == null || queryVector.length == 0) {
            log.warn("findSimilarPolls: queryVector is null or empty");
            return Collections.emptyList();
        }

        List<Poll> all = pollRepository.findAll();

        all.forEach(poll -> {
            poll.getContext();
            poll.getTitle();
            poll.getType();
        });

        log.info("🔍 Searching {} polls with query vector length: {}", all.size(), queryVector.length);

        List<ScoredPoll> scored = new ArrayList<>();
        int skippedNoEmbedding = 0;
        int skippedDeserializationError = 0;

        for (Poll p : all) {
            if (p.getEmbedding() == null || p.getEmbedding().trim().isEmpty()) {
                log.debug("Skipping poll {} - no embedding", p.getId());
                skippedNoEmbedding++;
                continue;
            }

            try {
                float[] vec = gson.fromJson(p.getEmbedding(), floatArrayType);

                if (vec == null || vec.length == 0) {
                    log.warn("Poll {} has null or empty embedding after deserialization", p.getId());
                    skippedDeserializationError++;
                    continue;
                }

                if (vec.length != queryVector.length) {
                    log.warn("❌ Poll {} dimension mismatch: {} vs {}",
                            p.getId(), vec.length, queryVector.length);
                    skippedDeserializationError++;
                    continue;
                }

                double sim = cosineSimilarity(queryVector, vec);
                scored.add(new ScoredPoll(p, sim));

                log.info("Poll #{}: '{}' - Similarity: {}", p.getId(), p.getTitle(), String.format("%.4f", sim));

            } catch (Exception e) {
                log.error("Failed to process embedding for poll {}: {}", p.getId(), e.getMessage());
                skippedDeserializationError++;
                continue;
            }
        }

        log.info("⚠️ Skipped: {} (no embedding), {} (errors)", skippedNoEmbedding, skippedDeserializationError);

        List<Poll> topResults = scored.stream()
                .sorted(Comparator.comparingDouble(s -> -s.score))
                .limit(topK)
                .map(s -> s.poll)
                .collect(Collectors.toList());

        log.info("📊 Top {} results:", topK);
        scored.stream()
                .sorted(Comparator.comparingDouble(s -> -s.score))
                .limit(topK)
                .forEach(sp -> log.info("  → Poll #{}: '{}' (score: {})",
                        sp.poll.getId(), sp.poll.getTitle(), String.format("%.4f", sp.score)));

        return topResults;
    }

    private static class ScoredPoll {
        final Poll poll;
        final double score;
        ScoredPoll(Poll p, double s) { this.poll = p; this.score = s; }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null) {
            log.warn("cosineSimilarity: null vector(s) - a={}, b={}",
                    a == null ? "null" : "not null",
                    b == null ? "null" : "not null");
            return 0.0;
        }

        if (a.length == 0 || b.length == 0) {
            log.warn("cosineSimilarity: empty vector(s) - a.length={}, b.length={}",
                    a.length, b.length);
            return 0.0;
        }

        if (a.length != b.length) {
            log.warn("cosineSimilarity: dimension mismatch - a.length={}, b.length={}",
                    a.length, b.length);
            return 0.0;
        }

        try {
            ArrayRealVector v1 = new ArrayRealVector(a.length);
            ArrayRealVector v2 = new ArrayRealVector(b.length);
            for (int i = 0; i < a.length; i++) {
                v1.setEntry(i, a[i]);
                v2.setEntry(i, b[i]);
            }
            return v1.cosine(v2);
        } catch (Exception e) {
            log.error("cosineSimilarity calculation failed: {}", e.getMessage());
            return 0.0;
        }
    }

    public Optional<Poll> findPoll(Long id) {
        return pollRepository.findById(id);
    }

    /* ------------------------------------------------------------------ */
    /*  VOTING WITH POLLING WINDOW VALIDATION                             */
    /* ------------------------------------------------------------------ */
    @Transactional
    public void addVote(Long pollId, Vote vote) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new NoSuchElementException("Poll not found"));

        if (!poll.isActive()) {
            throw new IllegalStateException("Poll inactive");
        }

        // check if poll is private and user is a member
        if (poll.getVisibility() == Poll.Visibility.PRIVATE) {
            if (!isMember(pollId, vote.getUserId()) && !isOwner(pollId, vote.getUserId())) {
                throw new IllegalArgumentException("Access denied. Must be a member to vote on private polls.");
            }
        }

        // prevent duplicate voting by same user
        // NEW: Check if there's an active polling window and if user can vote
        List<PollingWindow> activeWindows = pollingWindowRepository.findActiveWindowsByPollId(pollId);
        if (!activeWindows.isEmpty()) {
            boolean canVote = false;
            for (PollingWindow window : activeWindows) {
                if (window.canUserVote(vote.getUserId())) {
                    canVote = true;
                    break;
                }
            }
            if (!canVote) {
                throw new IllegalStateException("User is not authorized to vote in the current polling window");
            }
        }

        // Prevent duplicate voting by same user
        Vote existing = voteRepository.findByPollAndUser(pollId, vote.getUserId());
        if (existing != null) {
            throw new IllegalArgumentException("User already voted");
        }

        // Validate according to poll logic
        poll.validateVote(vote);

        // Link and persist
        vote.setPoll(poll);
        voteRepository.save(vote);

        // Broadcast updated vote result
        liveResultsService.broadcastResults(pollId);
    }

    public Vote getUserVote(Long pollId, String userId) {
        return voteRepository.findByPollAndUser(pollId, userId);
    }


    // Stats methods using SQL
    public Map<String, Object> getYesNoStats(Long pollId) {
        long yes = voteRepository.countYes(pollId);
        long no = voteRepository.countNo(pollId);
        long total = yes + no;
        double yesPct = (total == 0) ? 0.0 : (yes * 100.0 / total);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("yes", yes);
        m.put("no", no);
        m.put("total", total);
        m.put("yesPercent", yesPct);
        return m;
    }

    public Map<String, Object> getRatingStats(Long pollId) {
        Double avg = voteRepository.averageNumeric(pollId);
        Integer max = voteRepository.maxNumeric(pollId);
        long total = voteRepository.countTotalByPoll(pollId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalVotes", total);
        m.put("average", avg == null ? 0.0 : avg);
        m.put("max", max == null ? 0 : max);
        return m;
    }

    public Map<String, Object> getMultipleChoiceStats(Long pollId, List<String> options) {
        List<Object[]> rows = voteRepository.countChoices(pollId);
        long total = voteRepository.countTotalByPoll(pollId);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String opt : options) counts.put(opt, 0L);
        for (Object[] r : rows) {
            String opt = (String) r[0];
            Long cnt = (Long) r[1];
            counts.put(opt, cnt);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalVotes", total);
        List<Map<String,Object>> breakdown = new ArrayList<>();
        for (String opt : options) {
            long cnt = counts.getOrDefault(opt, 0L);
            double pct = total == 0 ? 0.0 : (cnt * 100.0 / total);
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("option", opt);
            item.put("count", cnt);
            item.put("percent", pct);
            breakdown.add(item);
        }
        m.put("breakdown", breakdown);
        return m;
    }

    @Transactional
    public boolean deleteUserVote(Long pollId, String userId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new NoSuchElementException("Poll not found"));

        Vote existingVote = voteRepository.findByPollAndUser(pollId, userId);
        if (existingVote == null) {
            return false;
        }

        voteRepository.delete(existingVote);
        return true;
    }

    @Transactional
    public void addRankingVotes(Long pollId, List<Vote> votes) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new NoSuchElementException("Poll not found"));

        if (!poll.isActive()) {
            throw new IllegalStateException("Poll inactive");
        }

        String userId = votes.get(0).getUserId();

        // NEW: Check polling window authorization
        List<PollingWindow> activeWindows = pollingWindowRepository.findActiveWindowsByPollId(pollId);
        if (!activeWindows.isEmpty()) {
            boolean canVote = false;
            for (PollingWindow window : activeWindows) {
                if (window.canUserVote(userId)) {
                    canVote = true;
                    break;
                }
            }
            if (!canVote) {
                throw new IllegalStateException("User is not authorized to vote in the current polling window");
            }
        }

        // check if poll is private and user is a member
        if (poll.getVisibility() == Poll.Visibility.PRIVATE) {
            if (!isMember(pollId, userId) && !isOwner(pollId, userId)) {
                throw new IllegalArgumentException("Access denied. Must be a member to vote on private polls.");
            }
        }

        Vote existing = voteRepository.findByPollAndUser(pollId, userId);
        if (existing != null) {
            throw new IllegalArgumentException("User already voted");
        }

        for (Vote v : votes) {
            poll.validateVote(v);
            v.setPoll(poll);
        }

        voteRepository.saveAll(votes);
        liveResultsService.broadcastResults(pollId);
    }

    public List<Vote> getRankingVotesForUser(Long pollId, String userId) {
        return voteRepository.findAllByPollAndUser(pollId, userId);
    }

    public Map<String, Object> getRankingStats(Long pollId, int totalOptions) {
        List<Object[]> rows = voteRepository.rankingScores(pollId, totalOptions);
        long total = voteRepository.countTotalByPoll(pollId);
        List<Map<String,Object>> scores = new ArrayList<>();
        for (Object[] r : rows) {
            String choice = (String) r[0];
            Number scoreNumber = (Number) r[1];
            long score = scoreNumber == null ? 0L : scoreNumber.longValue();
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("choice", choice);
            item.put("score", score);
            scores.add(item);
        }
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("totalVotes", total);
        m.put("scores", scores);
        return m;
    }

    public List<Vote> getVotesForUser(Long pollId, String userId, boolean isRanking) {
        if (isRanking) {
            return voteRepository.findAllByPollAndUser(pollId, userId);
        } else {
            Vote vote = voteRepository.findSingleByPollAndUser(pollId, userId);
            return vote == null ? Collections.emptyList() : List.of(vote);
        }
    }

    @Transactional
    public boolean deleteUserVote(Long pollId, String userId, boolean isRanking) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new NoSuchElementException("Poll not found"));

        List<Vote> votes = getVotesForUser(pollId, userId, isRanking);
        if (votes.isEmpty()) return false;

        voteRepository.deleteAll(votes);
        liveResultsService.broadcastResults(pollId);

        return true;
    }

    // generate private poll access code
    private String generateAccessCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        int length = 6 + random.nextInt(3);
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        // no repeats
        String generatedCode = code.toString();
        Optional<Poll> existing = pollRepository.findAll().stream()
                .filter(p -> generatedCode.equals(p.getAccessCode()))
                .findFirst();
        if (existing.isPresent()) {
            return generateAccessCode(); // recursively generate if there is collision
        }
        return generatedCode;
    }

    // join poll with the access code
    public Poll joinPoll(String code, String userEmail) {
        // finds poll by access code
        Optional<Poll> pollOpt = pollRepository.findAll().stream()
                .filter(p -> p.getVisibility() == Poll.Visibility.PRIVATE
                        && code != null
                        && code.equals(p.getAccessCode()))
                .findFirst();

        if (pollOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid access code.");
        }

        Poll poll = pollOpt.get();
        Users user = userRepository.findById(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // check if already is a member
        boolean isMember = pollMemberRepository.existsByPollAndUser(poll.getId(), userEmail);
        if (isMember) {
            return poll;
        }

        // add user as member
        PollMember member = new PollMember(poll, user);
        pollMemberRepository.save(member);

        return poll;
    }

    // rotate access code (for owner only)
    public String rotateAccessCode(Long pollId, String userEmail) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found."));

        // verify owner
        if (poll.getOwner() == null || !poll.getOwner().getEmailId().equals(userEmail)) {
            throw new IllegalArgumentException("Only poll owner can rotate access code.");
        }

        if (poll.getVisibility() != Poll.Visibility.PRIVATE) {
            throw new IllegalArgumentException("Only private polls have access codes.");
        }

        // generate new code
        String newCode = generateAccessCode();
        poll.setAccessCode(newCode);
        pollRepository.save(poll);

        // send an email with the new access code
        try {
            mailingService.sendPollAccessCodeEmail(
                    poll.getOwner().getEmailId(),
                    poll.getTitle(),
                    newCode
            );
        } catch (Exception e) {
            System.err.println("Failed to send access code email: " + e.getMessage());
        }

        return newCode;
    }

    // check if user is member of a poll
    public boolean isMember(Long pollId, String userEmail) {
        return pollMemberRepository.existsByPollAndUser(pollId, userEmail);
    }

    // check if user is the owner of a poll
    public boolean isOwner(Long pollId, String userEmail) {
        Optional<Poll> pollOpt = pollRepository.findById(pollId);
        if (pollOpt.isEmpty()) return false;
        Poll poll = pollOpt.get();
        return poll.getOwner() != null && poll.getOwner().getEmailId().equals(userEmail);
    }

    // list accessible polls for a user
    @Transactional(readOnly = true)
    public List<Map<String,Object>> listAccessiblePolls(String userEmail) {
        List<Poll> publicPolls = pollRepository.findAll().stream()
                .filter(p -> p.getVisibility() == Poll.Visibility.PUBLIC)
                .collect(Collectors.toList());
        List<PollMember> memberships = pollMemberRepository.findAllByUserEmail(userEmail);
        List<Poll> privatePolls = memberships.stream()
                .map(PollMember::getPoll)
                .filter(p -> p.getVisibility() == Poll.Visibility.PRIVATE)
                .collect(Collectors.toList());

        List<Poll> accessible = new ArrayList<>();
        accessible.addAll(publicPolls);
        accessible.addAll(privatePolls);

        return accessible.stream().map(p -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("title", p.getTitle());
            m.put("visibility", p.getVisibility());
            m.put("hasAccessCode", p.getAccessCode() != null);
            m.put("isMember", pollMemberRepository.existsByPollAndUser(p.getId(), userEmail));
            return m;
        }).collect(Collectors.toList());
    }

    // returns the access code for a poll (owner of poll only)
    public String getAccessCode(Long pollId, String requesterEmail) {
        Optional<Poll> pollOpt = pollRepository.findById(pollId);
        if (pollOpt.isEmpty()) {
            throw new IllegalArgumentException("Poll not found");
        }

        Poll poll = pollOpt.get();

        // Only owner can get the access code
        Users owner = poll.getOwner();
        if (owner == null || !owner.getEmailId().equalsIgnoreCase(requesterEmail)) {
            throw new IllegalArgumentException("Access denied: only poll owner can retrieve the code");
        }

        // Only private polls have access codes
        if (poll.getVisibility() != Poll.Visibility.PRIVATE) {
            throw new IllegalArgumentException("Access code only exists for private polls");
        }

        String code = poll.getAccessCode();
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Access code not available");
        }

        return code;
    }

    /* ------------------------------------------------------------------ */
    /*  ACCESS LEVEL MANAGEMENT                                            */
    /* ------------------------------------------------------------------ */

    // check if a user is the owner of a poll
    public boolean isPollOwner(Long pollId, String userEmail) {
        return isOwner(pollId, userEmail); // Uses existing method
    }

    // check if a user is an active moderator for a poll
    public boolean isPollModerator(Long pollId, String userEmail) {
        return pollModeratorRepository.isActiveModerator(pollId, userEmail);
    }

    // check if a user can edit a poll (only owner or active moderator)
    public boolean canEditPoll(Long pollId, String userEmail) {
        return isPollOwner(pollId, userEmail) || isPollModerator(pollId, userEmail);
    }

    // check if a user can delete a poll (only owner)
    public boolean canDeletePoll(Long pollId, String userEmail) {
        return isPollOwner(pollId, userEmail);
    }

    // assign moderators to a poll
    @Transactional
    public PollModerator assignModerator(Long pollId, String requesterEmail, ModeratorAssignmentDTO moderatorDto) {
        // verify requester is the owner
        if (!isPollOwner(pollId, requesterEmail)) {
            throw new IllegalArgumentException("Only poll owner can assign moderators");
        }

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new NoSuchElementException("Poll not found"));

        // find the moderator user
        Users moderator = userRepository.findById(moderatorDto.getModeratorEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + moderatorDto.getModeratorEmail()));
        if (moderator == null) {
            throw new IllegalArgumentException("Moderator user not found: " + moderatorDto.getModeratorEmail());
        }

        // check if already a moderator
        Optional<PollModerator> existing = pollModeratorRepository
                .findByPollAndModerator(pollId, moderatorDto.getModeratorEmail());

        if (existing.isPresent()) {
            // update
            PollModerator existingMod = existing.get();
            existingMod.setExpiresAt(moderatorDto.calculateExpiresAt());
            existingMod.setActive(true);
            existingMod.setAssignedAt(LocalDateTime.now());
            return pollModeratorRepository.save(existingMod);
        }

        // create new moderator assignment
        LocalDateTime expiresAt = moderatorDto.calculateExpiresAt();
        PollModerator pollModerator = new PollModerator(poll, moderator, expiresAt);

        return pollModeratorRepository.save(pollModerator);
    }

    // remove a moderator from a poll
    @Transactional
    public void removeModerator(Long pollId, String requesterEmail, String moderatorEmail) {
        // verify requester is the owner
        if (!isPollOwner(pollId, requesterEmail)) {
            throw new IllegalArgumentException("Only poll owner can remove moderators");
        }

        PollModerator moderator = pollModeratorRepository
                .findByPollAndModerator(pollId, moderatorEmail)
                .orElseThrow(() -> new NoSuchElementException("Moderator assignment not found"));

        moderator.setActive(false);
        pollModeratorRepository.save(moderator);
    }

    // get all moderators for a poll
    @Transactional(readOnly = true)
    public List<PollModerator> getPollModerators(Long pollId, String requesterEmail) {
        // only owner can view moderators
        if (!isPollOwner(pollId, requesterEmail)) {
            throw new IllegalArgumentException("Only poll owner can view moderators");
        }

        return pollModeratorRepository.findActiveModerators(pollId);
    }

    // get all polls where user is owner or moderator
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserAccessiblePolls(String userEmail) {
        List<Map<String, Object>> result = new ArrayList<>();

        // get polls where user is owner
        List<Poll> ownedPolls = pollRepository.findAll().stream()
                .filter(p -> isOwner(p.getId(), userEmail))
                .collect(Collectors.toList());

        for (Poll poll : ownedPolls) {
            Map<String, Object> pollData = new HashMap<>();
            pollData.put("pollId", poll.getId());
            pollData.put("title", poll.getTitle());
            pollData.put("type", poll.getType());
            pollData.put("role", "OWNER");
            pollData.put("visibility", poll.getVisibility());
            pollData.put("isActive", poll.isActive());
            result.add(pollData);
        }

        // get polls where user is moderator
        List<PollModerator> moderatedPolls = pollModeratorRepository
                .findPollsModeratedByUser(userEmail);

        for (PollModerator mod : moderatedPolls) {
            Poll poll = mod.getPoll();
            Map<String, Object> pollData = new HashMap<>();
            pollData.put("pollId", poll.getId());
            pollData.put("title", poll.getTitle());
            pollData.put("type", poll.getType());
            pollData.put("role", "MODERATOR");
            pollData.put("visibility", poll.getVisibility());
            pollData.put("isActive", poll.isActive());
            pollData.put("moderatorExpiresAt", mod.getExpiresAt());
            pollData.put("isForeverAccess", mod.isForeverAccess());
            result.add(pollData);
        }

        return result;
    }

    // update a poll (owner or moderator)
    // allows updates on title and context (no structural changes)
    @Transactional
    public Poll updatePoll(Long pollId, String requesterEmail, Map<String, Object> updates) {
        // verify user can edit
        if (!canEditPoll(pollId, requesterEmail)) {
            throw new IllegalArgumentException("Access denied: only owner or moderator can edit this poll");
        }

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new NoSuchElementException("Poll not found"));

        // update allowed fields
        if (updates.containsKey("title")) {
            poll.setTitle((String) updates.get("title"));
        }

        if (updates.containsKey("context")) {
            poll.setContext((String) updates.get("context"));
        }

        // update options for polls that have them
        if (updates.containsKey("options") && poll instanceof MultipleChoicePoll) {
            @SuppressWarnings("unchecked")
            List<String> newOptions = (List<String>) updates.get("options");
            ((MultipleChoicePoll) poll).setOptions(newOptions);
        }

        // regenerate embedding if content changed
        if (updates.containsKey("title") || updates.containsKey("context") || updates.containsKey("options")) {
            try {
                String textRep = poll.getTextRepresentation();
                float[] vec = embeddingModel.embed(textRep);
                String embeddingJson = gson.toJson(vec);
                poll.setEmbedding(embeddingJson);
            } catch (Exception e) {
                log.error("Failed to update embedding for poll {}: {}", pollId, e.getMessage());
            }
        }

        return pollRepository.save(poll);
    }

    // delete a poll (owner only)
    @Transactional
    public void deletePoll(Long pollId, String requesterEmail) {
        if (!canDeletePoll(pollId, requesterEmail)) {
            throw new IllegalArgumentException("Access denied: only poll owner can delete this poll");
        }

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new NoSuchElementException("Poll not found"));

        // Delete associated moderators
        pollModeratorRepository.deleteByPollId(pollId);

        // Delete the poll (cascades to votes, windows, etc.)
        pollRepository.delete(poll);
    }
}