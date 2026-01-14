package onetoone.PollingWindow.service;

import onetoone.PollingWindow.model.Group;
import onetoone.PollingWindow.model.PollingWindow;
import onetoone.PollingWindow.repository.GroupRepository;
import onetoone.PollingWindow.repository.PollingWindowRepository;
import onetoone.Polling.model.Poll;
import onetoone.Polling.repository.PollRepository;
import onetoone.Users.model.Users;
import onetoone.Users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
public class PollingWindowService {

    @Autowired
    private PollingWindowRepository pollingWindowRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new polling window for a poll
     */
    @Transactional
    public PollingWindow createPollingWindow(
            Long pollId,
            String windowName,
            String creatorEmail,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Set<Long> groupIds,
            Set<String> individualUserEmails) {

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new NoSuchElementException("Poll not found"));

        Users creator = userRepository.findByEmailId(creatorEmail);
        if (creator == null) {
            throw new IllegalArgumentException("Creator user not found: " + creatorEmail);
        }

        PollingWindow window = new PollingWindow(poll, windowName, creator);

        if (startTime != null) {
            window.setStartTime(startTime);
        }
        if (endTime != null) {
            window.setEndTime(endTime);
        }

        // Add target groups
        if (groupIds != null && !groupIds.isEmpty()) {
            for (Long groupId : groupIds) {
                Group group = groupRepository.findById(groupId)
                        .orElseThrow(() -> new NoSuchElementException("Group not found: " + groupId));
                window.addTargetGroup(group);
            }
        }

        // Add individual users
        if (individualUserEmails != null && !individualUserEmails.isEmpty()) {
            for (String email : individualUserEmails) {
                Users user = userRepository.findByEmailId(email);
                if (user == null) {
                    throw new IllegalArgumentException("User not found: " + email);
                }
                window.addTargetUser(user);
            }
        }

        PollingWindow savedWindow = pollingWindowRepository.save(window);

        // Initialize lazy collections before returning
        initializeWindow(savedWindow);

        return savedWindow;
    }

    /**
     * Open/activate a polling window
     * Returns error message if cannot open (outside time window)
     */
    @Transactional
    public PollingWindow openPollingWindow(Long windowId) {
        PollingWindow window = pollingWindowRepository.findById(windowId)
                .orElseThrow(() -> new NoSuchElementException("Polling window not found"));

        // Check if within time boundaries
        if (!window.isWithinTimeWindow()) {
            LocalDateTime now = LocalDateTime.now();
            if (window.getStartTime() != null && now.isBefore(window.getStartTime())) {
                throw new IllegalStateException(
                        "Cannot open window before start time. Start time: " + window.getStartTime()
                );
            }
            if (window.getEndTime() != null && now.isAfter(window.getEndTime())) {
                throw new IllegalStateException(
                        "Cannot open window after end time. End time: " + window.getEndTime()
                );
            }
        }

        // Close all other active windows for this poll
        List<PollingWindow> activeWindows = pollingWindowRepository
                .findActiveWindowsByPollId(window.getPoll().getId());
        for (PollingWindow activeWindow : activeWindows) {
            if (!activeWindow.getId().equals(windowId)) {
                activeWindow.close();
                pollingWindowRepository.save(activeWindow);
            }
        }

        // Open this window using the model's open() method
        if (!window.open()) {
            throw new IllegalStateException("Cannot open window: outside time boundaries");
        }

        PollingWindow savedWindow = pollingWindowRepository.save(window);

        // Initialize lazy collections
        initializeWindow(savedWindow);

        return savedWindow;
    }

    /**
     * Force open a window (admin override - bypasses time restrictions)
     */
    @Transactional
    public PollingWindow forceOpenPollingWindow(Long windowId) {
        PollingWindow window = pollingWindowRepository.findById(windowId)
                .orElseThrow(() -> new NoSuchElementException("Polling window not found"));

        // Close all other active windows for this poll
        List<PollingWindow> activeWindows = pollingWindowRepository
                .findActiveWindowsByPollId(window.getPoll().getId());
        for (PollingWindow activeWindow : activeWindows) {
            if (!activeWindow.getId().equals(windowId)) {
                activeWindow.close();
                pollingWindowRepository.save(activeWindow);
            }
        }

        // Force open this window (bypasses time checks)
        window.forceOpen();
        PollingWindow savedWindow = pollingWindowRepository.save(window);

        // Initialize lazy collections
        initializeWindow(savedWindow);

        return savedWindow;
    }

    /**
     * Close a polling window
     */
    @Transactional
    public PollingWindow closePollingWindow(Long windowId) {
        PollingWindow window = pollingWindowRepository.findById(windowId)
                .orElseThrow(() -> new NoSuchElementException("Polling window not found"));

        window.close();
        PollingWindow savedWindow = pollingWindowRepository.save(window);

        // Initialize lazy collections
        initializeWindow(savedWindow);

        return savedWindow;
    }

    /**
     * Check if a user can vote on a poll (via any active window)
     */
    @Transactional(readOnly = true)
    public boolean canUserVoteOnPoll(Long pollId, String userEmail) {
        List<PollingWindow> activeWindows = pollingWindowRepository.findActiveWindowsByPollId(pollId);

        for (PollingWindow window : activeWindows) {
            // Initialize collections for checking
            Hibernate.initialize(window.getTargetGroups());
            Hibernate.initialize(window.getTargetIndividualUsers());

            if (window.canUserVote(userEmail)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the currently active window for a poll
     */
    @Transactional(readOnly = true)
    public Optional<PollingWindow> getCurrentActiveWindow(Long pollId) {
        Optional<PollingWindow> window = pollingWindowRepository.findCurrentActiveWindow(pollId);
        window.ifPresent(this::initializeWindow);
        return window;
    }

    /**
     * Get all windows for a poll
     */
    @Transactional(readOnly = true)
    public List<PollingWindow> getWindowsForPoll(Long pollId) {
        List<PollingWindow> windows = pollingWindowRepository.findByPollId(pollId);
        windows.forEach(this::initializeWindow);
        return windows;
    }

    /**
     * Get a specific window
     */
    @Transactional(readOnly = true)
    public PollingWindow getWindowById(Long windowId) {
        PollingWindow window = pollingWindowRepository.findById(windowId)
                .orElseThrow(() -> new NoSuchElementException("Polling window not found"));
        initializeWindow(window);
        return window;
    }

    /**
     * Update window end time
     * If window is currently open and new end time has passed, it will auto-close
     */
    @Transactional
    public PollingWindow updateWindowEndTime(Long windowId, LocalDateTime newEndTime) {
        PollingWindow window = pollingWindowRepository.findById(windowId)
                .orElseThrow(() -> new NoSuchElementException("Polling window not found"));

        window.setEndTime(newEndTime);

        // If window is active but new end time has passed, close it
        if (window.isActive() && !window.isWithinTimeWindow()) {
            window.close();
        }

        PollingWindow savedWindow = pollingWindowRepository.save(window);

        // Initialize lazy collections
        initializeWindow(savedWindow);

        return savedWindow;
    }

    /**
     * Delete a polling window
     */
    @Transactional
    public void deletePollingWindow(Long windowId) {
        pollingWindowRepository.deleteById(windowId);
    }

    /**
     * Helper method to initialize lazy-loaded collections
     */
    private void initializeWindow(PollingWindow window) {
        // Initialize poll
        Hibernate.initialize(window.getPoll());

        // Initialize creator
        Hibernate.initialize(window.getCreator());

        // Initialize target groups and their members
        Hibernate.initialize(window.getTargetGroups());
        for (Group group : window.getTargetGroups()) {
            Hibernate.initialize(group.getMembers());
            Hibernate.initialize(group.getCreator());
        }

        // Initialize individual target users
        Hibernate.initialize(window.getTargetIndividualUsers());
    }
}