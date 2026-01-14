package onetoone.PollingWindow.controller;

import onetoone.PollingWindow.dto.GroupResponseDTO;
import onetoone.PollingWindow.model.Group;
import onetoone.PollingWindow.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    /**
     * Create a new group
     */
    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            String creatorEmail = request.get("creatorEmail");

            if (name == null || creatorEmail == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name and creatorEmail are required"));
            }

            Group group = groupService.createGroup(name, description, creatorEmail);
            GroupResponseDTO dto = GroupResponseDTO.fromEntity(group);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Add a member to a group
     */
    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(@PathVariable Long groupId, @RequestBody Map<String, String> request) {
        try {
            String memberEmail = request.get("memberEmail");
            if (memberEmail == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "memberEmail is required"));
            }

            Group group = groupService.addMemberToGroup(groupId, memberEmail);
            GroupResponseDTO dto = GroupResponseDTO.fromEntity(group);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Add multiple members to a group
     */
    @PostMapping("/{groupId}/members/batch")
    public ResponseEntity<?> addMultipleMembers(@PathVariable Long groupId, @RequestBody Map<String, Set<String>> request) {
        try {
            Set<String> memberEmails = request.get("memberEmails");
            if (memberEmails == null || memberEmails.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "memberEmails are required"));
            }

            Group group = groupService.addMultipleMembersToGroup(groupId, memberEmails);
            GroupResponseDTO dto = GroupResponseDTO.fromEntity(group);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remove a member from a group
     */
    @DeleteMapping("/{groupId}/members/{memberEmail}")
    public ResponseEntity<?> removeMember(@PathVariable Long groupId, @PathVariable String memberEmail) {
        try {
            Group group = groupService.removeMemberFromGroup(groupId, memberEmail);
            GroupResponseDTO dto = GroupResponseDTO.fromEntity(group);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all groups created by a user
     */
    @GetMapping("/creator/{email}")
    public ResponseEntity<?> getGroupsByCreator(@PathVariable String email) {
        try {
            List<Group> groups = groupService.getGroupsByCreator(email);
            List<GroupResponseDTO> dtos = groups.stream()
                    .map(GroupResponseDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all groups a user is a member of
     */
    @GetMapping("/member/{email}")
    public ResponseEntity<?> getGroupsForUser(@PathVariable String email) {
        try {
            List<Group> groups = groupService.getGroupsForUser(email);
            List<GroupResponseDTO> dtos = groups.stream()
                    .map(GroupResponseDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get a specific group
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroup(@PathVariable Long groupId) {
        try {
            Group group = groupService.getGroupById(groupId);
            GroupResponseDTO dto = GroupResponseDTO.fromEntity(group);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all groups
     */
    @GetMapping
    public ResponseEntity<?> getAllGroups() {
        try {
            List<Group> groups = groupService.getAllGroups();
            List<GroupResponseDTO> dtos = groups.stream()
                    .map(GroupResponseDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a group
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId) {
        try {
            groupService.deleteGroup(groupId);
            return ResponseEntity.ok(Map.of("message", "Group deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}