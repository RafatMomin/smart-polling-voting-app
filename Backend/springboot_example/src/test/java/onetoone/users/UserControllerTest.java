package onetoone.users;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import onetoone.Users.controller.UserController;
import onetoone.Users.model.Users;
import onetoone.Users.repository.UserRepository;
import onetoone.Users.service.MailingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailingService mailingService; // for completeness, matches controller field

    @InjectMocks
    private UserController controller;

    private Users user;

    @Before
    public void setup() {
        user = new Users();
        user.setEmailId("user@x.com");
        user.setName("User");
        user.setPassword("oldpass");
    }

    /*
     * USERS FEATURE TEST: getUserById returns message when user not found.
     */
    @Test
    public void getUserById_notFound_returnsMessage() {
        when(userRepository.findByEmailId("missing@x.com")).thenReturn(null);

        String res = controller.getUserById("missing@x.com");
        assertEquals("{\"message\":\"No user with the given email.\"}", res);
    }

    /*
     * USERS FEATURE TEST: getUserById returns user email when found.
     */
    @Test
    public void getUserById_found_returnsUserJson() {
        when(userRepository.findByEmailId("user@x.com")).thenReturn(user);

        String res = controller.getUserById("user@x.com");
        assertEquals("{\"user\":\"user@x.com\"}", res);
    }

    /*
     * USERS FEATURE TEST: updateUser not found returns error message.
     */
    @Test
    public void updateUser_notFound_returnsMessage() {
        when(userRepository.findByEmailId("missing@x.com")).thenReturn(null);

        String res = controller.updateUser("missing@x.com", new Users());
        assertEquals("{\"message\":\"No user with the given email.\"}", res);
    }

    /*
     * USERS FEATURE TEST: updateUser updates only non-null fields and saves.
     */
    @Test
    public void updateUser_updatesFieldsAndSaves() {
        when(userRepository.findByEmailId("user@x.com")).thenReturn(user);

        Users updated = new Users();
        updated.setName("New Name");
        updated.setPassword("newpass");
        // email remains null -> should not change

        String res = controller.updateUser("user@x.com", updated);

        assertEquals("{\"message\":\"User updated!\"}", res);
        assertEquals("New Name", user.getName());
        assertEquals("newpass", user.getPassword());
        assertEquals("user@x.com", user.getEmailId());
        verify(userRepository).save(user);
    }

    /*
     * USERS FEATURE TEST: generateSixDigitToken always returns a 6-digit numeric string.
     * uses reflection to call the private helper since it's part of your logic.
     */
    @Test
    public void generateSixDigitToken_returnsSixDigitNumeric() throws Exception {
        java.lang.reflect.Method m =
                UserController.class.getDeclaredMethod("generateSixDigitToken");
        m.setAccessible(true);

        String token = (String) m.invoke(controller);

        assertEquals(6, token.length());
        assertTrue(token.matches("\\d{6}"));
    }
}