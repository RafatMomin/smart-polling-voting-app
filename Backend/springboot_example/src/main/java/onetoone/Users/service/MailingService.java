package onetoone.Users.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailingService {

    private final JavaMailSender mailSender;

    public MailingService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // HTML template with logo and light/dark mode support
    private String buildBaseTemplate(String pageTitle, String bodyContent) {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>%s</title>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <meta name="color-scheme" content="light dark">
            <meta name="supported-color-schemes" content="light dark">
            <style>
                @media (prefers-color-scheme: dark) {
                    body { background-color: #0b0b10 !important; color: #f5f5f5 !important; }
                    .email-wrapper { background-color: #0b0b10 !important; }
                    .email-card { background-color: #171821 !important; border-color: #2b2b3c !important; }
                    .email-footer { border-top-color: #2b2b3c !important; color: #aaaaaa !important; }
                    .highlight { background-color: #25263a !important; color: #f5f5f5 !important; border-left-color: #7c82ff !important; }
                }
            </style>
        </head>
        <body style="margin:0;padding:0;background-color:#f4f4f4; font-family:Arial,sans-serif;color:#111111;">
            <div class="email-wrapper" style="padding:20px;">
                <div class="email-card" style="max-width:600px;margin:0 auto; background-color:#ffffff;border:1px solid #e5e5e5; border-radius:8px;overflow:hidden;">

                    <div style="background:linear-gradient(90deg,#2b4eff,#6c63ff); padding:14px 20px;display:flex;align-items:center;">
                        <img src="cid:appLogo" alt="Voting & Polling Logo" style="height:32px;width:auto;margin-right:10px;display:block;">
                        <span style="font-size:18px;color:#ffffff;font-weight:bold;">Smart Voting &amp; Polling</span>
                    </div>

                    <div style="padding:20px;line-height:1.5;font-size:14px;">
                        %s
                    </div>

                    <div class="email-footer" style="padding:16px 20px; border-top:1px solid #e5e5e5; font-size:12px;color:#777777;">
                        Need help? Contact us at
                        <a href="mailto:votingandpolling@gmail.com" style="color:#2b4eff;text-decoration:none;">
                            votingandpolling@gmail.com
                        </a>
                    </div>

                </div>
            </div>
        </body>
        </html>
        """.formatted(pageTitle, bodyContent);
    }

    // add logo
    private void addLogo(MimeMessageHelper helper) throws MessagingException {
        ClassPathResource logo = new ClassPathResource("email/logo.png");
        helper.addInline("appLogo", logo, "image/png");
    }

    // safe send emails, wrap failures
    private void safeSend(java.util.function.Consumer<MimeMessageHelper> emailBuilder) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            emailBuilder.accept(helper);
            addLogo(helper);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    // verification email
    public void sendVerificationEmail(String to, String token) {
        safeSend(helper -> {
            try {
                helper.setTo(to);
                helper.setSubject("Verify Your Email – Smart Voting & Polling");

                String body = """
                        <h2 style="margin-top:0;margin-bottom:12px;font-size:22px;">
                            Welcome to Smart Voting &amp; Polling!
                        </h2>
                        <p style="margin:0 0 12px 0;">
                            Thank you for signing up. Please verify your account using the code below:
                        </p>
                        
                        <div class="highlight"
                             style="background-color:#f4f6ff;padding:12px 16px;border-radius:4px;
                                    border-left:4px solid #2b4eff;font-size:18px;font-weight:bold;
                                    display:inline-block;margin:8px 0;">
                        """ + token + """
                            </div>
                        
                            <p style="margin:16px 0 8px 0;">
                                Enter this code in the app to complete your registration.
                            </p>
                            <p style="margin:0 0 16px 0;">
                                If you did not create an account, you can safely ignore this email.
                            </p>
                            <p style="margin:0;">
                                Best regards,<br>
                                The Smart Voting &amp; Polling Team
                            </p>
                        """;

                String html = buildBaseTemplate("Verify your email", body);
                helper.setText(html, true);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // password reset email
    public void sendPasswordResetEmail(String to, String resetToken) {
        safeSend(helper -> {
            try {
                helper.setTo(to);
                helper.setSubject("Password Reset Instructions – Smart Voting & Polling");

                String body = """
                        <h2 style="margin-top:0;margin-bottom:12px;font-size:22px;">
                            Password Reset Request
                        </h2>
                        <p style="margin:0 0 12px 0;">
                            We received a request to reset the password for your Smart Voting &amp; Polling account.
                        </p>
                        <p style="margin:0 0 8px 0;">
                            Use the code below to create a new password:
                        </p>
                        
                        <div class="highlight"
                             style="background-color:#fff3cd;padding:12px 16px;border-radius:4px;
                                    border-left:4px solid #ffcc00;font-size:18px;font-weight:bold;
                                    display:inline-block;margin:8px 0;">
                        """ + resetToken + """
                            </div>
                        
                            <p style="margin:16px 0 8px 0;">
                                Enter this code in the app to update your password.
                            </p>
                            <p style="margin:0 0 16px 0;">
                                If you did not request a password reset, you can ignore this email and your password will stay the same.
                            </p>
                            <p style="margin:0;">
                                Best regards,<br>
                                The Smart Voting &amp; Polling Team
                            </p>
                        """;

                String html = buildBaseTemplate("Password reset", body);
                helper.setText(html, true);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // private poll access code email
    public void sendPollAccessCodeEmail(String to, String pollTitle, String accessCode) {
        safeSend(helper -> {
            try {
                helper.setTo(to);
                helper.setSubject("Your Private Poll Access Code – " + pollTitle);

                String body = """
                        <h2 style="margin-top:0;margin-bottom:12px;font-size:22px;">
                            Your Private Poll Is Ready
                        </h2>
                        <p style="margin:0 0 8px 0;">
                            You successfully created a private poll:
                        </p>
                        <p style="margin:0 0 16px 0;font-size:18px;font-weight:bold;">
                        """ + pollTitle + """
                        </p>
                        
                        <p style="margin:0 0 8px 0;">
                            Your access code is:
                        </p>
                        
                        <div class="highlight"
                             style="background-color:#e8ffe8;padding:12px 16px;border-radius:4px;
                                    border-left:4px solid #28a745;font-size:18px;font-weight:bold;
                                    display:inline-block;margin:8px 0;">
                        """ + accessCode + """
                            </div>
                        
                            <p style="margin:16px 0 8px 0;">
                                Share this code with the participants you want to invite. They can use it in the app
                                to join and vote in your poll.
                            </p>
                            <p style="margin:0 0 16px 0;">
                                Important: Only share this code with people you trust to access this poll.
                            </p>
                            <p style="margin:0;">
                                Best regards,<br>
                                The Smart Voting &amp; Polling Team
                            </p>
                        """;

                String html = buildBaseTemplate("Private poll access code", body);
                helper.setText(html, true);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });
    }
}