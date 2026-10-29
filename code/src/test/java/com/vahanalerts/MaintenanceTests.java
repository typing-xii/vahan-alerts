package com.vahanalerts;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import com.vahanalerts.maintenance.MaintenanceForm;
import com.vahanalerts.maintenance.MaintenanceRecord;
import com.vahanalerts.maintenance.MaintenanceRecordRepository;
import com.vahanalerts.maintenance.ServiceType;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:vahanalerts-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class MaintenanceTests {

    private static final String PASSWORD = "CorrectHorseBattery12";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @BeforeEach
    void clearMaintenanceAndAccounts() {
        maintenanceRecordRepository.deleteAll();
        vehicleRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @AfterEach
    void removeMaintenanceAndAccounts() {
        maintenanceRecordRepository.deleteAll();
        vehicleRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void authenticatedOwnerCanCreateListViewEditAndDeleteServiceRecord() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        MockHttpSession session = login(owner.getPhoneNumber());
        LocalDate olderDate = LocalDate.now(clock).minusDays(40);
        LocalDate newerDate = LocalDate.now(clock).minusDays(10);

        mockMvc.perform(get("/vehicles/{vehicleId}/maintenance", vehicle.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No service history yet")))
                .andExpect(content().string(containsString("Not recorded")));
        mockMvc.perform(get("/vehicles/{vehicleId}/maintenance/new", vehicle.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add service record")));

        String olderRedirect = createRecord(session, vehicle, olderDate, 12000, "GENERAL_SERVICE", "10.10");
        Long olderId = extractId(olderRedirect);
        String newerRedirect = createRecord(session, vehicle, newerDate, 18000, "ENGINE_OIL_CHANGE", "20.25");
        Long newerId = extractId(newerRedirect);

        MaintenanceRecord created = maintenanceRecordRepository.findById(newerId).orElseThrow();
        assertEquals(vehicle.getId(), created.getVehicle().getId());
        assertEquals(new BigDecimal("20.25"), created.getServiceCostInr());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());

        mockMvc.perform(get("/vehicles/{vehicleId}/maintenance", vehicle.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("General Service")))
                .andExpect(content().string(containsString("Engine Oil Change")))
                .andExpect(content().string(containsString("TOTAL RECORDED COST")))
                .andExpect(content().string(containsString("30.35")))
                .andExpect(content().string(containsString("2")))
                .andExpect(content().string(containsString(newerDate.toString())));

        var history = maintenanceRecordRepository.findAllByVehicle_IdOrderByServiceDateDescIdDesc(vehicle.getId());
        assertEquals(newerId, history.get(0).getId());
        assertEquals(olderId, history.get(1).getId());

        mockMvc.perform(get("/vehicles/{vehicleId}/maintenance/{recordId}",
                        vehicle.getId(), newerId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Engine Oil Change")))
                .andExpect(content().string(containsString("20.25")));
        mockMvc.perform(get("/vehicles/{vehicleId}/maintenance/{recordId}/edit",
                        vehicle.getId(), newerId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit service record")));

        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance/{recordId}",
                        vehicle.getId(), newerId)
                        .session(session)
                        .with(csrf())
                        .param("serviceDate", newerDate.toString())
                        .param("odometerKm", "18000")
                        .param("serviceType", "REPAIRS")
                        .param("workshopName", "Central Workshop")
                        .param("serviceCostInr", "32.75")
                        .param("description", "Replaced a worn belt"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + vehicle.getId() + "/maintenance/" + newerId));

        MaintenanceRecord updated = maintenanceRecordRepository.findById(newerId).orElseThrow();
        assertEquals(ServiceType.REPAIRS, updated.getServiceType());
        assertEquals(new BigDecimal("32.75"), updated.getServiceCostInr());
        assertEquals("Central Workshop", updated.getWorkshopName());
        mockMvc.perform(get("/vehicles/{vehicleId}/maintenance/{recordId}",
                        vehicle.getId(), newerId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Replaced a worn belt")));

        mockMvc.perform(get("/vehicles/{vehicleId}/maintenance/{recordId}/delete",
                        vehicle.getId(), newerId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Delete this service record?")));
        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance/{recordId}/delete",
                        vehicle.getId(), newerId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + vehicle.getId() + "/maintenance"));

        assertFalse(maintenanceRecordRepository.existsById(newerId));
        assertEquals(1, maintenanceRecordRepository.count());
    }

    @Test
    void rejectsNegativeOdometerAndCostAndMissingRequiredFields() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        MockHttpSession session = login(owner.getPhoneNumber());

        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance", vehicle.getId())
                        .session(session)
                        .with(csrf())
                        .param("serviceDate", LocalDate.now(clock).toString())
                        .param("odometerKm", "-1")
                        .param("serviceType", "GENERAL_SERVICE")
                        .param("serviceCostInr", "-2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Odometer reading cannot be negative.")))
                .andExpect(content().string(containsString("Service cost cannot be negative.")));

        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance", vehicle.getId())
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter the service date.")))
                .andExpect(content().string(containsString("Enter the odometer reading.")))
                .andExpect(content().string(containsString("Choose a service type.")))
                .andExpect(content().string(containsString("Enter the service cost.")));

        assertEquals(0, maintenanceRecordRepository.count());
    }

    @Test
    void futureServiceDateIsRejected() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        MockHttpSession session = login(owner.getPhoneNumber());

        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance", vehicle.getId())
                        .session(session)
                        .with(csrf())
                        .param("serviceDate", LocalDate.now(clock).plusDays(1).toString())
                        .param("odometerKm", "100")
                        .param("serviceType", "GENERAL_SERVICE")
                        .param("serviceCostInr", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Service date cannot be in the future.")));

        assertEquals(0, maintenanceRecordRepository.count());
    }

    @Test
    void chronologicalOdometerSequenceIsEnforcedOnCreateAndHistoricalEdit() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        MockHttpSession session = login(owner.getPhoneNumber());
        LocalDate today = LocalDate.now(clock);
        Long firstId = extractId(createRecord(session, vehicle, today.minusDays(30), 1000,
                "GENERAL_SERVICE", "5.00"));
        Long secondId = extractId(createRecord(session, vehicle, today.minusDays(10), 2000,
                "GENERAL_SERVICE", "5.00"));

        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance", vehicle.getId())
                        .session(session)
                        .with(csrf())
                        .param("serviceDate", today.minusDays(20).toString())
                        .param("odometerKm", "999")
                        .param("serviceType", "REPAIRS")
                        .param("serviceCostInr", "1.00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Odometer reading must not be lower than an earlier service or higher than a later service.")));

        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance/{recordId}",
                        vehicle.getId(), firstId)
                        .session(session)
                        .with(csrf())
                        .param("serviceDate", today.minusDays(30).toString())
                        .param("odometerKm", "2001")
                        .param("serviceType", "GENERAL_SERVICE")
                        .param("serviceCostInr", "5.00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Odometer reading must not be lower than an earlier service or higher than a later service.")));

        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance/{recordId}",
                        vehicle.getId(), secondId)
                        .session(session)
                        .with(csrf())
                        .param("serviceDate", today.minusDays(10).toString())
                        .param("odometerKm", "999")
                        .param("serviceType", "GENERAL_SERVICE")
                        .param("serviceCostInr", "5.00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Odometer reading must not be lower than an earlier service or higher than a later service.")));

        assertEquals(2, maintenanceRecordRepository.count());
    }

    @Test
    void userCannotViewCreateEditOrDeleteRecordsThroughAnotherUsersVehicle() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        UserAccount anotherUser = saveUser("+919876543211");
        Vehicle ownersVehicle = createVehicle(owner, "KA01AB1234");
        Vehicle anotherVehicle = createVehicle(anotherUser, "KA02AB1234");
        LocalDate serviceDate = LocalDate.now(clock).minusDays(5);
        MaintenanceRecord record = createRecord(ownersVehicle, serviceDate, 5000, "100.00");
        MockHttpSession anotherSession = login(anotherUser.getPhoneNumber());

        mockMvc.perform(get("/vehicles/{vehicleId}/maintenance", ownersVehicle.getId()).session(anotherSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/vehicles/{vehicleId}/maintenance/new", ownersVehicle.getId()).session(anotherSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance", ownersVehicle.getId())
                        .session(anotherSession)
                        .with(csrf())
                        .param("serviceDate", serviceDate.toString())
                        .param("odometerKm", "6000")
                        .param("serviceType", "REPAIRS")
                        .param("serviceCostInr", "1.00"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/vehicles/{vehicleId}/maintenance/{recordId}",
                        anotherVehicle.getId(), record.getId()).session(anotherSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/vehicles/{vehicleId}/maintenance/{recordId}/edit",
                        ownersVehicle.getId(), record.getId()).session(anotherSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance/{recordId}",
                        ownersVehicle.getId(), record.getId())
                        .session(anotherSession)
                        .with(csrf())
                        .param("serviceDate", serviceDate.toString())
                        .param("odometerKm", "7000")
                        .param("serviceType", "REPAIRS")
                        .param("serviceCostInr", "1.00"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/vehicles/{vehicleId}/maintenance/{recordId}/delete",
                        ownersVehicle.getId(), record.getId()).session(anotherSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance/{recordId}/delete",
                        ownersVehicle.getId(), record.getId())
                        .session(anotherSession)
                        .with(csrf()))
                .andExpect(status().isNotFound());

        MaintenanceRecord unchanged = maintenanceRecordRepository.findById(record.getId()).orElseThrow();
        assertEquals(ownersVehicle.getId(), unchanged.getVehicle().getId());
        assertEquals(5000L, unchanged.getOdometerKm());
    }

    @Test
    void maintenanceMutationsRequireCsrf() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        Vehicle vehicle = createVehicle(owner, "KA01AB1234");
        MockHttpSession session = login(owner.getPhoneNumber());
        MaintenanceRecord record = createRecord(vehicle, LocalDate.now(clock).minusDays(1), 100, "1.00");

        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance", vehicle.getId())
                        .session(session)
                        .param("serviceDate", LocalDate.now(clock).toString())
                        .param("odometerKm", "101")
                        .param("serviceType", "REPAIRS")
                        .param("serviceCostInr", "1.00"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance/{recordId}",
                        vehicle.getId(), record.getId())
                        .session(session)
                        .param("serviceDate", LocalDate.now(clock).minusDays(1).toString())
                        .param("odometerKm", "100")
                        .param("serviceType", "REPAIRS")
                        .param("serviceCostInr", "1.00"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/vehicles/{vehicleId}/maintenance/{recordId}/delete",
                        vehicle.getId(), record.getId())
                        .session(session))
                .andExpect(status().isForbidden());

        assertEquals(1, maintenanceRecordRepository.count());
    }

    private UserAccount saveUser(String phoneNumber) {
        return userAccountRepository.saveAndFlush(new UserAccount(
                "Maintenance Owner",
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

    private MaintenanceRecord createRecord(
            Vehicle vehicle,
            LocalDate date,
            long odometer,
            String cost) {
        MaintenanceForm form = new MaintenanceForm();
        form.setServiceDate(date);
        form.setOdometerKm(odometer);
        form.setServiceType(ServiceType.GENERAL_SERVICE);
        form.setServiceCostInr(new BigDecimal(cost));
        return maintenanceRecordRepository.saveAndFlush(new MaintenanceRecord(vehicle, form));
    }

    private String createRecord(
            MockHttpSession session,
            Vehicle vehicle,
            LocalDate date,
            long odometer,
            String type,
            String cost) throws Exception {
        return mockMvc.perform(post("/vehicles/{vehicleId}/maintenance", vehicle.getId())
                        .session(session)
                        .with(csrf())
                        .param("serviceDate", date.toString())
                        .param("odometerKm", Long.toString(odometer))
                        .param("serviceType", type)
                        .param("serviceCostInr", cost))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
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
