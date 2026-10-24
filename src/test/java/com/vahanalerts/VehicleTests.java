package com.vahanalerts;

import java.time.LocalDate;

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
class VehicleTests {

    private static final String PASSWORD = "CorrectHorseBattery12";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearVehiclesAndUsers() {
        vehicleRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @AfterEach
    void removeVehiclesAndUsers() {
        vehicleRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void authenticatedUserCanCreateListViewEditAndDeleteVehicle() throws Exception {
        UserAccount user = saveUser("+919876543210");
        MockHttpSession session = login(user.getPhoneNumber());

        String createRedirect = mockMvc.perform(post("/vehicles")
                        .session(session)
                        .with(csrf())
                        .param("nickname", "Family car")
                        .param("registrationNumber", " ka 01 ab 1234 ")
                        .param("type", "CAR")
                        .param("make", "Tata")
                        .param("model", "Nexon")
                        .param("manufacturingYear", "2022")
                        .param("fuelType", "ELECTRIC")
                        .param("notes", "Home charging"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        assertNotNull(createRedirect);
        Long vehicleId = Long.valueOf(createRedirect.substring(createRedirect.lastIndexOf('/') + 1));
        Vehicle created = vehicleRepository.findById(vehicleId).orElseThrow();
        assertEquals("KA01AB1234", created.getRegistrationNumber());
        assertEquals(user.getId(), created.getOwner().getId());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());

        mockMvc.perform(get("/vehicles/new").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add a vehicle")));

        mockMvc.perform(get("/vehicles").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Family car")))
                .andExpect(content().string(containsString("KA01AB1234")));

        mockMvc.perform(get("/vehicles/{id}/edit", vehicleId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit vehicle")));

        mockMvc.perform(get("/vehicles/{id}", vehicleId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Home charging")));

        mockMvc.perform(get("/vehicles/{id}/delete", vehicleId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Delete this vehicle?")));

        mockMvc.perform(post("/vehicles/{id}", vehicleId)
                        .session(session)
                        .with(csrf())
                        .param("nickname", "Commuter")
                        .param("registrationNumber", "KA01AB1234")
                        .param("type", "CAR")
                        .param("make", "Tata")
                        .param("model", "Nexon EV"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles/" + vehicleId));

        mockMvc.perform(get("/vehicles/{id}", vehicleId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Commuter")))
                .andExpect(content().string(containsString("Nexon EV")));

        mockMvc.perform(post("/vehicles/{id}/delete", vehicleId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"));

        assertFalse(vehicleRepository.existsById(vehicleId));
    }

    @Test
    void anonymousUserCannotAccessVehicleManagementPages() throws Exception {
        mockMvc.perform(get("/vehicles")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/vehicles/new")).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/vehicles").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void vehicleChangesRequireCsrfToken() throws Exception {
        UserAccount user = saveUser("+919876543210");
        MockHttpSession session = login(user.getPhoneNumber());

        mockMvc.perform(post("/vehicles")
                        .session(session)
                        .param("registrationNumber", "KA01AB1234")
                        .param("type", "CAR"))
                .andExpect(status().isForbidden());

        assertEquals(0, vehicleRepository.count());
    }

    @Test
    void userCannotViewEditUpdateOrDeleteAnotherUsersVehicle() throws Exception {
        UserAccount owner = saveUser("+919876543210");
        UserAccount otherUser = saveUser("+919876543211");
        MockHttpSession otherUserSession = login(otherUser.getPhoneNumber());
        Vehicle vehicle = createVehicle(owner);

        mockMvc.perform(get("/vehicles/{id}", vehicle.getId()).session(otherUserSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/vehicles/{id}/edit", vehicle.getId()).session(otherUserSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/vehicles/{id}/delete", vehicle.getId()).session(otherUserSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/vehicles/{id}", vehicle.getId())
                        .session(otherUserSession)
                        .with(csrf())
                        .param("registrationNumber", "EVIL123")
                        .param("type", "CAR"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/vehicles/{id}/delete", vehicle.getId())
                        .session(otherUserSession)
                        .with(csrf()))
                .andExpect(status().isNotFound());

        Vehicle unchangedVehicle = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals("KA01AB1234", unchangedVehicle.getRegistrationNumber());
        assertEquals(owner.getId(), unchangedVehicle.getOwner().getId());
    }

    @Test
    void duplicateNormalizedRegistrationIsRejectedForSameUser() throws Exception {
        UserAccount user = saveUser("+919876543210");
        MockHttpSession session = login(user.getPhoneNumber());
        createVehicle(user);

        mockMvc.perform(post("/vehicles")
                        .session(session)
                        .with(csrf())
                        .param("registrationNumber", "ka 01 ab 1234")
                        .param("type", "MOTORCYCLE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "This registration number is already in your vehicles.")));

        assertEquals(1, vehicleRepository.count());
    }

    @Test
    void differentUsersCanUseSameNormalizedRegistration() throws Exception {
        UserAccount firstUser = saveUser("+919876543210");
        UserAccount secondUser = saveUser("+919876543211");
        createVehicle(firstUser);
        MockHttpSession secondUserSession = login(secondUser.getPhoneNumber());

        mockMvc.perform(post("/vehicles")
                        .session(secondUserSession)
                        .with(csrf())
                        .param("registrationNumber", "ka 01 ab 1234")
                        .param("type", "CAR"))
                .andExpect(status().is3xxRedirection());

        assertEquals(2, vehicleRepository.count());
    }

    @Test
    void invalidVehicleInputShowsUsefulValidationMessage() throws Exception {
        UserAccount user = saveUser("+919876543210");
        MockHttpSession session = login(user.getPhoneNumber());

        mockMvc.perform(post("/vehicles")
                        .session(session)
                        .with(csrf())
                        .param("registrationNumber", "!")
                        .param("type", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Use letters, numbers, spaces, or hyphens")))
                .andExpect(content().string(containsString("Choose a vehicle type.")));

        assertEquals(0, vehicleRepository.count());
    }

    private UserAccount saveUser(String phoneNumber) {
        return userAccountRepository.saveAndFlush(new UserAccount(
                "Test Owner",
                PhoneNumber.normalize(phoneNumber),
                LocalDate.of(1990, 5, 15),
                passwordEncoder.encode(PASSWORD),
                false
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

    private Vehicle createVehicle(UserAccount owner) {
        VehicleForm form = new VehicleForm();
        form.setRegistrationNumber("KA 01 AB 1234");
        form.setType(VehicleType.CAR);
        return vehicleRepository.saveAndFlush(new Vehicle(owner, form, "KA01AB1234"));
    }
}
