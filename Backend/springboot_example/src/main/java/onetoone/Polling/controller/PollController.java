package onetoone.Polling.controller;

import onetoone.LiveResults.controller.LiveResultsController;
import onetoone.LiveResults.service.LiveResultsService;
import onetoone.Location.dto.VoteRequest;
import onetoone.Polling.model.*;
import onetoone.Polling.repository.PollRepository;
import onetoone.Location.service.LocationService;
import onetoone.Polling.service.PollService;
import onetoone.Users.repository.UserRepository;
import onetoone.Polling.model.PollModerator;
//import onetoone.Polling.util.ModeratorUtils;
import onetoone.Polling.dto.ModeratorAssignmentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/polls")
public class PollController {

    @Autowired
    private PollService pollService;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private LiveResultsService liveResultsService;

    @Autowired
    private UserRepository userRepository;

    // Create endpoints for each poll type

    @PostMapping("/yesno")
    public YesNoPoll createYesNoPoll(@RequestBody Map<String,Object> body) {
        String title = (String)body.get("title");
        String context = (String)body.get("context");  // NEW
        String visibilityStr = (String)body.getOrDefault("visibility", "PUBLIC");
        Poll.Visibility visibility = Poll.Visibility.valueOf(visibilityStr.toUpperCase());

        // get owners emailId from request body
        String ownerEmail = (String)body.get("emailId");

        YesNoPoll p = new YesNoPoll(title);
        p.setContext(context);  // NEW
        p.setVisibility(visibility);
        p.setCreatedAt(LocalDateTime.now());

        YesNoPoll created = (YesNoPoll) pollService.createPoll(p, ownerEmail);

        // moderator assignments if provided
        if (body.containsKey("moderators")) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> mods = (List<Map<String, String>>) body.get("moderators");
            for (Map<String, String> m : mods) {
                String modEmail = m.get("moderatorEmail");

                // Check if moderator exists first
                if (!userRepository.existsById(modEmail)) {
                    System.err.println("Skipping moderator - user not found: " + modEmail);
                    continue;
                }

                ModeratorAssignmentDTO dto = new ModeratorAssignmentDTO();
                dto.setModeratorEmail(modEmail);
                dto.setDuration(m.get("duration"));

                try {
                    pollService.assignModerator(created.getId(), ownerEmail, dto);
                } catch (Exception e) {
                    System.err.println("Moderator assignment failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return created;

    }

    @PostMapping("/rating")
    public RatingPoll createRatingPoll(@RequestBody Map<String,Object> body) {
        String title = (String)body.get("title");
        String context = (String)body.get("context");  // NEW
        String visibilityStr = (String)body.getOrDefault("visibility", "PUBLIC");
        Poll.Visibility visibility = Poll.Visibility.valueOf(visibilityStr.toUpperCase());

        // get owners emailId from request body
        String ownerEmail = (String)body.get("emailId");

        RatingPoll p = new RatingPoll(title);
        p.setContext(context);  // NEW
        p.setVisibility(visibility);
        p.setCreatedAt(LocalDateTime.now());

        RatingPoll created = (RatingPoll) pollService.createPoll(p, ownerEmail);

        // moderator assignments if provided
        if (body.containsKey("moderators")) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> mods = (List<Map<String, String>>) body.get("moderators");
            for (Map<String, String> m : mods) {
                String modEmail = m.get("moderatorEmail");

                // Check if moderator exists first
                if (!userRepository.existsById(modEmail)) {
                    System.err.println("Skipping moderator - user not found: " + modEmail);
                    continue;
                }

                ModeratorAssignmentDTO dto = new ModeratorAssignmentDTO();
                dto.setModeratorEmail(modEmail);
                dto.setDuration(m.get("duration"));

                try {
                    pollService.assignModerator(created.getId(), ownerEmail, dto);
                } catch (Exception e) {
                    System.err.println("Moderator assignment failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return created;
    }

    @PostMapping("/multiple")
    public MultipleChoicePoll createMultipleChoicePoll(@RequestBody Map<String,Object> body) {
        String title = (String) body.get("title");
        String context = (String) body.get("context");  // NEW
        @SuppressWarnings("unchecked")
        List<String> options = (List<String>) body.getOrDefault("options", Collections.emptyList());
        String visibilityStr = body.containsKey("visibility") ? (String) body.get("visibility") : "PUBLIC";
        Poll.Visibility visibility = Poll.Visibility.valueOf(visibilityStr.toUpperCase());

        // get owners emailId from request body
        String ownerEmail = (String) body.get("emailId");

        MultipleChoicePoll p = new MultipleChoicePoll(title, options);
        p.setContext(context);  // NEW
        p.setVisibility(visibility);
        p.setCreatedAt(LocalDateTime.now());

        MultipleChoicePoll created = (MultipleChoicePoll) pollService.createPoll(p, ownerEmail);

        // moderator assignments if provided
        if (body.containsKey("moderators")) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> mods = (List<Map<String, String>>) body.get("moderators");
            for (Map<String, String> m : mods) {
                String modEmail = m.get("moderatorEmail");

                // Check if moderator exists first
                if (!userRepository.existsById(modEmail)) {
                    System.err.println("Skipping moderator - user not found: " + modEmail);
                    continue;
                }

                ModeratorAssignmentDTO dto = new ModeratorAssignmentDTO();
                dto.setModeratorEmail(modEmail);
                dto.setDuration(m.get("duration"));

                try {
                    pollService.assignModerator(created.getId(), ownerEmail, dto);
                } catch (Exception e) {
                    System.err.println("Moderator assignment failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return created;
    }

    @PostMapping("/ranking")
    public RankingPoll createRankingPoll(@RequestBody Map<String,Object> body) {
        String title = (String) body.get("title");
        String context = (String) body.get("context");  // NEW
        @SuppressWarnings("unchecked")
        List<String> options = (List<String>) body.getOrDefault("options", Collections.emptyList());
        String visibilityStr = body.containsKey("visibility") ? (String) body.get("visibility") : "PUBLIC";
        Poll.Visibility visibility = Poll.Visibility.valueOf(visibilityStr.toUpperCase());

        // get owners emailId from request body
        String ownerEmail = (String) body.get("emailId");

        RankingPoll p = new RankingPoll(title, options);
        p.setContext(context);  // NEW
        p.setVisibility(visibility);
        p.setCreatedAt(LocalDateTime.now());

        RankingPoll created = (RankingPoll) pollService.createPoll(p, ownerEmail);

        // moderator assignments if provided
        if (body.containsKey("moderators")) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> mods = (List<Map<String, String>>) body.get("moderators");
            for (Map<String, String> m : mods) {
                String modEmail = m.get("moderatorEmail");

                // Check if moderator exists first
                if (!userRepository.existsById(modEmail)) {
                    System.err.println("Skipping moderator - user not found: " + modEmail);
                    continue;
                }

                ModeratorAssignmentDTO dto = new ModeratorAssignmentDTO();
                dto.setModeratorEmail(modEmail);
                dto.setDuration(m.get("duration"));

                try {
                    pollService.assignModerator(created.getId(), ownerEmail, dto);
                } catch (Exception e) {
                    System.err.println("Moderator assignment failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
            return created;
    }

    // get all polls depending on visibility and user, and optional filters
    @GetMapping
    public List<Poll> getAllPolls(
            @RequestParam(required = false) String emailId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Poll.Visibility visibility){

        List<Poll> allPolls = pollRepository.findAll();
        List<Poll> accessiblePolls;

        if (emailId == null || emailId.isEmpty()) {
            // return only public polls if no emailId provided
            accessiblePolls = allPolls.stream()
                    .filter(p -> p.getVisibility() == Poll.Visibility.PUBLIC)
                    .collect(Collectors.toList());
        }
            else {
            // return public polls and private polls where user is member/owner
            accessiblePolls = allPolls.stream()
                    .filter(p -> p.getVisibility() == Poll.Visibility.PUBLIC ||
                            (pollService.isOwner(p.getId(), emailId) ||
                                    pollService.isMember(p.getId(), emailId)))
                    .collect(Collectors.toList());
        }
        // return with optional filters
        return accessiblePolls.stream()
                // filter by poll type if given
                .filter(p -> type == null || type.isBlank() || p.getType().equalsIgnoreCase(type))
                // filter by visibility if given
                .filter(p -> visibility == null || p.getVisibility() == visibility)
                .collect(Collectors.toList());

    }

    // get poll by id with visibility checks
    @GetMapping("/{id}")
    public Object getPoll(
            @PathVariable Long id,
            @RequestParam(required = false) String emailId) {

        Optional<Poll> pollOpt = pollService.findPoll(id);
        if (pollOpt.isEmpty()) {
            return Map.of("error", "Poll not found");
        }

        Poll poll = pollOpt.get();

        // public polls: everyone can see
        if (poll.getVisibility() == Poll.Visibility.PUBLIC) {
            return poll;
        }

        // private polls: only members/owner can see
        if (poll.getVisibility() == Poll.Visibility.PRIVATE) {
            if (emailId == null || emailId.isEmpty()) {
                return Map.of("error", "emailId required for private polls");
            }

            if (!pollService.isOwner(poll.getId(), emailId) &&
                    !pollService.isMember(poll.getId(), emailId)) {
                return Map.of("error", "Access denied. Not a member of this poll.");
            }

            return poll;
        }

        return poll;
    }


    @PostMapping("/{pollId}/vote/ranking")
    public ResponseEntity<?> submitRanking(
            @PathVariable Long pollId,
            @RequestParam String userId,
            @RequestBody List<VoteRequest> rankingVotes) {

        try {
            if (rankingVotes.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Empty ranking list"));
            }

            // fetch the poll to validate options
            Optional<Poll> pollOpt = pollService.findPoll(pollId);
            if (pollOpt.isEmpty() || !(pollOpt.get() instanceof RankingPoll)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Poll not found or not a ranking poll"));
            }

            RankingPoll rankingPoll = (RankingPoll) pollOpt.get();
            List<String> pollOptions = rankingPoll.getOptions();

            // check if poll is private and user is member
            Poll poll = pollOpt.get();
            if (poll.getVisibility() == Poll.Visibility.PRIVATE) {
                if (!pollService.isOwner(pollId, userId) && !pollService.isMember(pollId, userId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Access denied, user must be a member of the poll to vote on private polls."));
                }
            }

            // check length
            if (rankingVotes.size() != pollOptions.size()) {
                return ResponseEntity.badRequest().body(Map.of("error", "You must rank all options"));
            }

            // check content
            Set<String> votedOptions = new HashSet<>();
            Set<Integer> ranks = new HashSet<>();
            for (VoteRequest vr : rankingVotes) {
                votedOptions.add(vr.getChoice());
                ranks.add(vr.getRank());
            }

            if (!votedOptions.containsAll(pollOptions) || ranks.size() != pollOptions.size() ||
                    !ranks.containsAll(IntStream.rangeClosed(1, pollOptions.size()).boxed().collect(Collectors.toSet()))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Ranking list invalid: must rank all options 1..N exactly once"));
            }

            // validate location for first vote
            VoteRequest first = rankingVotes.get(0);
            locationService.validateLocation(first.getLat(), first.getLng());

            // convert VoteRequest list to Vote list
            List<Vote> votes = new ArrayList<>();
            for (VoteRequest vr : rankingVotes) {
                votes.add(new Vote(userId, vr.getChoice(), vr.getRank()));
            }

            pollService.addRankingVotes(pollId, votes);

            // broadcast live results update
            liveResultsService.broadcastResults(pollId);

            return ResponseEntity.ok(Map.of("message", "Ranking submitted"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }



    // cast a vote with location verification
    // Generic vote endpoint for Yes/No, Rating, Multiple Choice
    @PostMapping("/{pollId}/vote")
    public Map<String,Object> vote(
            @PathVariable Long pollId,
            @RequestParam String userId,
            @RequestBody VoteRequest voteRequest,
            @RequestParam(required = false) Integer numericValue,
            @RequestParam(required = false) String choice) {

        Map<String, Object> res = new HashMap<>();
        try {

            // check if poll is private and user is member
            Optional<Poll> pollOpt = pollService.findPoll(pollId);
            if (pollOpt.isPresent()) {
                Poll poll = pollOpt.get();
                if (poll.getVisibility() == Poll.Visibility.PRIVATE) {
                    if (!pollService.isOwner(pollId, userId) && !pollService.isMember(pollId, userId)) {
                        res.put("error", "Access denied, the user must be a member of this poll to vote on private polls.");
                        return res;
                    }
                }
            }

            // validate user location
            locationService.validateLocation(voteRequest.getLat(), voteRequest.getLng());

            Vote vote;
            if (numericValue != null) {
                vote = new Vote(userId, numericValue); // Yes/No or Rating
            } else if (choice != null) {
                vote = new Vote(userId, choice); // Multiple Choice
            } else {
                throw new IllegalArgumentException("Invalid vote payload: provide numericValue OR choice.");
            }

            vote.setLatitude(voteRequest.getLat());
            vote.setLongitude(voteRequest.getLng());

            pollService.addVote(pollId, vote);

            // broadcast live results update
            liveResultsService.broadcastResults(pollId);

            res.put("message", "Vote added");
            return res;

        } catch (Exception e) {
            res.put("error", e.getMessage());
            return res;
        }
    }



    @GetMapping("/{pollId}/user/{userId}")
    public Object getUserVote(@PathVariable Long pollId, @PathVariable String userId) {
        Optional<Poll> pollOpt = pollService.findPoll(pollId);
        if (pollOpt.isEmpty()) return Map.of("error", "poll not found");
        Poll poll = pollOpt.get();

        if ("RANKING".equals(poll.getType())) {
            List<Vote> votes = pollService.getRankingVotesForUser(pollId, userId);
            if (votes.isEmpty()) return Map.of("message", "User has not voted on this poll yet.");
            List<Map<String, Object>> ranking = votes.stream()
                    .map(v -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("choice", v.getChoice());
                        m.put("rank", v.getRank());
                        return m;
                    })
                    .toList();
            Map<String, Object> response = new HashMap<>();
            response.put("pollId", pollId);
            response.put("userId", userId);
            response.put("ranking", ranking);
            return response;
        } else {
            Vote vote = pollService.getUserVote(pollId, userId);
            if (vote == null) return Map.of("message", "User has not voted on this poll yet.");
            Map<String, Object> voteResponse = new HashMap<>();
            voteResponse.put("id", vote.getId());
            voteResponse.put("userId", vote.getUserId());
            voteResponse.put("choice", vote.getChoice());
            voteResponse.put("numericValue", vote.getNumericValue());
            voteResponse.put("rank", vote.getRank());
            return voteResponse;
        }
    }




    // results endpoint - delegates to service and chooses strategy by poll type
    @GetMapping("/{pollId}/results")
    public Object getResults(@PathVariable Long pollId, @RequestParam(required = false) String emailId) {
        Optional<Poll> op = pollService.findPoll(pollId);
        if (op.isEmpty()) return Map.of("error", "poll not found");

        Poll poll = op.get();

        // check if private poll
        if (poll.getVisibility() == Poll.Visibility.PRIVATE) {
            if (emailId == null || emailId.isEmpty()) {
                return Map.of("error", "emailId required for private polls");
            }
            if (!pollService.isOwner(pollId, emailId) && !pollService.isMember(pollId, emailId)) {
                return Map.of("error", "Access denied, the user is not a member of this poll.");
            }
        }

        String type = poll.getType();
        switch (type) {
            case "YES_NO":
                return pollService.getYesNoStats(pollId);
            case "RATING":
                return pollService.getRatingStats(pollId);
            case "MULTIPLE_CHOICE":
                // fetch options from concrete type
                MultipleChoicePoll mcp = (MultipleChoicePoll) poll;
                return pollService.getMultipleChoiceStats(pollId, mcp.getOptions());
            case "RANKING":
                RankingPoll r = (RankingPoll) poll;
                return pollService.getRankingStats(pollId, r.getOptions().size());
            default:
                return Map.of("error", "unknown poll type");
        }
    }

    // close poll
    @PutMapping("/{pollId}/close")
    public Map<String,String> closePoll(@PathVariable Long pollId) {
        Map<String,String> res = new HashMap<>();
        Optional<Poll> op = pollService.findPoll(pollId);
        if (op.isEmpty()) {
            res.put("error", "not found");
            return res;
        }
        Poll p = op.get();
        p.setActive(false);
        pollRepository.save(p);

        // broadcast that status changed to closed
        liveResultsService.broadcastResults(pollId);
        res.put("message", "poll closed");
        return res;
    }

    @DeleteMapping("/{pollId}/user/{userId}")
    public Map<String, String> deleteUserVote(@PathVariable Long pollId, @PathVariable String userId) {
        Map<String, String> res = new HashMap<>();
        Optional<Poll> pollOpt = pollService.findPoll(pollId);
        if (pollOpt.isEmpty()) {
            res.put("error", "Poll not found");
            return res;
        }
        Poll poll = pollOpt.get();
        boolean isRanking = "RANKING".equals(poll.getType());

        try {
            boolean deleted = pollService.deleteUserVote(pollId, userId, isRanking);
            if (deleted) {
                // broadcast updated results
                liveResultsService.broadcastResults(pollId);
                res.put("message", "Vote deleted");
            } else {
                res.put("error", "Vote not found");
            }
        } catch (Exception e) {
            res.put("error", e.getMessage());
        }
        return res;
    }

    @PostMapping("/join")
    public Map<String, Object> joinPoll(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String emailId = body.get("emailId");

        Map<String, Object> response = new HashMap<>();

        if (code == null || code.isEmpty()) {
            response.put("error", "Access code required");
            return response;
        }

        if (emailId == null || emailId.isEmpty()) {
            response.put("error", "emailId required");
            return response;
        }

        try {
            Poll poll = pollService.joinPoll(code, emailId);
            response.put("pollId", poll.getId());
            response.put("title", poll.getTitle());
            response.put("visibility", poll.getVisibility().toString());
            return response;
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return response;
        }
    }

    @PostMapping("/{id}/rotate-code")
    public Map<String, String> rotateAccessCode(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String emailId = body.get("emailId");

        Map<String, String> response = new HashMap<>();

        if (emailId == null || emailId.isEmpty()) {
            response.put("error", "emailId required");
            return response;
        }

        try {
            String newCode = pollService.rotateAccessCode(id, emailId);
            response.put("accessCode", newCode);
            return response;
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return response;
        }
    }

    @GetMapping("/{pollId}/access-code")
    public Map<String, String> getAccessCode(@PathVariable Long pollId,
                                             @RequestParam String emailId) {
        Map<String, String> response = new HashMap<>();
        if (emailId == null || emailId.isEmpty()) {
            response.put("error", "emailId required");
            return response;
        }

        try {
            String code = pollService.getAccessCode(pollId, emailId); // only owner can access
            response.put("accessCode", code);
            return response;
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return response;
        }
    }

    /* ------------------------------------------------------------*/
    /*  ACCESS LEVEL ENDPOINTS                                     */
    /* ------------------------------------------------------------ */

    // edit a poll (owner or moderator)
    @PutMapping("/{pollId}/edit")
    public ResponseEntity<?> editPoll(
            @PathVariable Long pollId,
            @RequestParam String emailId,
            @RequestBody Map<String, Object> updates) {

        try {
            Poll updatedPoll = pollService.updatePoll(pollId, emailId, updates);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Poll updated successfully");
            response.put("pollId", updatedPoll.getId());
            response.put("title", updatedPoll.getTitle());
            response.put("type", updatedPoll.getType());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update poll: " + e.getMessage()));
        }
    }

    // delete a poll (owner only)
    @DeleteMapping("/{pollId}")
    public ResponseEntity<?> deletePoll(
            @PathVariable Long pollId,
            @RequestParam String emailId) {

        try {
            pollService.deletePoll(pollId, emailId);
            return ResponseEntity.ok(Map.of("message", "Poll deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete poll: " + e.getMessage()));
        }
    }


    // assign a moderator to a poll
    @PostMapping("/{pollId}/moderators")
    public ResponseEntity<?> assignModerator(
            @PathVariable Long pollId,
            @RequestParam String emailId,
            @RequestBody ModeratorAssignmentDTO moderatorDto) {

        try {
            PollModerator moderator = pollService.assignModerator(pollId, emailId, moderatorDto);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Moderator assigned successfully");
            response.put("moderatorEmail", moderator.getModeratorEmail());
            response.put("expiresAt", moderator.getExpiresAt());
            response.put("isForeverAccess", moderator.isForeverAccess());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // remove a moderator from a poll
    @DeleteMapping("/{pollId}/moderators/{moderatorEmail}")
    public ResponseEntity<?> removeModerator(
            @PathVariable Long pollId,
            @PathVariable String moderatorEmail,
            @RequestParam String emailId) {

        try {
            pollService.removeModerator(pollId, emailId, moderatorEmail);
            return ResponseEntity.ok(Map.of("message", "Moderator removed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to remove moderator: " + e.getMessage()));
        }
    }

    // get all moderators for a poll (owner only)
    @GetMapping("/{pollId}/moderators")
    public ResponseEntity<?> getPollModerators(
            @PathVariable Long pollId,
            @RequestParam String emailId) {

        try {
            List<PollModerator> moderators = pollService.getPollModerators(pollId, emailId);

            List<Map<String, Object>> moderatorList = moderators.stream()
                    .map(mod -> {
                        Map<String, Object> modData = new HashMap<>();
                        modData.put("moderatorEmail", mod.getModeratorEmail());
                        modData.put("assignedAt", mod.getAssignedAt());
                        modData.put("expiresAt", mod.getExpiresAt());
                        modData.put("isForeverAccess", mod.isForeverAccess());
                        modData.put("isCurrentlyValid", mod.isCurrentlyValid());
                        return modData;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("moderators", moderatorList));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get moderators: " + e.getMessage()));
        }
    }

    // get all polls a user has access to
    @GetMapping("/user/{emailId}/accessible")
    public ResponseEntity<?> getUserAccessiblePolls(@PathVariable String emailId) {
        try {
            List<Map<String, Object>> polls = pollService.getUserAccessiblePolls(emailId);
            return ResponseEntity.ok(Map.of("polls", polls));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch accessible polls: " + e.getMessage()));
        }
    }

    // check a users access level for a poll
    @GetMapping("/{pollId}/access-level")
    public ResponseEntity<?> checkAccessLevel(
            @PathVariable Long pollId,
            @RequestParam String emailId) {

        try {
            boolean isOwner = pollService.isPollOwner(pollId, emailId);
            boolean isModerator = pollService.isPollModerator(pollId, emailId);
            boolean canEdit = pollService.canEditPoll(pollId, emailId);
            boolean canDelete = pollService.canDeletePoll(pollId, emailId);

            String role = isOwner ? "OWNER" : (isModerator ? "MODERATOR" : "USER");

            Map<String, Object> response = new HashMap<>();
            response.put("pollId", pollId);
            response.put("userEmail", emailId);
            response.put("role", role);
            response.put("canEdit", canEdit);
            response.put("canDelete", canDelete);
            response.put("isOwner", isOwner);
            response.put("isModerator", isModerator);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check access level: " + e.getMessage()));
        }
    }

    // helper method for creatin polls to assign moderators from a list
    private void assignModeratorsFromList(Long pollId, String ownerEmail, List<ModeratorAssignmentDTO> moderators) {
        for (ModeratorAssignmentDTO dto : moderators) {
            try {
                pollService.assignModerator(pollId, ownerEmail, dto);
            } catch (Exception e) {
                System.err.println("Failed to assign moderator: " + dto.getModeratorEmail() + " → " + e.getMessage());
            }
        }
    }
}