package com.vahanalerts;

import java.time.Instant;
import java.time.LocalDate;

import com.vahanalerts.user.PhoneNumber;
import com.vahanalerts.user.UserAccount;
import com.vahanalerts.user.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:vahanalerts-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AuthenticationTests {

    private static final String PHONE_NUMBER = "+919876543210";
    private static final String PASSWORD = "CorrectHorseBattery12";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1990, 5, 15);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearUsers() {
        userAccountRepository.deleteAll();
    }

    @AfterEach
    void removeUsers() {
        userAccountRepository.deleteAll();
    }

    @Test
    void registrationCreatesAccountWithBcryptPasswordAndConsentRecord() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "  Vinay Kumar  ")
                        .param("phoneNumber", " +91 (98765) 43210 ")
                        .param("password", PASSWORD)
                        .param("passwordConfirmation", PASSWORD)
                        .param("dateOfBirth", "1990-05-15")
                        .param("whatsappReminderConsent", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        UserAccount user = userAccountRepository.findByPhoneNumber(PHONE_NUMBER).orElseThrow();
        assertEquals("Vinay Kumar", user.getName());
        assertEquals(PHONE_NUMBER, user.getPhoneNumber());
        assertNull(user.getEmail());
        assertEquals(DATE_OF_BIRTH, user.getDateOfBirth());
        assertNotEquals(PASSWORD, user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches(PASSWORD, user.getPasswordHash()));
        assertTrue(user.isWhatsappReminderConsent());
        assertNotNull(user.getWhatsappConsentAt());
        assertEquals("whatsapp-reminders-v1", user.getWhatsappConsentVersion());
        assertNull(user.getWhatsappConsentWithdrawnAt());
    }

    @Test
    void registrationWithoutWhatsAppConsentIsAllowedButRemindersAreDisabled() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "No Consent")
                        .param("phoneNumber", PHONE_NUMBER)
                        .param("password", PASSWORD)
                        .param("passwordConfirmation", PASSWORD)
                        .param("dateOfBirth", DATE_OF_BIRTH.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        UserAccount user = userAccountRepository.findByPhoneNumber(PHONE_NUMBER).orElseThrow();
        assertFalse(user.isWhatsappReminderConsent());
        assertNull(user.getWhatsappConsentAt());
        assertNull(user.getWhatsappConsentWithdrawnAt());
        assertNull(user.getWhatsappConsentVersion());
    }

    @Test
    void registrationAcceptsInternationalNumberAndDoesNotRejectUnder18() throws Exception {
        String internationalNumber = "+12025550123";
        LocalDate under18DateOfBirth = LocalDate.now().minusYears(15);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Young Vehicle Owner")
                        .param("phoneNumber", "+1 (202) 555-0123")
                        .param("password", PASSWORD)
                        .param("passwordConfirmation", PASSWORD)
                        .param("dateOfBirth", under18DateOfBirth.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        UserAccount user = userAccountRepository.findByPhoneNumber(internationalNumber).orElseThrow();
        assertEquals(under18DateOfBirth, user.getDateOfBirth());
    }

    @Test
    void missingRegistrationFieldsAreRejectedWithUsefulErrors() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "")
                        .param("phoneNumber", "")
                        .param("password", "")
                        .param("passwordConfirmation", "")
                        .param("dateOfBirth", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter your name.")))
                .andExpect(content().string(containsString("Enter your phone number with its country code")))
                .andExpect(content().string(containsString("Create a password.")))
                .andExpect(content().string(containsString("Confirm your password.")))
                .andExpect(content().string(containsString("Enter your date of birth.")));

        assertEquals(0, userAccountRepository.count());
    }

    @Test
    void futureDateOfBirthIsRejected() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Future Person")
                        .param("phoneNumber", PHONE_NUMBER)
                        .param("password", PASSWORD)
                        .param("passwordConfirmation", PASSWORD)
                        .param("dateOfBirth", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Date of birth cannot be in the future.")));

        assertEquals(0, userAccountRepository.count());
    }

    @Test
    void invalidDateOfBirthIsRejected() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Invalid Birthday")
                        .param("phoneNumber", PHONE_NUMBER)
                        .param("password", PASSWORD)
                        .param("passwordConfirmation", PASSWORD)
                        .param("dateOfBirth", "not-a-date"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter a valid date of birth.")));

        assertEquals(0, userAccountRepository.count());
    }

    @Test
    void invalidPhoneNumberIsRejected() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Invalid Phone")
                        .param("phoneNumber", "+91 12345 67890")
                        .param("password", PASSWORD)
                        .param("passwordConfirmation", PASSWORD)
                        .param("dateOfBirth", DATE_OF_BIRTH.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Enter a valid phone number with country code")));

        assertEquals(0, userAccountRepository.count());
    }

    @Test
    void duplicateNormalizedPhoneNumberIsRejected() throws Exception {
        saveUser(PHONE_NUMBER);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Another User")
                        .param("phoneNumber", "0091 98765 43210")
                        .param("password", PASSWORD)
                        .param("passwordConfirmation", PASSWORD)
                        .param("dateOfBirth", DATE_OF_BIRTH.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "An account with this phone number already exists.")));

        assertEquals(1, userAccountRepository.count());
    }

    @Test
    void successfulLoginAcceptsNormalizedPhoneNumberAndPreservesUserIdentity() throws Exception {
        UserAccount user = saveUser(PHONE_NUMBER);

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("phoneNumber", "+91 98765 43210")
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn()
                .getRequest()
                .getSession(false);

        String response = mockMvc.perform(get("/account").session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(response.contains(user.getName()));
        assertTrue(response.contains(PHONE_NUMBER));
        assertFalse(response.contains(user.getPasswordHash()));
        assertEquals(PHONE_NUMBER, PhoneNumber.normalize("0091 (98765) 43210"));
    }

    @Test
    void failedLoginRejectsIncorrectPasswordAndUnknownPhoneNumber() throws Exception {
        saveUser(PHONE_NUMBER);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("phoneNumber", PHONE_NUMBER)
                        .param("password", "incorrect-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("phoneNumber", "+919999999999")
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void anonymousVisitorsCanOpenPublicPagesAndHealthEndpoint() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());
    }

    @Test
    void anonymousVisitorsAreRedirectedFromAccountPage() throws Exception {
        mockMvc.perform(get("/account"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void logoutInvalidatesAuthenticatedSession() throws Exception {
        saveUser(PHONE_NUMBER);
        MockHttpSession session = login(PHONE_NUMBER);

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        assertTrue(session.isInvalid());
    }

    @Test
    void logoutRequiresCsrfToken() throws Exception {
        saveUser(PHONE_NUMBER);
        MockHttpSession session = login(PHONE_NUMBER);

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountOwnerCanWithdrawWhatsAppConsent() throws Exception {
        UserAccount user = saveUserWithConsent(PHONE_NUMBER);
        MockHttpSession session = login(PHONE_NUMBER);

        mockMvc.perform(post("/account/whatsapp-consent/withdraw")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"));

        UserAccount updated = userAccountRepository.findById(user.getId()).orElseThrow();
        assertFalse(updated.isWhatsappReminderConsent());
        assertNotNull(updated.getWhatsappConsentAt());
        assertNotNull(updated.getWhatsappConsentWithdrawnAt());
        assertTrue(updated.getWhatsappConsentWithdrawnAt().compareTo(Instant.now()) <= 0);
    }

    @Test
    void legacyAccountDataIsPreservedButEmailDoesNotAuthenticate() throws Exception {
        UserAccount legacy = userAccountRepository.saveAndFlush(new UserAccount(
                "Legacy User",
                "legacy@example.com",
                passwordEncoder.encode(PASSWORD)
        ));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("phoneNumber", "legacy@example.com")
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        UserAccount unchanged = userAccountRepository.findById(legacy.getId()).orElseThrow();
        assertEquals("legacy@example.com", unchanged.getEmail());
        assertNull(unchanged.getPhoneNumber());
        assertNull(unchanged.getDateOfBirth());
    }

    private UserAccount saveUser(String phoneNumber) {
        return userAccountRepository.saveAndFlush(new UserAccount(
                "Vinay Kumar",
                PhoneNumber.normalize(phoneNumber),
                DATE_OF_BIRTH,
                passwordEncoder.encode(PASSWORD),
                false
        ));
    }

    private UserAccount saveUserWithConsent(String phoneNumber) {
        return userAccountRepository.saveAndFlush(new UserAccount(
                "Vinay Kumar",
                PhoneNumber.normalize(phoneNumber),
                DATE_OF_BIRTH,
                passwordEncoder.encode(PASSWORD),
                true
        ));
    }

    private MockHttpSession login(String phoneNumber) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("phoneNumber", phoneNumber)
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getRequest()
                .getSession(false);
    }
}
