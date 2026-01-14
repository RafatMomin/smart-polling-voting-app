package onetoone.PollingWindow.service;

import onetoone.PollingWindow.model.Group;
import onetoone.PollingWindow.repository.GroupRepository;
import onetoone.Users.model.Users;
import onetoone.Users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Group createGroup(String name, String description, String creatorEmail) {
        Users creator = userRepository.findByEmailId(creatorEmail);
        if (creator == null) {
            throw new IllegalArgumentException("Creator user not found: " + creatorEmail);
        }

        Group group = new Group(name, creator);
        group.setDescription(description);
        Group savedGroup = groupRepository.save(group);

        // Initialize lazy collections
        initializeGroup(savedGroup);

        return savedGroup;
    }

    @Transactional
    public Group addMemberToGroup(Long groupId, String memberEmail) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found"));

        Users user = userRepository.findByEmailId(memberEmail);
        if (user == null) {
            throw new IllegalArgumentException("User with email " + memberEmail + " does not exist");
        }

        group.addMember(user);
        Group savedGroup = groupRepository.save(group);

        // Initialize lazy collections
        initializeGroup(savedGroup);

        return savedGroup;
    }

    @Transactional
    public Group addMultipleMembersToGroup(Long groupId, Set<String> memberEmails) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found"));

        for (String email : memberEmails) {
            Users user = userRepository.findByEmailId(email);
            if (user != null) {
                group.addMember(user);
            }
        }

        Group savedGroup = groupRepository.save(group);

        // Initialize lazy collections
        initializeGroup(savedGroup);

        return savedGroup;
    }

    @Transactional
    public Group removeMemberFromGroup(Long groupId, String memberEmail) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found"));

        Users user = userRepository.findByEmailId(memberEmail);
        if (user != null) {
            group.removeMember(user);
        }

        Group savedGroup = groupRepository.save(group);

        // Initialize lazy collections
        initializeGroup(savedGroup);

        return savedGroup;
    }

    @Transactional(readOnly = true)
    public List<Group> getGroupsByCreator(String creatorEmail) {
        Users creator = userRepository.findByEmailId(creatorEmail);
        if (creator == null) {
            throw new IllegalArgumentException("User not found: " + creatorEmail);
        }
        List<Group> groups = groupRepository.findByCreator(creator);
        groups.forEach(this::initializeGroup);
        return groups;
    }

    @Transactional(readOnly = true)
    public List<Group> getGroupsForUser(String userEmail) {
        Users user = userRepository.findByEmailId(userEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userEmail);
        }
        List<Group> groups = groupRepository.findGroupsByMember(user);
        groups.forEach(this::initializeGroup);
        return groups;
    }

    @Transactional(readOnly = true)
    public Group getGroupById(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found"));
        initializeGroup(group);
        return group;
    }

    @Transactional(readOnly = true)
    public List<Group> getAllGroups() {
        List<Group> groups = groupRepository.findAll();
        groups.forEach(this::initializeGroup);
        return groups;
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        groupRepository.deleteById(groupId);
    }

    /**
     * Helper method to initialize lazy-loaded collections
     */
    private void initializeGroup(Group group) {
        Hibernate.initialize(group.getCreator());
        Hibernate.initialize(group.getMembers());
    }
}