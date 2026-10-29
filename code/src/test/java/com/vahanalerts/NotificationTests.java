package com.vahanalerts;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vahanalerts.document.DocumentForm;
import com.vahanalerts.document.DocumentType;
import com.vahanalerts.document.VehicleDocument;
import com.vahanalerts.document.VehicleDocumentRepository;
import com.vahanalerts.notification.DocumentExpiryReminderGenerator;
import com.vahanalerts.notification.InAppNotification;
import com.vahanalerts.notification.InAppNotificationRepository;
import com.vahanalerts.notification.InAppNotificationService;
import com.vahanalerts.notification.NotificationListItem;
import com.vahanalerts.user.PhoneNumber;
import com.vahanalerts.user.UserAccount;
import com.vahanalerts.user.UserAccountRepository;
import com.vahanalerts.vehicle.Vehicle;
import com.vahanalerts.vehicle.VehicleForm;
import com.vahanalerts.vehicle.VehicleRepository;
import com.vahanalerts.vehicle.VehicleType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vahanalerts-test;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class NotificationTests {

    private static final String PASSWORD = "CorrectHorseBattery12";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleDocumentRepository documentRepository;

    @Autowired
    private InAppNotificationRepository notificationRepository;

    @Autowired
    private DocumentExpiryReminderGenerator reminderGenerator;

    @Autowired
    private com.vahanalerts.notification.DocumentExpiryReminderSchedule reminderSchedule;

    @Autowired
    private InAppNotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @BeforeEach
    void clearRecords() {
        notificationRepository.deleteAll();
        documentRepository.deleteAll();
        vehicleRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @AfterEach
    void removeRecords() {
        notificationRepository.deleteAll();
        documentRepository.deleteAll();
        vehicleRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void generatesAllConfiguredReminderOffsetsForTheDocumentOwner() {
        LocalDate today = LocalDate.now(clock);
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        createDocument(vehicle, today.plusDays(30));
        createDocument(vehicle, today.plusDays(7));
        createDocument(vehicle, today.plusDays(1));
        createDocument(vehicle, today);

        assertEquals(10, reminderGenerator.generateForDate(today));

        List<InAppNotification> notifications =
                notificationRepository.findAllByOwner_IdOrderByCreatedAtDescIdDesc(owner.getId());
        assertEquals(10, notifications.size());
        assertEquals(Set.of(30, 7, 1, 0), notifications.stream()
                .map(InAppNotification::getReminderOffsetDays)
                .collect(Collectors.toSet()));
        assertEquals(4, notifications.stream()
                .filter(notification -> notification.getReminderOffsetDays() == 30).count());
        assertEquals(3, notifications.stream()
                .filter(notification -> notification.getReminderOffsetDays() == 7).count());
        assertEquals(2, notifications.stream()
                .filter(notification -> notification.getReminderOffsetDays() == 1).count());
        assertEquals(1, notifications.stream()
                .filter(notification -> notification.getReminderOffsetDays() == 0).count());
        assertTrue(notifications.stream().allMatch(notification ->
                notification.getVehicle().getId().equals(vehicle.getId())
                        && notification.getOwner().getId().equals(owner.getId())
                        && notification.getDocument() != null
                        && notification.getMessage().contains(vehicle.getRegistrationNumber())
                        && notification.getMessage().contains("Vehicle insurance")));
    }

    @Test
    void skipsDocumentsWithoutExpiryAndDatesOutsideReminderOffsets() {
        LocalDate today = LocalDate.now(clock);
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        createDocument(vehicle, null, DocumentType.OTHER);
        createDocument(vehicle, today.plusDays(31), DocumentType.INSURANCE);

        assertEquals(0, reminderGenerator.generateForDate(today));
        assertEquals(0, notificationRepository.count());
    }

    @Test
    void repeatedProcessingDoesNotCreateDuplicateReminders() {
        LocalDate today = LocalDate.now(clock);
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        createDocument(vehicle, today.plusDays(7));

        assertEquals(2, reminderGenerator.generateForDate(today));
        assertEquals(0, reminderGenerator.generateForDate(today));
        assertEquals(2, notificationRepository.count());
    }

    @Test
    void startupCatchUpCreatesMissedOffsetsWithoutDuplicates() {
        LocalDate today = LocalDate.now(clock);
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        createDocument(vehicle, today.plusDays(2));

        reminderSchedule.recoverMissedRemindersAtStartup();
        assertEquals(2, notificationRepository.count());
        reminderSchedule.recoverMissedRemindersAtStartup();
        assertEquals(2, notificationRepository.count());

        List<InAppNotification> notifications =
                notificationRepository.findAllByOwner_IdOrderByCreatedAtDescIdDesc(owner.getId());
        assertEquals(Set.of(30, 7), notifications.stream()
                .map(InAppNotification::getReminderOffsetDays)
                .collect(Collectors.toSet()));
    }

    @Test
    void renewalCreatesNewScheduleWhilePreservingOriginalExpirySnapshot() {
        LocalDate today = LocalDate.now(clock);
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        VehicleDocument document = createDocument(vehicle, today.plusDays(30));

        assertEquals(1, reminderGenerator.generateForDate(today));
        document.updateFrom(documentForm(DocumentType.INSURANCE, today.plusDays(7)));
        documentRepository.saveAndFlush(document);
        assertEquals(2, reminderGenerator.generateForDate(today));

        List<InAppNotification> notifications =
                notificationRepository.findAllByOwner_IdOrderByCreatedAtDescIdDesc(owner.getId());
        assertEquals(3, notifications.size());
        assertTrue(notifications.stream().anyMatch(notification ->
                notification.getExpiryDateSnapshot().equals(today.plusDays(30))));
        assertTrue(notifications.stream().anyMatch(notification ->
                notification.getExpiryDateSnapshot().equals(today.plusDays(7))));
        List<NotificationListItem> listItems = notificationService.findAllForOwner(owner.getId());
        assertTrue(listItems.stream().anyMatch(NotificationListItem::isExpiryDateChanged));
    }

    @Test
    void notificationCentreIsOwnerScopedOrderedAndSupportsReadActions() throws Exception {
        LocalDate today = LocalDate.now(clock);
        UserAccount owner = saveUser("+919876543210");
        UserAccount otherOwner = saveUser("+919876543211");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        createDocument(vehicle, today.plusDays(30));
        createDocument(vehicle, today.plusDays(30));
        reminderGenerator.generateForDate(today);
        MockHttpSession ownerSession = login(owner.getPhoneNumber());
        MockHttpSession otherSession = login(otherOwner.getPhoneNumber());

        List<InAppNotification> notifications =
                notificationRepository.findAllByOwner_IdOrderByCreatedAtDescIdDesc(owner.getId());
        assertEquals(2, notifications.size());
        InAppNotification older = notifications.get(0);
        InAppNotification newer = notifications.get(1);
        jdbcTemplate.update("UPDATE in_app_notification SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2024-01-01T00:00:00Z")), older.getId());
        jdbcTemplate.update("UPDATE in_app_notification SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2024-02-01T00:00:00Z")), newer.getId());
        assertEquals(newer.getId(),
                notificationService.findAllForOwner(owner.getId()).get(0).id());

        mockMvc.perform(get("/notifications").session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("KA01AB1234")))
                .andExpect(content().string(containsString("2 unread")));
        mockMvc.perform(get("/notifications").session(otherSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("KA01AB1234"))));

        mockMvc.perform(post("/notifications/{id}/read", newer.getId())
                        .session(ownerSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));
        assertNotNull(notificationRepository.findById(newer.getId()).orElseThrow().getReadAt());
        assertNull(notificationRepository.findById(older.getId()).orElseThrow().getReadAt());

        mockMvc.perform(post("/notifications/read-all").session(ownerSession).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertTrue(notificationRepository.findAllByOwner_IdOrderByCreatedAtDescIdDesc(owner.getId())
                .stream().allMatch(InAppNotification::isRead));
    }

    @Test
    void notificationIdsAndMutationCsrfAreProtected() throws Exception {
        LocalDate today = LocalDate.now(clock);
        UserAccount owner = saveUser("+919876543210");
        UserAccount otherOwner = saveUser("+919876543211");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        createDocument(vehicle, today.plusDays(7));
        reminderGenerator.generateForDate(today);
        InAppNotification notification = notificationRepository.findAll().get(0);
        MockHttpSession otherSession = login(otherOwner.getPhoneNumber());

        mockMvc.perform(post("/notifications/{id}/read", notification.getId())
                        .session(otherSession)
                        .with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/notifications/{id}/read", notification.getId())
                        .session(login(owner.getPhoneNumber())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/notifications/read-all")
                        .session(otherSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertFalse(notificationRepository.findById(notification.getId()).orElseThrow().isRead());
    }

    @Test
    void unauthenticatedNotificationAccessRequiresLogin() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/notifications/read-all").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void reminderScheduleHasConfigurableCronAndIndiaTimezone() throws Exception {
        Scheduled scheduled = com.vahanalerts.notification.DocumentExpiryReminderSchedule.class
                .getMethod("generateDailyReminders")
                .getAnnotation(Scheduled.class);

        assertNotNull(scheduled);
        assertEquals("${vahan.reminders.document-expiry.cron:0 0 9 * * *}", scheduled.cron());
        assertEquals("${vahan.reminders.time-zone:Asia/Kolkata}", scheduled.zone());
        assertEquals("Asia/Kolkata", environment.getProperty("vahan.reminders.time-zone"));
    }

    private UserAccount saveUser(String phoneNumber) {
        return userAccountRepository.saveAndFlush(new UserAccount(
                "Notification Owner",
                PhoneNumber.normalize(phoneNumber),
                LocalDate.of(1990, 5, 15),
                passwordEncoder.encode(PASSWORD),
                false));
    }

    private Vehicle createVehicle(UserAccount owner, String registrationNumber) {
        VehicleForm form = new VehicleForm();
        form.setRegistrationNumber(registrationNumber);
        form.setType(VehicleType.CAR);
        return vehicleRepository.saveAndFlush(new Vehicle(owner, form, registrationNumber));
    }

    private VehicleDocument createDocument(Vehicle vehicle, LocalDate expiryDate) {
        return createDocument(vehicle, expiryDate, DocumentType.INSURANCE);
    }

    private VehicleDocument createDocument(Vehicle vehicle, LocalDate expiryDate, DocumentType type) {
        return documentRepository.saveAndFlush(new VehicleDocument(
                vehicle, documentForm(type, expiryDate)));
    }

    private DocumentForm documentForm(DocumentType type, LocalDate expiryDate) {
        DocumentForm form = new DocumentForm();
        form.setType(type);
        form.setExpiryDate(expiryDate);
        return form;
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
