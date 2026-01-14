// DiscussionWebSocketServer.java
package onetoone.LiveDiscussion.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import onetoone.LiveDiscussion.model.Discussion;
import onetoone.LiveDiscussion.model.DiscussionMessage;
import onetoone.LiveDiscussion.model.MessageType;
import onetoone.LiveDiscussion.service.DiscussionService;
import onetoone.Users.model.Users;
import onetoone.Users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket server for discussion-based real-time messaging
 * Endpoint: ws://localhost:8080/discussion/{discussionId}?token={authToken}
 */
@ServerEndpoint("/discussion/{discussionId}")
@Component
public class DiscussionWebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(DiscussionWebSocketServer.class);
    private static final Gson gson = new Gson();

    // Static maps to store sessions per discussion
    // discussionId -> (userEmail -> Session)
    private static Map<Long, Map<String, Session>> discussionSessions = new ConcurrentHashMap<>();

    // session -> discussionId mapping
    private static Map<Session, Long> sessionDiscussionMap = new ConcurrentHashMap<>();

    // session -> userEmail mapping
    private static Map<Session, String> sessionUserMap = new ConcurrentHashMap<>();

    // Spring beans (injected via configurator)
    private static DiscussionService discussionService;
    private static UserRepository userRepository;

    @Autowired
    public void setDiscussionService(DiscussionService service) {
        DiscussionWebSocketServer.discussionService = service;
    }

    @Autowired
    public void setUserRepository(UserRepository repository) {
        DiscussionWebSocketServer.userRepository = repository;
    }

    /**
     * Called when a new WebSocket connection is established
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("discussionId") Long discussionId) {
        try {
            log.info("[onOpen] Attempting connection to discussion {}", discussionId);

            // Extract token from query parameters
            String token = getTokenFromSession(session);
            if (token == null) {
                sendError(session, "Authentication required");
                session.close();
                return;
            }

            // Validate token and get user
            Users user = userRepository.findByauthtoken(token);
            if (user == null) {
                sendError(session, "Invalid authentication token");
                session.close();
                return;
            }

            String userEmail = user.getEmailId();

            // Validate discussion exists
            Discussion discussion = discussionService.getDiscussionById(discussionId);
            if (discussion == null) {
                sendError(session, "Discussion not found");
                session.close();
                return;
            }

            // Check if user is a participant
            if (!discussionService.isParticipant(discussionId, userEmail)) {
                sendError(session, "You must join the discussion before connecting");
                session.close();
                return;
            }

            // Check if discussion is active
            if (!discussion.isActive()) {
                sendError(session, "Discussion is closed");
                session.close();
                return;
            }

            // Add session to maps
            discussionSessions.computeIfAbsent(discussionId, k -> new ConcurrentHashMap<>())
                    .put(userEmail, session);
            sessionDiscussionMap.put(session, discussionId);
            sessionUserMap.put(session, userEmail);

            // Update participant online status
            discussionService.updateParticipantStatus(discussionId, userEmail, true);

            // Send welcome message to user
            long participantCount = discussionService.getParticipantCount(discussionId);
            long onlineCount = discussionSessions.get(discussionId).size();

            JsonObject welcome = new JsonObject();
            welcome.addProperty("type", "WELCOME");
            welcome.addProperty("message", "Connected to discussion: " + discussion.getTitle());
            welcome.addProperty("discussionId", discussionId);
            welcome.addProperty("participantCount", participantCount);
            welcome.addProperty("onlineCount", onlineCount);
            session.getBasicRemote().sendText(gson.toJson(welcome));

            // Broadcast user joined to others
            JsonObject userJoined = new JsonObject();
            userJoined.addProperty("type", "USER_JOINED");
            JsonObject userObj = new JsonObject();
            userObj.addProperty("emailId", user.getEmailId());
            userObj.addProperty("name", user.getName());
            userJoined.add("user", userObj);
            userJoined.addProperty("timestamp", LocalDateTime.now().toString());
            userJoined.addProperty("participantCount", participantCount);

            broadcastToDiscussion(discussionId, gson.toJson(userJoined), userEmail);

            log.info("[onOpen] User {} connected to discussion {}", userEmail, discussionId);

        } catch (Exception e) {
            log.error("[onOpen] Error: {}", e.getMessage(), e);
            try {
                sendError(session, "Connection failed: " + e.getMessage());
                session.close();
            } catch (IOException ioe) {
                log.error("[onOpen] Failed to close session", ioe);
            }
        }
    }

    /**
     * Handles incoming messages from clients
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        try {
            Long discussionId = sessionDiscussionMap.get(session);
            String userEmail = sessionUserMap.get(session);

            if (discussionId == null || userEmail == null) {
                sendError(session, "Invalid session");
                return;
            }

            log.info("[onMessage] Discussion {}, User {}: {}", discussionId, userEmail, message);

            // Parse message JSON
            JsonObject messageObj = gson.fromJson(message, JsonObject.class);
            String type = messageObj.get("type").getAsString();
            String content = messageObj.get("content").getAsString();

            // Validate content
            if (content == null || content.trim().isEmpty()) {
                sendError(session, "Message content cannot be empty");
                return;
            }

            if (content.length() > 2000) {
                sendError(session, "Message exceeds maximum length");
                return;
            }

            Users sender = userRepository.findByEmailId(userEmail);

            if ("BROADCAST".equals(type)) {
                // Save broadcast message
                DiscussionMessage savedMessage = discussionService.saveMessage(
                        discussionId, userEmail, content, MessageType.BROADCAST, null);

                // Broadcast to all participants
                JsonObject broadcast = new JsonObject();
                broadcast.addProperty("type", "MESSAGE");
                broadcast.addProperty("messageId", savedMessage.getId());

                JsonObject userObj = new JsonObject();
                userObj.addProperty("emailId", sender.getEmailId());
                userObj.addProperty("name", sender.getName());
                broadcast.add("user", userObj);

                broadcast.addProperty("content", content);
                broadcast.addProperty("timestamp", savedMessage.getTimestamp().toString());

                broadcastToDiscussion(discussionId, gson.toJson(broadcast), null);

            } else if ("DIRECT_MESSAGE".equals(type)) {
                // Handle direct message
                String recipientEmail = messageObj.get("recipientEmail").getAsString();

                // Check if recipient is in discussion
                if (!discussionService.isParticipant(discussionId, recipientEmail)) {
                    sendError(session, "Recipient not found in this discussion");
                    return;
                }

                // Save direct message
                DiscussionMessage savedMessage = discussionService.saveMessage(
                        discussionId, userEmail, content, MessageType.DIRECT_MESSAGE, recipientEmail);

                // Send to sender and recipient only
                JsonObject dm = new JsonObject();
                dm.addProperty("type", "DIRECT_MESSAGE");
                dm.addProperty("messageId", savedMessage.getId());

                JsonObject senderObj = new JsonObject();
                senderObj.addProperty("emailId", sender.getEmailId());
                senderObj.addProperty("name", sender.getName());
                dm.add("sender", senderObj);

                Users recipient = userRepository.findByEmailId(recipientEmail);
                JsonObject recipientObj = new JsonObject();
                recipientObj.addProperty("emailId", recipient.getEmailId());
                recipientObj.addProperty("name", recipient.getName());
                dm.add("recipient", recipientObj);

                dm.addProperty("content", content);
                dm.addProperty("timestamp", savedMessage.getTimestamp().toString());

                String dmJson = gson.toJson(dm);

                // Send to sender
                session.getBasicRemote().sendText(dmJson);

                // Send to recipient if online
                sendToUser(discussionId, recipientEmail, dmJson);

            } else {
                sendError(session, "Unknown message type: " + type);
            }

        } catch (Exception e) {
            log.error("[onMessage] Error: {}", e.getMessage(), e);
            sendError(session, "Failed to process message: " + e.getMessage());
        }
    }

    /**
     * Called when WebSocket connection is closed
     */
    @OnClose
    public void onClose(Session session) {
        try {
            Long discussionId = sessionDiscussionMap.get(session);
            String userEmail = sessionUserMap.get(session);

            if (discussionId != null && userEmail != null) {
                log.info("[onClose] User {} disconnected from discussion {}", userEmail, discussionId);

                // Remove from maps
                Map<String, Session> sessions = discussionSessions.get(discussionId);
                if (sessions != null) {
                    sessions.remove(userEmail);
                    if (sessions.isEmpty()) {
                        discussionSessions.remove(discussionId);
                    }
                }
                sessionDiscussionMap.remove(session);
                sessionUserMap.remove(session);

                // Update participant online status
                discussionService.updateParticipantStatus(discussionId, userEmail, false);

                // Broadcast user left
                Users user = userRepository.findByEmailId(userEmail);
                if (user != null) {
                    JsonObject userLeft = new JsonObject();
                    userLeft.addProperty("type", "USER_LEFT");
                    JsonObject userObj = new JsonObject();
                    userObj.addProperty("emailId", user.getEmailId());
                    userObj.addProperty("name", user.getName());
                    userLeft.add("user", userObj);
                    userLeft.addProperty("timestamp", LocalDateTime.now().toString());
                    userLeft.addProperty("participantCount",
                            discussionService.getParticipantCount(discussionId));

                    broadcastToDiscussion(discussionId, gson.toJson(userLeft), null);
                }
            }
        } catch (Exception e) {
            log.error("[onClose] Error: {}", e.getMessage(), e);
        }
    }

    /**
     * Called on WebSocket errors
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        String userEmail = sessionUserMap.get(session);
        Long discussionId = sessionDiscussionMap.get(session);
        log.error("[onError] Discussion: {}, User: {}, Error: {}",
                discussionId, userEmail, throwable.getMessage());
    }

    // Helper methods

    private String getTokenFromSession(Session session) {
        Map<String, List<String>> params = session.getRequestParameterMap();
        List<String> tokens = params.get("token");
        return (tokens != null && !tokens.isEmpty()) ? tokens.get(0) : null;
    }

    private void sendError(Session session, String errorMessage) {
        try {
            JsonObject error = new JsonObject();
            error.addProperty("type", "ERROR");
            error.addProperty("message", errorMessage);
            session.getBasicRemote().sendText(gson.toJson(error));
        } catch (IOException e) {
            log.error("[sendError] Failed to send error message", e);
        }
    }

    private void broadcastToDiscussion(Long discussionId, String message, String excludeUser) {
        Map<String, Session> sessions = discussionSessions.get(discussionId);
        if (sessions != null) {
            sessions.forEach((email, session) -> {
                if (excludeUser == null || !email.equals(excludeUser)) {
                    try {
                        if (session.isOpen()) {
                            session.getBasicRemote().sendText(message);
                        }
                    } catch (IOException e) {
                        log.error("[broadcast] Failed to send to {}: {}", email, e.getMessage());
                    }
                }
            });
        }
    }

    private void sendToUser(Long discussionId, String userEmail, String message) {
        Map<String, Session> sessions = discussionSessions.get(discussionId);
        if (sessions != null) {
            Session session = sessions.get(userEmail);
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.error("[sendToUser] Failed to send to {}: {}", userEmail, e.getMessage());
                }
            }
        }
    }
}