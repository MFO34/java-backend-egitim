package com.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * E-POSTA SERVİSİ
 * ===============
 * JavaMailSender kullanarak HTML e-posta gönderir.
 *
 * Kullanım senaryoları:
 *   1. E-posta doğrulama: Kayıt sonrası "Hesabınızı Doğrulayın" e-postası
 *   2. Şifre sıfırlama: "Şifremi Unuttum" e-postası
 *
 * @Async nedir?
 *   E-posta gönderme yavaş bir işlemdir (SMTP sunucusuna bağlanmak gerekir).
 *   @Async ile bu metodu ayrı bir thread'de çalıştırırız.
 *   Böylece kullanıcı e-posta gönderilmesini beklemez → hızlı yanıt alır.
 *
 *   Dikkat: @Async çalışması için @EnableAsync gerekir.
 *   AuthApplication'a ekleyeceğiz.
 *
 * application.yml gereksinimler:
 *   spring.mail.host: smtp.gmail.com
 *   spring.mail.port: 587
 *   spring.mail.username: your@gmail.com
 *   spring.mail.password: app-specific-password  (Gmail 2FA etkinse)
 *   spring.mail.properties.mail.smtp.starttls.enable: true
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    // Spring Boot'un otomatik yapılandırdığı mail gönderici
    private final JavaMailSender mailSender;

    // application.yml'den: app.base-url (örn: http://localhost:8080)
    @Value("${app.base-url}")
    private String baseUrl;

    // E-postanın "gönderen" adresi
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * E-POSTA DOĞRULAMA E-POSTASI GÖNDER
     * ====================================
     * Kayıt olan kullanıcıya doğrulama linki gönderir.
     *
     * Link: http://localhost:8080/api/v1/auth/verify-email?token=<uuid>
     * Süre: 24 saat
     *
     * @Async: E-posta gönderimi ayrı thread'de, kullanıcı beklemez
     */
    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String token) {
        try {
            // Doğrulama URL'ini oluştur
            var verificationUrl = baseUrl + "/api/v1/auth/verify-email?token=" + token;

            // HTML e-posta içeriği oluştur
            var subject = "Hesabınızı Doğrulayın - Auth Service";
            var htmlContent = buildVerificationEmailHtml(firstName, verificationUrl);

            // E-postayı gönder
            sendHtmlEmail(toEmail, subject, htmlContent);

            log.info("Doğrulama e-postası gönderildi: {}", toEmail);

        } catch (Exception e) {
            // E-posta gönderilemedi — loglayıp devam et (kullanıcıyı bloklamayalım)
            log.error("Doğrulama e-postası gönderilemedi: {} → {}", toEmail, e.getMessage());
        }
    }

    /**
     * ŞİFRE SIFIRLAMA E-POSTASI GÖNDER
     * ==================================
     * "Şifremi Unuttum" isteğinde reset linki gönderir.
     *
     * Link: http://localhost:3000/reset-password?token=<uuid>  (frontend URL!)
     * Süre: 1 saat
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String token) {
        try {
            // Frontend reset sayfasına yönlendirir (backend endpoint değil!)
            // Frontend bu token'ı alır ve POST /api/v1/auth/reset-password ile gönderir
            var resetUrl = "http://localhost:3000/reset-password?token=" + token;

            var subject = "Şifre Sıfırlama İsteği - Auth Service";
            var htmlContent = buildPasswordResetEmailHtml(firstName, resetUrl);

            sendHtmlEmail(toEmail, subject, htmlContent);

            log.info("Şifre sıfırlama e-postası gönderildi: {}", toEmail);

        } catch (Exception e) {
            log.error("Şifre sıfırlama e-postası gönderilemedi: {} → {}", toEmail, e.getMessage());
        }
    }

    /**
     * HTML E-POSTA GÖNDER
     * ===================
     * MimeMessageHelper ile HTML destekli e-posta oluşturur ve gönderir.
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws Exception {
        // MimeMessage: HTML destekli e-posta nesnesi
        var message = mailSender.createMimeMessage();

        // MimeMessageHelper: MimeMessage'ı kolayca doldurmak için yardımcı sınıf
        // true → multipart (HTML + text), "UTF-8" → Türkçe karakter desteği
        var helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);  // Gönderen: your@gmail.com
        helper.setTo(to);           // Alıcı
        helper.setSubject(subject); // Konu
        helper.setText(htmlContent, true); // HTML içerik (true = HTML modunda)

        // SMTP sunucusuna gönder
        mailSender.send(message);
    }

    /**
     * Doğrulama e-postası HTML içeriğini oluştur.
     * Basit ama güzel görünen bir HTML template.
     */
    private String buildVerificationEmailHtml(String firstName, String verificationUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 40px auto; background: white; border-radius: 8px;
                                 padding: 40px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; margin-bottom: 30px; }
                    .header h1 { color: #2c3e50; }
                    .button { display: inline-block; padding: 14px 28px; background-color: #3498db;
                              color: white; text-decoration: none; border-radius: 5px; font-size: 16px; }
                    .button:hover { background-color: #2980b9; }
                    .footer { margin-top: 30px; text-align: center; color: #7f8c8d; font-size: 12px; }
                    .warning { color: #e74c3c; font-size: 13px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Auth Service</h1>
                    </div>
                    <p>Merhaba <strong>%s</strong>,</p>
                    <p>Hesabınızı oluşturduğunuz için teşekkürler! E-posta adresinizi doğrulamak için
                       aşağıdaki butona tıklayın:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" class="button">E-Postamı Doğrula</a>
                    </div>
                    <p class="warning">⚠️ Bu link 24 saat içinde geçerliliğini yitirecektir.</p>
                    <p>Eğer bu hesabı siz oluşturmadıysanız, bu e-postayı görmezden gelebilirsiniz.</p>
                    <div class="footer">
                        <p>© 2024 Auth Service. Tüm hakları saklıdır.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName, verificationUrl);
    }

    /**
     * Şifre sıfırlama e-postası HTML içeriğini oluştur.
     */
    private String buildPasswordResetEmailHtml(String firstName, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 40px auto; background: white; border-radius: 8px;
                                 padding: 40px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; margin-bottom: 30px; }
                    .button { display: inline-block; padding: 14px 28px; background-color: #e74c3c;
                              color: white; text-decoration: none; border-radius: 5px; font-size: 16px; }
                    .footer { margin-top: 30px; text-align: center; color: #7f8c8d; font-size: 12px; }
                    .warning { color: #e74c3c; font-size: 13px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Auth Service</h1>
                    </div>
                    <p>Merhaba <strong>%s</strong>,</p>
                    <p>Şifre sıfırlama talebinde bulundunuz. Yeni şifrenizi belirlemek için
                       aşağıdaki butona tıklayın:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" class="button">Şifremi Sıfırla</a>
                    </div>
                    <p class="warning">⚠️ Bu link 1 saat içinde geçerliliğini yitirecektir.</p>
                    <p>Eğer bu talebi siz yapmadıysanız, hesabınız güvende demektir. Bu e-postayı
                       görmezden gelebilirsiniz.</p>
                    <div class="footer">
                        <p>© 2024 Auth Service. Tüm hakları saklıdır.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName, resetUrl);
    }
}
