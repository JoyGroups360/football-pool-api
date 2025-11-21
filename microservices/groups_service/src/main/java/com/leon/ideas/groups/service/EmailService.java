package com.leon.ideas.groups.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.download-url:https://your-app-download-url.com}")
    private String appDownloadUrl;

    @Value("${app.deep-link-android-dev:exp://10.0.2.2:8081/--/group-details}")
    private String deepLinkAndroidDev;

    @Value("${app.deep-link-ios-dev:exp://localhost:8081/--/group-details}")
    private String deepLinkIosDev;

    @Value("${app.deep-link-production:football-pool://group-details}")
    private String deepLinkProduction;

    public boolean sendGroupInvitation(String toEmail, String groupId, String groupName, String inviterName, String competitionName) {
        try {
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("📧 EMAIL SERVICE - Starting invitation email");
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("📧 To: " + toEmail);
            System.out.println("📧 From: " + fromEmail);
            System.out.println("📧 Group ID: " + groupId);
            System.out.println("📧 Group name: " + groupName);
            System.out.println("📧 Inviter: " + inviterName);
            System.out.println("📧 Competition: " + competitionName);
            System.out.println("📧 MailSender configured: " + (mailSender != null ? "YES" : "NO"));
            
            if (mailSender == null) {
                System.err.println("❌ CRITICAL ERROR: JavaMailSender is NULL!");
                return false;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("Football Pool - Groups <" + fromEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject("🎉 You've been invited to join a Football Pool group!");

            String htmlContent = buildInvitationEmailHtml(groupName, inviterName, competitionName, groupId);
            helper.setText(htmlContent, true);

            System.out.println("📧 Attempting to send message via JavaMailSender...");
            mailSender.send(message);
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("✅ SUCCESS: Group invitation email sent to: " + toEmail);
            System.out.println("═══════════════════════════════════════════════════════");
            return true;
        } catch (jakarta.mail.AuthenticationFailedException e) {
            System.err.println("❌ Authentication failed when sending email to: " + toEmail);
            System.err.println("❌ Error details: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (jakarta.mail.MessagingException e) {
            System.err.println("❌ Messaging error when sending email to: " + toEmail);
            System.err.println("❌ Error details: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error when sending email to: " + toEmail);
            System.err.println("❌ Error type: " + e.getClass().getName());
            System.err.println("❌ Error message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String buildInvitationEmailHtml(String groupName, String inviterName, String competitionName, String groupId) {
        // Build deep linking URLs for group details
        String androidDevLink = deepLinkAndroidDev + "?id=" + groupId;
        String iosDevLink = deepLinkIosDev + "?id=" + groupId;
        String productionLink = deepLinkProduction + "?id=" + groupId;

        // Classic table-based HTML for maximum email client compatibility
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Football Pool Invitation</title>
            </head>
            <body style="margin:0; padding:0; background-color:#f4f4f4;">
                <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f4f4f4; padding:20px 0;">
                    <tr>
                        <td align="center">
                            <table width="600" border="0" cellspacing="0" cellpadding="0" style="background-color:#ffffff; border-radius:10px; overflow:hidden;">
                                <!-- Header -->
                                <tr>
                                    <td align="center" bgcolor="#10B981" style="padding:30px 20px; color:#ffffff; font-family:Arial, Helvetica, sans-serif;">
                                        <h1 style="margin:0; font-size:26px; font-weight:bold;">⚽ Football Pool Invitation</h1>
                                    </td>
                                </tr>

                                <!-- Content -->
                                <tr>
                                    <td align="left" style="padding:30px; font-family:Arial, Helvetica, sans-serif; color:#34495e; font-size:14px; line-height:1.6;">
                                        <p style="margin:0 0 15px 0;">¡Hola!</p>
                                        <p style="margin:0 0 15px 0;">
                                            <strong style="color:#059669;">%s</strong> te ha invitado a unirte a su grupo de Football Pool:
                                        </p>

                                        <!-- Group Info Box -->
                                        <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f8f9fa; border-radius:8px; margin:20px 0; border-left:4px solid #10B981;">
                                            <tr>
                                                <td style="padding:15px 20px; font-family:Arial, Helvetica, sans-serif; color:#2c3e50; font-size:14px;">
                                                    <h2 style="margin:0 0 10px 0; font-size:20px; font-weight:bold;">%s</h2>
                                                    <p style="margin:0;"><strong>Competencia:</strong> %s</p>
                                                </td>
                                            </tr>
                                        </table>

                                        <p style="margin:0 0 20px 0;">
                                            ¡Únete ahora para competir con tus amigos, hacer predicciones y subir en la tabla de clasificación!
                                        </p>

                                        <!-- Download App Button -->
                                        <table border="0" cellspacing="0" cellpadding="0" align="center" style="margin:20px 0;">
                                            <tr>
                                                <td align="center" bgcolor="#10B981" style="border-radius:8px;">
                                                    <a href="%s" target="_blank"
                                                       style="display:inline-block; padding:12px 30px; font-family:Arial, Helvetica, sans-serif; font-size:16px; color:#ffffff; text-decoration:none; font-weight:bold;">
                                                        📱 Descargar App
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>

                                        <!-- Deep Link Buttons -->
                                        <p style="margin:20px 0 10px 0; padding-top:10px; border-top:1px solid #e0e0e0;">
                                            <strong>¿Ya tienes la app?</strong><br>
                                            Haz clic en el enlace correspondiente a tu dispositivo:
                                        </p>

                                        <table border="0" cellspacing="0" cellpadding="0" align="center" style="margin:10px 0;">
                                            <tr>
                                                <td align="center" bgcolor="#059669" style="border-radius:6px; padding:0 5px;">
                                                    <a href="%s" target="_blank"
                                                       style="display:inline-block; padding:10px 20px; font-family:Arial, Helvetica, sans-serif; font-size:14px; color:#ffffff; text-decoration:none; font-weight:bold;">
                                                        🤖 Abrir en Android (Dev)
                                                    </a>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td height="10"></td>
                                            </tr>
                                            <tr>
                                                <td align="center" bgcolor="#059669" style="border-radius:6px; padding:0 5px;">
                                                    <a href="%s" target="_blank"
                                                       style="display:inline-block; padding:10px 20px; font-family:Arial, Helvetica, sans-serif; font-size:14px; color:#ffffff; text-decoration:none; font-weight:bold;">
                                                        🍎 Abrir en iOS (Dev)
                                                    </a>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td height="10"></td>
                                            </tr>
                                            <tr>
                                                <td align="center" bgcolor="#059669" style="border-radius:6px; padding:0 5px;">
                                                    <a href="%s" target="_blank"
                                                       style="display:inline-block; padding:10px 20px; font-family:Arial, Helvetica, sans-serif; font-size:14px; color:#ffffff; text-decoration:none; font-weight:bold;">
                                                        🚀 Abrir en App (Producción)
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>

                                        <p style="margin:20px 0 10px 0; padding-top:10px; border-top:1px solid #e0e0e0;">
                                            <strong>¿No tienes cuenta aún?</strong><br>
                                            No te preocupes, podrás crear una cuando descargues la app o hagas clic en los enlaces de arriba.
                                        </p>

                                        <!-- Raw Links -->
                                        <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f8f9fa; border-radius:8px; padding:10px; margin-top:10px;">
                                            <tr>
                                                <td style="padding:15px; font-family:Arial, Helvetica, sans-serif; font-size:12px; color:#7f8c8d;">
                                                    <strong>Enlaces directos (copia y pega):</strong><br><br>
                                                    <strong>Android (Dev):</strong><br>
                                                    <span style="word-break:break-all;">%s</span><br><br>
                                                    <strong>iOS (Dev):</strong><br>
                                                    <span style="word-break:break-all;">%s</span><br><br>
                                                    <strong>Producción:</strong><br>
                                                    <span style="word-break:break-all;">%s</span>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td align="center" bgcolor="#f8f9fa" style="padding:15px; font-family:Arial, Helvetica, sans-serif; font-size:12px; color:#7f8c8d; border-top:1px solid #e0e0e0;">
                                        Este es un correo automático de Football Pool. Por favor no respondas a este mensaje.
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(
                inviterName,
                groupName,
                competitionName,
                appDownloadUrl,
                androidDevLink,
                iosDevLink,
                productionLink,
                androidDevLink,
                iosDevLink,
                productionLink
            );
    }
}


