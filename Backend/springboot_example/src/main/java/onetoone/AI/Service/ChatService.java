package onetoone.AI.Service;

import onetoone.AI.model.ChatMessage;
import onetoone.AI.model.SentenceTransformerEmbeddingModel;
import onetoone.AI.repository.ChatMessageRepository;
import onetoone.Users.model.Users;
import onetoone.Users.repository.UserRepository;
import onetoone.Polling.model.*;
import onetoone.Polling.service.PollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final OpenAiChatModel chatModel;

    @Autowired private SentenceTransformerEmbeddingModel embeddingModel;
    @Autowired private PollService pollService;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private UserRepository userRepository;

    // Regex patterns for poll ID detection
    private static final Pattern POLL_ID_PATTERN = Pattern.compile(
            "\\bpoll\\s*#?(\\d+)\\b|\\bpoll\\s+id\\s*:?\\s*(\\d+)\\b|#(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    @Autowired
    public ChatService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String getChatResponse(String userInput) {
        return getChatResponseWithAuth(userInput, null);
    }

    @Transactional
    public String getChatResponseWithAuth(String userInput, String authToken) {
        long startTime = System.currentTimeMillis();

        // Find user by auth token (if provided)
        Users user = null;
        if (authToken != null && !authToken.isEmpty()) {
            String token = authToken.startsWith("Bearer ")
                    ? authToken.substring(7)
                    : authToken;
            log.info("Token: {}", token);

            user = userRepository.findByauthtoken(token);
            if (user == null) {
                log.warn("Invalid token attempt: {}", token);
                return "{\"error\":\"Invalid or expired authentication token\"}";
            }
            if (!user.isVerified()) {
                return "{\"error\":\"Please verify your email before using chat\"}";
            }
        }

        ChatMessage chatMessage = user != null ?
                new ChatMessage(user, userInput) :
                null;

        try {
            log.info("Processing chat query{}: {}",
                    user != null ? " from " + user.getEmailId() : "",
                    userInput);

            // 🎯 HYBRID SEARCH: Combine exact ID matching + semantic search
            List<Poll> relevant = performHybridSearch(userInput);

            log.info("📊 Total polls retrieved: {} (hybrid search result)", relevant.size());

            if (chatMessage != null) {
                chatMessage.setReferencedPolls(relevant);
            }

            StringBuilder ctx = buildContext(relevant);
            String prompt = buildPrompt(ctx.toString(), userInput);
            String response = chatModel.call(prompt);

            if (chatMessage != null) {
                chatMessage.setBotResponse(response);
                long elapsed = System.currentTimeMillis() - startTime;
                chatMessage.setResponseTimeMs(elapsed);
                chatMessageRepository.save(chatMessage);
                log.info("Chat saved for user {} in {}ms", user.getEmailId(), elapsed);
            }

            return response;

        } catch (Exception e) {
            log.error("Error processing chat: {}", e.getMessage(), e);
            String errorMsg = "Sorry, error: " + e.getMessage();

            if (chatMessage != null) {
                chatMessage.setBotResponse(errorMsg);
                chatMessage.setResponseTimeMs(System.currentTimeMillis() - startTime);
                chatMessageRepository.save(chatMessage);
            }

            return errorMsg;
        }
    }

    /**
     * Hybrid search: Combines exact ID matching with semantic search
     * Priority: Mentioned polls first, then fill with semantic matches
     */
    private List<Poll> performHybridSearch(String userInput) throws Exception {
        Set<Poll> results = new LinkedHashSet<>(); // Use LinkedHashSet to maintain order and avoid duplicates

        // Step 1: Extract and fetch polls mentioned by ID
        List<Long> mentionedIds = extractPollIds(userInput);

        if (!mentionedIds.isEmpty()) {
            log.info("🎯 User mentioned poll IDs: {}", mentionedIds);

            for (Long pollId : mentionedIds) {
                Optional<Poll> poll = pollService.findPoll(pollId);
                if (poll.isPresent()) {
                    results.add(poll.get());
                    log.info("  ✅ Added poll #{}: '{}'", pollId, poll.get().getTitle());
                } else {
                    log.warn("  ❌ Poll #{} not found", pollId);
                }
            }
        }

        // Step 2: Perform semantic search to fill remaining slots
        int neededPolls = 5 - results.size();

        if (neededPolls > 0) {
            log.info("🔍 Performing semantic search for {} more polls...", neededPolls);

            float[] qVec = embeddingModel.embed(userInput);
            log.info("Generated query embedding of length: {}", qVec.length);

            // Get more candidates than needed to account for duplicates
            List<Poll> semanticMatches = pollService.findSimilarPolls(qVec, neededPolls + mentionedIds.size());

            // Add semantic matches, skipping duplicates
            int added = 0;
            for (Poll p : semanticMatches) {
                if (results.stream().noneMatch(existing -> existing.getId().equals(p.getId()))) {
                    results.add(p);
                    added++;
                    log.info("  ✅ Added semantic match poll #{}: '{}' (rank: #{})",
                            p.getId(), p.getTitle(), results.size());

                    if (results.size() >= 5) break; // Stop when we have 5 total
                }
            }
            log.info("  📈 Added {} polls from semantic search", added);
        }

        return new ArrayList<>(results);
    }

    /**
     * Extract poll IDs from user query
     * Matches: "poll 50", "poll #50", "poll ID: 50", "#50"
     */
    private List<Long> extractPollIds(String userInput) {
        List<Long> pollIds = new ArrayList<>();
        Matcher matcher = POLL_ID_PATTERN.matcher(userInput);

        while (matcher.find()) {
            try {
                // Check which group matched and extract the ID
                String idStr = null;
                if (matcher.group(1) != null) {
                    idStr = matcher.group(1);
                } else if (matcher.group(2) != null) {
                    idStr = matcher.group(2);
                } else if (matcher.group(3) != null) {
                    idStr = matcher.group(3);
                }

                if (idStr != null) {
                    Long pollId = Long.parseLong(idStr);
                    if (!pollIds.contains(pollId)) {
                        pollIds.add(pollId);
                    }
                }
            } catch (NumberFormatException e) {
                log.warn("Failed to parse poll ID from matcher groups");
            }
        }

        return pollIds;
    }

    public List<ChatMessage> getUserChatHistory(String email) {
        return chatMessageRepository.findByUserEmailOrderByTimestampDesc(email);
    }

    public long getUserMessageCount(String email) {
        return chatMessageRepository.countByUserEmail(email);
    }

    private StringBuilder buildContext(List<Poll> relevant) {
        StringBuilder ctx = new StringBuilder();
        if (relevant.isEmpty()) {
            ctx.append("No polls found.\n");
        } else {
            ctx.append("Available polls:\n\n");
            for (int i = 0; i < relevant.size(); i++) {
                Poll p = relevant.get(i);
                ctx.append(String.format("%d. Poll #%d: \"%s\"\n",
                        i + 1, p.getId(), p.getTitle()));
                ctx.append(String.format("   Type: %s, Status: %s\n",
                        p.getType(), p.isActive() ? "Active" : "Closed"));

                if (p.getContext() != null && !p.getContext().trim().isEmpty()) {
                    ctx.append(String.format("   Context: %s\n", p.getContext()));
                }

                if (p instanceof MultipleChoicePoll) {
                    MultipleChoicePoll mcp = (MultipleChoicePoll) p;
                    ctx.append(String.format("   Options: %s\n",
                            String.join(", ", mcp.getOptions())));
                } else if (p instanceof RankingPoll) {
                    RankingPoll rp = (RankingPoll) p;
                    ctx.append(String.format("   Options to rank: %s\n",
                            String.join(", ", rp.getOptions())));
                } else if (p instanceof RatingPoll) {
                    ctx.append("   Rating scale: 1-5\n");
                } else if (p instanceof YesNoPoll) {
                    ctx.append("   Response options: Yes/No\n");
                }

                try {
                    String results = fetchLiveResults(p);
                    ctx.append(String.format("   Results: %s\n", results));
                } catch (Exception e) {
                    ctx.append("   Results: unavailable\n");
                }
                ctx.append("\n");
            }
        }
        return ctx;
    }

    private String buildPrompt(String context, String userInput) {
        return String.format("""
            You are a helpful poll assistant. Answer using the poll data below.
            Be specific with poll IDs, titles, and exact numbers.
            
            POLL DATA:
            %s
            
            USER QUESTION: %s
            
            YOUR ANSWER:
            """, context, userInput);
    }

    private String fetchLiveResults(Poll p) {
        try {
            if (p instanceof YesNoPoll) {
                var stats = pollService.getYesNoStats(p.getId());
                return String.format("Yes: %s (%.1f%%), No: %s",
                        stats.get("yes"), stats.get("yesPercent"), stats.get("no"));
            } else if (p instanceof RatingPoll) {
                var stats = pollService.getRatingStats(p.getId());
                return String.format("Avg: %.2f/5, Total: %s votes",
                        stats.get("average"), stats.get("totalVotes"));
            } else if (p instanceof MultipleChoicePoll mcp) {
                var stats = pollService.getMultipleChoiceStats(p.getId(), mcp.getOptions());
                @SuppressWarnings("unchecked")
                List<java.util.Map<String,Object>> breakdown =
                        (List<java.util.Map<String,Object>>) stats.get("breakdown");

                StringBuilder sb = new StringBuilder();
                for (var item : breakdown) {
                    sb.append(String.format("%s: %s (%.1f%%), ",
                            item.get("option"), item.get("count"), item.get("percent")));
                }
                return sb.toString() + "Total: " + stats.get("totalVotes");
            } else if (p instanceof RankingPoll rp) {
                var stats = pollService.getRankingStats(p.getId(), rp.getOptions().size());
                @SuppressWarnings("unchecked")
                List<java.util.Map<String,Object>> scores =
                        (List<java.util.Map<String,Object>>) stats.get("scores");

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(3, scores.size()); i++) {
                    var item = scores.get(i);
                    sb.append(String.format("#%d: %s (score: %s), ",
                            i + 1, item.get("choice"), item.get("score")));
                }
                return sb.toString() + "Total: " + stats.get("totalVotes");
            }
            return "available";
        } catch (Exception ex) {
            return "unavailable";
        }
    }
}