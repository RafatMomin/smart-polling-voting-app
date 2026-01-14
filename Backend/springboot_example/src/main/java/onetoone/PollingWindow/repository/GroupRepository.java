package onetoone.PollingWindow.repository;

import onetoone.PollingWindow.model.Group;
import onetoone.Users.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    // Find groups created by a specific user
    List<Group> findByCreator(Users creator);

    // Find groups that contain a specific member
    @Query("SELECT g FROM Group g JOIN g.members m WHERE m = :user")
    List<Group> findGroupsByMember(@Param("user") Users user);
}