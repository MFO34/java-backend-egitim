package com.auth.security.oauth2;

import com.auth.entity.AuthProvider;
import com.auth.entity.Role;
import com.auth.entity.User;
import com.auth.repository.RoleRepository;
import com.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * OAUTH2 KULLANICI SERVİSİ
 * ==========================
 * DefaultOAuth2UserService'i extend ederek özelleştirilir.
 * Google/GitHub'dan gelen kullanıcıyı veritabanına kaydeder/günceller.
 *
 * OAuth2 Akışı:
 *   1. Kullanıcı "Google ile Giriş" butonuna tıklar
 *   2. Spring Security → Google authorization endpoint'ine yönlendirir
 *   3. Kullanıcı Google'da giriş yapar → code=xxx ile geri döner
 *   4. Spring, code → access token için Google'a istek atar
 *   5. Spring, access token → user info için Google'a istek atar
 *   6. Bu servis çağrılır: loadUser(OAuth2UserRequest)
 *   7. DB'de kullanıcı var → güncelle, yok → oluştur
 *   8. CustomOAuth2User döner → OAuth2SuccessHandler çağrılır
 */
@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CustomOAuth2UserService(UserRepository userRepository,
                                    RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 1. Provider'dan (Google/GitHub) kullanıcı bilgisini al
        var oAuth2User = super.loadUser(userRequest);

        // 2. Hangi provider? "google" veya "github"
        var registrationId = userRequest.getClientRegistration()
            .getRegistrationId()
            .toUpperCase();  // "GOOGLE" veya "GITHUB"

        var provider = AuthProvider.valueOf(registrationId);
        var attributes = oAuth2User.getAttributes();

        // 3. Provider'a göre kullanıcı bilgilerini çıkar
        var oAuth2UserInfo = extractUserInfo(provider, attributes);

        log.info("OAuth2 girişi: provider={}, email={}", provider, oAuth2UserInfo.email());

        // 4. DB'de bu e-posta ile kullanıcı var mı?
        var user = userRepository.findByEmail(oAuth2UserInfo.email())
            .map(existingUser -> updateOAuth2User(existingUser, oAuth2UserInfo))
            .orElseGet(() -> createOAuth2User(oAuth2UserInfo, provider));

        return new CustomOAuth2User(user, attributes);
    }

    /**
     * Mevcut kullanıcıyı güncelle (profil resmi değişmiş olabilir)
     */
    private User updateOAuth2User(User user, OAuth2UserInfo info) {
        user.setFirstName(info.firstName());
        user.setLastName(info.lastName());
        user.setProfilePictureUrl(info.pictureUrl());
        user.setEmailVerified(true);  // OAuth2 sağlayıcı e-postayı doğruladı
        return userRepository.save(user);
    }

    /**
     * Yeni OAuth2 kullanıcısı oluştur
     */
    private User createOAuth2User(OAuth2UserInfo info, AuthProvider provider) {
        // Varsayılan USER rolü al
        var userRole = roleRepository.findByName("USER")
            .orElseGet(() -> roleRepository.save(new Role("USER")));

        var newUser = User.builder()
            .firstName(info.firstName())
            .lastName(info.lastName())
            .email(info.email())
            .password(null)            // OAuth2 kullanıcısının şifresi yok
            .provider(provider)
            .providerId(info.providerId())
            .profilePictureUrl(info.pictureUrl())
            .emailVerified(true)       // OAuth2 sağlayıcı doğruladı
            .enabled(true)
            .roles(Set.of(userRole))
            .build();

        log.info("Yeni OAuth2 kullanıcısı oluşturuldu: {}", info.email());
        return userRepository.save(newUser);
    }

    /**
     * Provider'a göre attribute'lardan kullanıcı bilgisi çıkar
     *
     * Google attribute'ları:
     *   sub → provider ID
     *   email, given_name, family_name, picture
     *
     * GitHub attribute'ları:
     *   id → provider ID
     *   email (null olabilir — private email), login, avatar_url, name
     */
    private OAuth2UserInfo extractUserInfo(AuthProvider provider, Map<String, Object> attrs) {
        return switch (provider) {
            case GOOGLE -> new OAuth2UserInfo(
                String.valueOf(attrs.get("sub")),    // Google benzersiz ID
                (String) attrs.get("email"),
                (String) attrs.getOrDefault("given_name", ""),
                (String) attrs.getOrDefault("family_name", ""),
                (String) attrs.get("picture")
            );
            case GITHUB -> {
                // GitHub'da email null olabilir (gizli e-posta ayarı)
                var email = (String) attrs.get("email");
                if (email == null) {
                    // login@github.placeholder kullan (gerçekte GitHub API'dan alınır)
                    email = attrs.get("login") + "@github.placeholder";
                }
                // GitHub'da name "FirstName LastName" formatında olabilir
                var fullName  = (String) attrs.getOrDefault("name", attrs.get("login"));
                var nameParts = fullName != null ? fullName.split(" ", 2) : new String[]{"", ""};
                yield new OAuth2UserInfo(
                    String.valueOf(attrs.get("id")),
                    email,
                    nameParts[0],
                    nameParts.length > 1 ? nameParts[1] : "",
                    (String) attrs.get("avatar_url")
                );
            }
            default -> throw new IllegalArgumentException("Desteklenmeyen provider: " + provider);
        };
    }

    // Provider bilgilerini taşıyan yardımcı record
    private record OAuth2UserInfo(
        String providerId,
        String email,
        String firstName,
        String lastName,
        String pictureUrl
    ) {}
}
