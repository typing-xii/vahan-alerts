package com.vahanalerts;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VahanAlertsApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void applicationContextStarts() {
    }

    @Test
    void homePageRendersVahanAlerts() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vahan Alerts")));
    }

    @Test
    void healthEndpointReturnsExpectedJson() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("Vahan Alerts"));
    }

    @Test
    void unknownRouteRendersNotFoundPage() throws Exception {
        mockMvc.perform(get("/route-that-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("That road doesn't lead anywhere.")));
    }
}
