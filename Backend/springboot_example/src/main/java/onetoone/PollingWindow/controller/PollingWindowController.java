package onetoone.PollingWindow.controller;

import onetoone.PollingWindow.dto.PollingWindowDTO;
import onetoone.PollingWindow.model.PollingWindow;
import onetoone.PollingWindow.service.PollingWindowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/polling-windows")
public class PollingWindowController {

    @Autowired
    private PollingWindowService pollingWindowService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Create a new polling window
     */
    @PostMapping
    public ResponseEntity<?> createPollingWindow(@RequestBody Map<String, Object> request) {
        try {
            Long pollId = Long.valueOf(request.get("pollId").toString());
            String windowName = (String) request.get("windowName");
            String creatorEmail = (String) request.get("creatorEmail");

            if (pollId == null || windowName == null || creatorEmail == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "pollId, windowName, and creatorEmail are required"));
            }

            LocalDateTime startTime = null;
            LocalDateTime endTime = null;

            if (request.containsKey("startTime") && request.get("startTime") != null) {
                startTime = LocalDateTime.parse((String) request.get("startTime"), DATE_FORMATTER);
            }
            if (request.containsKey("endTime") && request.get("endTime") != null) {
                endTime = LocalDateTime.parse((String) request.get("endTime"), DATE_FORMATTER);
            }

            Set<Long> groupIds = null;
            if (request.containsKey("groupIds") && request.get("groupIds") != null) {
                @SuppressWarnings("unchecked")
                List<Integer> groupIdsList = (List<Integer>) request.get("groupIds");
                groupIds = groupIdsList.stream()
                        .map(Long::valueOf)
                        .collect(Collectors.toSet());
            }

            Set<String> individualUserEmails = null;
            if (request.containsKey("individualUserEmails") && request.get("individualUserEmails") != null) {
                @SuppressWarnings("unchecked")
                List<String> emailsList = (List<String>) request.get("individualUserEmails");
                individualUserEmails = new HashSet<>(emailsList);
            }

            PollingWindow window = pollingWindowService.createPollingWindow(
                    pollId, windowName, creatorEmail, startTime, endTime, groupIds, individualUserEmails
            );

            PollingWindowDTO dto = PollingWindowDTO.fromEntity(window);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Open/activate a polling window
     * Will fail if outside time boundaries
     */
    @PutMapping("/{windowId}/open")
    public ResponseEntity<?> openPollingWindow(@PathVariable Long windowId) {
        try {
            PollingWindow window = pollingWindowService.openPollingWindow(windowId);
            PollingWindowDTO dto = PollingWindowDTO.fromEntity(window);
            return ResponseEntity.ok(Map.of(
                    "message", "Polling window opened",
                    "window", dto,
                    "status", window.getWindowStatus()
            ));
        } catch (IllegalStateException e) {
            // Return 400 with specific error about time boundaries
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "canForceOpen", true,
                    "hint", "Use /force-open endpoint to override time restrictions"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Force open a window (admin override - bypasses time restrictions)
     */
    @PutMapping("/{windowId}/force-open")
    public ResponseEntity<?> forceOpenPollingWindow(@PathVariable Long windowId) {
        try {
            PollingWindow window = pollingWindowService.forceOpenPollingWindow(windowId);
            PollingWindowDTO dto = PollingWindowDTO.fromEntity(window);
            return ResponseEntity.ok(Map.of(
                    "message", "Polling window force-opened (time restrictions bypassed)",
                    "window", dto,
                    "status", window.getWindowStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Close a polling window
     */
    @PutMapping("/{windowId}/close")
    public ResponseEntity<?> closePollingWindow(@PathVariable Long windowId) {
        try {
            PollingWindow window = pollingWindowService.closePollingWindow(windowId);
            PollingWindowDTO dto = PollingWindowDTO.fromEntity(window);
            return ResponseEntity.ok(Map.of(
                    "message", "Polling window closed",
                    "window", dto,
                    "status", window.getWindowStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if a user can vote on a poll
     */
    @GetMapping("/poll/{pollId}/can-vote")
    public ResponseEntity<?> canUserVote(@PathVariable Long pollId, @RequestParam String userEmail) {
        try {
            boolean canVote = pollingWindowService.canUserVoteOnPoll(pollId, userEmail);

            // Get active window for additional context
            Optional<PollingWindow> activeWindow = pollingWindowService.getCurrentActiveWindow(pollId);

            Map<String, Object> response = new HashMap<>();
            response.put("canVote", canVote);
            response.put("pollId", pollId);
            response.put("userEmail", userEmail);

            if (activeWindow.isPresent()) {
                response.put("windowStatus", activeWindow.get().getWindowStatus());
                response.put("windowId", activeWindow.get().getId());
                response.put("withinTimeWindow", activeWindow.get().isWithinTimeWindow());
            } else {
                response.put("windowStatus", "NO_ACTIVE_WINDOW");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get the currently active window for a poll
     */
    @GetMapping("/poll/{pollId}/active")
    public ResponseEntity<?> getCurrentActiveWindow(@PathVariable Long pollId) {
        try {
            Optional<PollingWindow> window = pollingWindowService.getCurrentActiveWindow(pollId);
            if (window.isPresent()) {
                PollingWindowDTO dto = PollingWindowDTO.fromEntity(window.get());
                return ResponseEntity.ok(Map.of(
                        "window", dto,
                        "status", window.get().getWindowStatus(),
                        "withinTimeWindow", window.get().isWithinTimeWindow()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "message", "No active polling window",
                        "status", "NO_ACTIVE_WINDOW"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all windows for a poll with their statuses
     */
    @GetMapping("/poll/{pollId}")
    public ResponseEntity<?> getWindowsForPoll(@PathVariable Long pollId) {
        try {
            List<PollingWindow> windows = pollingWindowService.getWindowsForPoll(pollId);
            List<Map<String, Object>> windowsWithStatus = windows.stream()
                    .map(w -> {
                        Map<String, Object> windowData = new HashMap<>();
                        windowData.put("window", PollingWindowDTO.fromEntity(w));
                        windowData.put("status", w.getWindowStatus());
                        windowData.put("withinTimeWindow", w.isWithinTimeWindow());
                        windowData.put("isCurrentlyOpen", w.isCurrentlyOpen());
                        return windowData;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(windowsWithStatus);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get a specific window with status
     */
    @GetMapping("/{windowId}")
    public ResponseEntity<?> getWindow(@PathVariable Long windowId) {
        try {
            PollingWindow window = pollingWindowService.getWindowById(windowId);
            PollingWindowDTO dto = PollingWindowDTO.fromEntity(window);
            return ResponseEntity.ok(Map.of(
                    "window", dto,
                    "status", window.getWindowStatus(),
                    "withinTimeWindow", window.isWithinTimeWindow(),
                    "isCurrentlyOpen", window.isCurrentlyOpen()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update window end time
     */
    @PutMapping("/{windowId}/end-time")
    public ResponseEntity<?> updateEndTime(@PathVariable Long windowId, @RequestBody Map<String, String> request) {
        try {
            String endTimeStr = request.get("endTime");
            if (endTimeStr == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "endTime is required"));
            }

            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, DATE_FORMATTER);
            PollingWindow window = pollingWindowService.updateWindowEndTime(windowId, endTime);
            PollingWindowDTO dto = PollingWindowDTO.fromEntity(window);
            return ResponseEntity.ok(Map.of(
                    "window", dto,
                    "status", window.getWindowStatus(),
                    "message", "End time updated. Window auto-closed if new end time has passed."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a polling window
     */
    @DeleteMapping("/{windowId}")
    public ResponseEntity<?> deletePollingWindow(@PathVariable Long windowId) {
        try {
            pollingWindowService.deletePollingWindow(windowId);
            return ResponseEntity.ok(Map.of("message", "Polling window deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}