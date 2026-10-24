package com.vahanalerts;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

import com.vahanalerts.document.DocumentAttachmentStorage;
import com.vahanalerts.document.DocumentType;
import com.vahanalerts.document.DocumentExpiryStatus;
import com.vahanalerts.document.DocumentExpiryStatusCalculator;
import com.vahanalerts.document.DocumentForm;
import com.vahanalerts.document.VehicleDocument;
import com.vahanalerts.document.VehicleDocumentRepository;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vahanalerts-test;DB_CLOSE_DELAY=-1",
        "vahan.documents.storage-directory=target/test-document-files"
})
@AutoConfigureMockMvc
class VehicleDocumentTests {

    private static final String PASSWORD = "CorrectHorseBattery12";
    private static final LocalDate ISSUE_DATE = LocalDate.now().minusMonths(1);
    private static final LocalDate EXPIRY_DATE = LocalDate.now().plusDays(30);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleDocumentRepository documentRepository;

    @Autowired
    private DocumentAttachmentStorage attachmentStorage;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DocumentExpiryStatusCalculator statusCalculator;

    @BeforeEach
    void clearDocumentsAndAccounts() {
        documentRepository.deleteAll();
        vehicleRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @AfterEach
    void removeDocumentsAndAccounts() {
        documentRepository.findAll().stream()
                .map(VehicleDocument::getAttachmentKey)
                .filter(key -> key != null)
                .forEach(attachmentStorage::delete);
        documentRepository.deleteAll();
        vehicleRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void authenticatedOwnerCanCreateListViewEditAndDeleteDocument() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        MockHttpSession session = login(owner.getPhoneNumber());

        mockMvc.perform(get("/vehicles/{vehicleId}/documents", vehicle.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No documents yet")));
        mockMvc.perform(get("/vehicles/{vehicleId}/documents/new", vehicle.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add a document")));

        String redirect = mockMvc.perform(post("/vehicles/{vehicleId}/documents", vehicle.getId())
                        .session(session)
                        .with(csrf())
                        .param("type", "INSURANCE")
                        .param("documentNumber", "POL-42")
                        .param("issueDate", ISSUE_DATE.toString())
                        .param("expiryDate", EXPIRY_DATE.toString())
                        .param("notes", "Renew before travel"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        Long documentId = extractId(redirect);
        VehicleDocument created = documentRepository.findById(documentId).orElseThrow();
        assertEquals(vehicle.getId(), created.getVehicle().getId());
        assertEquals("POL-42", created.getDocumentNumber());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());

        mockMvc.perform(get("/vehicles/{vehicleId}/documents", vehicle.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vehicle insurance")))
                .andExpect(content().string(containsString("Expiring Soon")))
                .andExpect(content().string(containsString(EXPIRY_DATE.toString())));

        mockMvc.perform(get("/vehicles/{vehicleId}/documents/{documentId}",
                        vehicle.getId(), documentId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Renew before travel")))
                .andExpect(content().string(containsString("Expiring Soon")));

        mockMvc.perform(get("/vehicles/{vehicleId}/documents/{documentId}/edit",
                        vehicle.getId(), documentId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit document")));

        LocalDate updatedExpiry = LocalDate.now().plusDays(60);
        mockMvc.perform(post("/vehicles/{vehicleId}/documents/{documentId}",
                        vehicle.getId(), documentId)
                        .session(session)
                        .with(csrf())
                        .param("type", "INSURANCE")
                        .param("documentNumber", "POL-43")
                        .param("issueDate", ISSUE_DATE.toString())
                        .param("expiryDate", updatedExpiry.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + vehicle.getId() + "/documents/" + documentId));

        assertEquals("POL-43", documentRepository.findById(documentId).orElseThrow().getDocumentNumber());
        assertEquals(updatedExpiry, documentRepository.findById(documentId).orElseThrow().getExpiryDate());

        mockMvc.perform(get("/vehicles/{vehicleId}/documents/{documentId}/delete",
                        vehicle.getId(), documentId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Delete this document?")));

        mockMvc.perform(post("/vehicles/{vehicleId}/documents/{documentId}/delete",
                        vehicle.getId(), documentId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + vehicle.getId() + "/documents"));

        assertFalse(documentRepository.existsById(documentId));
    }

    @Test
    void expiryStatusUsesInjectedClockAndInclusiveThirtyDayBoundary() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-03-01T12:00:00Z"),
                ZoneId.of("Asia/Kolkata"));
        DocumentExpiryStatusCalculator calculator = new DocumentExpiryStatusCalculator(fixedClock);
        LocalDate today = LocalDate.now(fixedClock);

        assertEquals(DocumentExpiryStatus.EXPIRED, calculator.calculate(today.minusDays(1)));
        assertEquals(DocumentExpiryStatus.EXPIRING_SOON, calculator.calculate(today));
        assertEquals(DocumentExpiryStatus.EXPIRING_SOON, calculator.calculate(today.plusDays(30)));
        assertEquals(DocumentExpiryStatus.VALID, calculator.calculate(today.plusDays(31)));
        assertEquals(DocumentExpiryStatus.NO_EXPIRY_DATE, calculator.calculate(null));
    }

    @Test
    void issueDateAfterExpiryAndMissingRequiredExpiryAreRejected() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        MockHttpSession session = login(owner.getPhoneNumber());

        mockMvc.perform(post("/vehicles/{vehicleId}/documents", vehicle.getId())
                        .session(session)
                        .with(csrf())
                        .param("type", "PUC")
                        .param("issueDate", LocalDate.now().toString())
                        .param("expiryDate", LocalDate.now().minusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Expiry date cannot be before the issue date.")));

        mockMvc.perform(post("/vehicles/{vehicleId}/documents", vehicle.getId())
                        .session(session)
                        .with(csrf())
                        .param("type", "INSURANCE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "An expiry date is required for this document type.")));

        assertEquals(0, documentRepository.count());
    }

    @Test
    void futureIssueDateIsRejected() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        MockHttpSession session = login(owner.getPhoneNumber());

        mockMvc.perform(post("/vehicles/{vehicleId}/documents", vehicle.getId())
                        .session(session)
                        .with(csrf())
                        .param("type", "PUC")
                        .param("issueDate", statusCalculator.today().plusDays(1).toString())
                        .param("expiryDate", statusCalculator.today().plusDays(30).toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Issue date cannot be in the future.")));

        assertEquals(0, documentRepository.count());
    }

    @Test
    void malformedExpiryDateIsRejected() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        MockHttpSession session = login(owner.getPhoneNumber());

        mockMvc.perform(post("/vehicles/{vehicleId}/documents", vehicle.getId())
                        .session(session)
                        .with(csrf())
                        .param("type", "INSURANCE")
                        .param("expiryDate", "not-a-date"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter a valid expiry date.")));

        assertEquals(0, documentRepository.count());
    }

    @Test
    void otherDocumentMayHaveNoExpiryDateAndDisplaysNoExpiryStatus() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        MockHttpSession session = login(owner.getPhoneNumber());

        String redirect = mockMvc.perform(post("/vehicles/{vehicleId}/documents", vehicle.getId())
                        .session(session)
                        .with(csrf())
                        .param("type", "OTHER")
                        .param("documentNumber", "Lifetime record"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        Long documentId = extractId(redirect);
        assertNull(documentRepository.findById(documentId).orElseThrow().getExpiryDate());
        mockMvc.perform(get("/vehicles/{vehicleId}/documents", vehicle.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No Expiry Date")));
    }

    @Test
    void anonymousUsersCannotAccessDocumentPagesOrActions() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");

        mockMvc.perform(get("/vehicles/{vehicleId}/documents", vehicle.getId()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/vehicles/{vehicleId}/documents/new", vehicle.getId()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/vehicles/{vehicleId}/documents", vehicle.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void userCannotAccessDocumentsThroughAnotherUsersVehicleOrTamperedDocumentId() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        UserAccount otherUser = saveUser("+919876543211");
        Vehicle ownersVehicle = createVehicle(owner, "KA01AB1234");
        Vehicle otherVehicle = createVehicle(otherUser, "KA02AB1234");
        VehicleDocument document = createDocument(ownersVehicle, LocalDate.now().plusDays(100));
        MockHttpSession otherSession = login(otherUser.getPhoneNumber());

        mockMvc.perform(get("/vehicles/{vehicleId}/documents", ownersVehicle.getId()).session(otherSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/vehicles/{vehicleId}/documents/new", ownersVehicle.getId()).session(otherSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/vehicles/{vehicleId}/documents", ownersVehicle.getId())
                        .session(otherSession)
                        .with(csrf())
                        .param("type", "PUC")
                        .param("expiryDate", LocalDate.now().plusDays(10).toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/vehicles/{vehicleId}/documents/{documentId}",
                        otherVehicle.getId(), document.getId()).session(otherSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/vehicles/{vehicleId}/documents/{documentId}/edit",
                        ownersVehicle.getId(), document.getId()).session(otherSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/vehicles/{vehicleId}/documents/{documentId}",
                        ownersVehicle.getId(), document.getId())
                        .session(otherSession)
                        .with(csrf())
                        .param("type", "PUC")
                        .param("expiryDate", LocalDate.now().plusDays(15).toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/vehicles/{vehicleId}/documents/{documentId}/delete",
                        ownersVehicle.getId(), document.getId()).session(otherSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/vehicles/{vehicleId}/documents/{documentId}/delete",
                        ownersVehicle.getId(), document.getId())
                        .session(otherSession)
                        .with(csrf()))
                .andExpect(status().isNotFound());

        VehicleDocument unchanged = documentRepository.findById(document.getId()).orElseThrow();
        assertEquals(ownersVehicle.getId(), unchanged.getVehicle().getId());
        assertEquals(LocalDate.now().plusDays(100), unchanged.getExpiryDate());
    }

    @Test
    void stateChangingDocumentRequestsRequireCsrf() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        MockHttpSession session = login(owner.getPhoneNumber());

        mockMvc.perform(post("/vehicles/{vehicleId}/documents", vehicle.getId())
                        .session(session)
                        .param("type", "PUC")
                        .param("expiryDate", LocalDate.now().plusDays(10).toString()))
                .andExpect(status().isForbidden());
        assertEquals(0, documentRepository.count());
    }

    @Test
    void documentUpdateAndDeleteRequireCsrf() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        VehicleDocument document = createDocument(vehicle, LocalDate.now().plusDays(50));
        MockHttpSession session = login(owner.getPhoneNumber());

        mockMvc.perform(post("/vehicles/{vehicleId}/documents/{documentId}",
                        vehicle.getId(), document.getId())
                        .session(session)
                        .param("type", "INSURANCE")
                        .param("expiryDate", LocalDate.now().plusDays(60).toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/vehicles/{vehicleId}/documents/{documentId}/delete",
                        vehicle.getId(), document.getId())
                        .session(session))
                .andExpect(status().isForbidden());

        assertTrue(documentRepository.existsById(document.getId()));
    }

    @Test
    void privatePdfAttachmentCanBeDownloadedAndIsRemovedWithDocument() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        MockHttpSession session = login(owner.getPhoneNumber());
        byte[] pdfBytes = "%PDF-1.7\nprivate test file".getBytes();
        MockMultipartFile pdf = new MockMultipartFile(
                "attachment", "unsafe-name.pdf", "application/pdf", pdfBytes);

        String redirect = mockMvc.perform(multipart("/vehicles/{vehicleId}/documents", vehicle.getId())
                        .file(pdf)
                        .session(session)
                        .with(csrf())
                        .param("type", "INSURANCE")
                        .param("expiryDate", LocalDate.now().plusDays(50).toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        Long documentId = extractId(redirect);
        VehicleDocument document = documentRepository.findById(documentId).orElseThrow();
        assertNotNull(document.getAttachmentKey());
        assertTrue(document.getAttachmentKey().endsWith(".pdf"));
        assertEquals("application/pdf", document.getAttachmentMediaType());
        assertEquals(pdfBytes.length, document.getAttachmentSize());

        mockMvc.perform(get("/vehicles/{vehicleId}/documents/{documentId}/attachment",
                        vehicle.getId(), documentId).session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(pdfBytes));

        Resource resource = new FileSystemResource(
                "target/test-document-files/" + document.getAttachmentKey());
        assertTrue(resource.exists());
        UserAccount otherUser = saveUser("+919876543211");
        MockHttpSession otherSession = login(otherUser.getPhoneNumber());
        mockMvc.perform(post("/vehicles/{vehicleId}/documents/{documentId}/delete",
                        vehicle.getId(), documentId)
                        .session(otherSession)
                        .with(csrf()))
                .andExpect(status().isNotFound());
        assertTrue(resource.exists());

        mockMvc.perform(post("/vehicles/{vehicleId}/documents/{documentId}/delete",
                        vehicle.getId(), documentId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertFalse(resource.exists());
    }

    @Test
    void invalidAttachmentAndOversizedAttachmentAreRejected() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        MockHttpSession session = login(owner.getPhoneNumber());
        MockMultipartFile invalidFile = new MockMultipartFile(
                "attachment", "not-a-pdf.pdf", "application/pdf", "not a pdf".getBytes());

        mockMvc.perform(multipart("/vehicles/{vehicleId}/documents", vehicle.getId())
                        .file(invalidFile)
                        .session(session)
                        .with(csrf())
                        .param("type", "INSURANCE")
                        .param("expiryDate", LocalDate.now().plusDays(50).toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Only valid PDF, JPEG, and PNG files are accepted.")));

        byte[] tooLarge = new byte[5 * 1024 * 1024 + 1];
        tooLarge[0] = '%';
        tooLarge[1] = 'P';
        tooLarge[2] = 'D';
        tooLarge[3] = 'F';
        tooLarge[4] = '-';
        Arrays.fill(tooLarge, 5, tooLarge.length, (byte) 'x');
        MockMultipartFile oversizedFile = new MockMultipartFile(
                "attachment", "large.pdf", "application/pdf", tooLarge);
        mockMvc.perform(multipart("/vehicles/{vehicleId}/documents", vehicle.getId())
                        .file(oversizedFile)
                        .session(session)
                        .with(csrf())
                        .param("type", "INSURANCE")
                        .param("expiryDate", LocalDate.now().plusDays(50).toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The attachment exceeds the configured size limit.")));

        assertEquals(0, documentRepository.count());
    }

    @Test
    void anotherUserCannotDownloadPrivateAttachment() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        UserAccount otherUser = saveUser("+919876543211");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        VehicleDocument document = createDocument(vehicle, LocalDate.now().plusDays(50));
        MockHttpSession ownerSession = login(owner.getPhoneNumber());
        MockMultipartFile pdf = new MockMultipartFile(
                "attachment", "doc.pdf", "application/pdf", "%PDF-1.5\nprivate".getBytes());

        mockMvc.perform(multipart("/vehicles/{vehicleId}/documents/{documentId}",
                        vehicle.getId(), document.getId())
                        .file(pdf)
                        .session(ownerSession)
                        .with(csrf())
                        .param("type", "INSURANCE")
                        .param("expiryDate", LocalDate.now().plusDays(50).toString()))
                .andExpect(status().is3xxRedirection());

        MockHttpSession otherSession = login(otherUser.getPhoneNumber());
        mockMvc.perform(get("/vehicles/{vehicleId}/documents/{documentId}/attachment",
                        vehicle.getId(), document.getId()).session(otherSession))
                .andExpect(status().isNotFound());

        VehicleDocument saved = documentRepository.findById(document.getId()).orElseThrow();
        FileSystemResource resource = new FileSystemResource(
                "target/test-document-files/" + saved.getAttachmentKey());
        assertTrue(resource.exists());
    }

    private UserAccount saveUser(String phoneNumber) {
        return userAccountRepository.saveAndFlush(new UserAccount(
                "Document Owner",
                PhoneNumber.normalize(phoneNumber),
                LocalDate.of(1990, 5, 15),
                passwordEncoder.encode(PASSWORD),
                false
        ));
    }

    private Vehicle createVehicle(UserAccount owner, String registrationNumber) {
        VehicleForm form = new VehicleForm();
        form.setRegistrationNumber(registrationNumber);
        form.setType(VehicleType.CAR);
        return vehicleRepository.saveAndFlush(new Vehicle(owner, form, registrationNumber));
    }

    private VehicleDocument createDocument(Vehicle vehicle, LocalDate expiryDate) {
        DocumentForm form = new DocumentForm();
        form.setType(DocumentType.INSURANCE);
        form.setExpiryDate(expiryDate);
        return documentRepository.saveAndFlush(new VehicleDocument(vehicle, form));
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

    private Long extractId(String redirectUrl) {
        assertNotNull(redirectUrl);
        return Long.valueOf(redirectUrl.substring(redirectUrl.lastIndexOf('/') + 1));
    }
}
