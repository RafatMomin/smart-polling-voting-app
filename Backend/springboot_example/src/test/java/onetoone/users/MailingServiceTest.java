package onetoone.users;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import onetoone.Users.service.MailingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@RunWith(MockitoJUnitRunner.class)
public class MailingServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    public void sendVerificationEmail_buildsCorrectMessage() {
        MailingService service = new MailingService(mailSender);

        service.sendVerificationEmail("user@x.com", "TOKEN123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertEquals("user@x.com", msg.getTo()[0]);
        assertTrue(msg.getSubject().contains("Verify Your Email"));
        assertTrue(msg.getText().contains("TOKEN123"));
    }

    @Test
    public void sendPasswordResetEmail_buildsCorrectMessage() {
        MailingService service = new MailingService(mailSender);

        service.sendPasswordResetEmail("user@x.com", "RESET123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertEquals("user@x.com", msg.getTo()[0]);
        assertTrue(msg.getSubject().contains("Password Reset"));
        assertTrue(msg.getText().contains("RESET123"));
    }

    @Test
    public void sendPollAccessCodeEmail_buildsCorrectMessage() {
        MailingService service = new MailingService(mailSender);

        service.sendPollAccessCodeEmail("user@x.com", "My Private Poll", "CODE123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertEquals("user@x.com", msg.getTo()[0]);
        assertTrue(msg.getSubject().contains("My Private Poll"));
        assertTrue(msg.getText().contains("CODE123"));
    }
}