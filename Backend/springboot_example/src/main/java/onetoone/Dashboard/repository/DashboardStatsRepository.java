package onetoone.Dashboard.repository;

import onetoone.Dashboard.model.DashboardStats;
import onetoone.Users.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DashboardStatsRepository extends JpaRepository<DashboardStats, Long> {

    Optional<DashboardStats> findByUser(Users user);

}
