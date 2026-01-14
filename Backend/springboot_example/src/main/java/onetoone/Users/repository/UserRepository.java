package onetoone.Users.repository;

import onetoone.Users.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 
 * @author Vivek Bengre
 * 
 */ 

public interface UserRepository extends JpaRepository<Users, String> {
    Users findByVerificationToken(String token);
    Users findByResetToken(String token);
    Users findByEmailIdAndPassword(String emailId, String password);
    Users findByauthtoken(String authToken);
    Users findByEmailId(String email);
}
