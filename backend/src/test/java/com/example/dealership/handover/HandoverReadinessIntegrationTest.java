package com.example.dealership.handover;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import java.time.Clock;
import java.time.LocalDate;

import com.example.dealership.FixedClockTestConfig;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Readiness through the public API: vehicle, documents and status changes stored in the database. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockTestConfig.class)
class HandoverReadinessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Clock clock;

    @Test
    void readinessFollowsDocumentsHandoverDateAndRegistrationChoice() throws Exception {
        LocalDate today = LocalDate.now(clock);
        long vehicleId = createVehicle("TESTRDY0000000001", today.plusDays(10));
        for (String type : new String[] {"SALES_AGREEMENT", "INVOICE", "COC", "OC_POLICY"}) {
            long documentId = addDocument(vehicleId, type, today, today.plusYears(1));
            if (!type.equals("COC")) {
                verify(vehicleId, documentId);
            }
        }

        mockMvc.perform(get(readinessUrl(vehicleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.missingDocumentTypes.length()").value(0))
                .andExpect(jsonPath("$.problems.length()").value(1))
                .andExpect(jsonPath("$.problems[0].documentType").value("COC"))
                .andExpect(jsonPath("$.problems[0].reason").value("NOT_VERIFIED"));

        long coc = JsonPath.<Number>read(mockMvc.perform(get("/api/vehicles/" + vehicleId + "/documents"))
                .andReturn().getResponse().getContentAsString(), "$[2].id").longValue();
        verify(vehicleId, coc);
        mockMvc.perform(get(readinessUrl(vehicleId)))
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.problems.length()").value(0));

        mockMvc.perform(put("/api/vehicles/" + vehicleId + "/documents/" + coc + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PENDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(coc))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get(readinessUrl(vehicleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.missingDocumentTypes.length()").value(0))
                .andExpect(jsonPath("$.problems.length()").value(1))
                .andExpect(jsonPath("$.problems[0].documentType").value("COC"))
                .andExpect(jsonPath("$.problems[0].reason").value("NOT_VERIFIED"));
        verify(vehicleId, coc);

        // The dealership now registers the vehicle, and the handover date is removed.
        mockMvc.perform(put("/api/vehicles/" + vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleBody("TESTRDY0000000001", null, true)))
                .andExpect(status().isOk());
        mockMvc.perform(get(readinessUrl(vehicleId)))
                .andExpect(jsonPath("$.ready").value(false))
                .andExpect(jsonPath("$.missingDocumentTypes[0]").value("REGISTRATION_POA"))
                .andExpect(jsonPath("$.problems.length()").value(1))
                .andExpect(jsonPath("$.problems[0].documentType").value(nullValue()))
                .andExpect(jsonPath("$.problems[0].reason").value("NO_HANDOVER_DATE"));

        mockMvc.perform(get(readinessUrl(999_999))).andExpect(status().isNotFound());
    }

    private long createVehicle(String vin, LocalDate handoverDate) throws Exception {
        String body = mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleBody(vin, handoverDate, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dealershipRegisters").value(false))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.id").longValue();
    }

    private long addDocument(long vehicleId, String type, LocalDate issueDate, LocalDate expiryDate) throws Exception {
        String body = mockMvc.perform(post("/api/vehicles/" + vehicleId + "/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "%s", "number": "N-1", "issueDate": "%s", "expiryDate": "%s" }
                                """.formatted(type, issueDate, expiryDate)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.id").longValue();
    }

    private void verify(long vehicleId, long documentId) throws Exception {
        mockMvc.perform(put("/api/vehicles/" + vehicleId + "/documents/" + documentId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"VERIFIED\"}"))
                .andExpect(status().isOk());
    }

    private static String readinessUrl(long vehicleId) {
        return "/api/vehicles/" + vehicleId + "/handover-readiness";
    }

    private static String vehicleBody(String vin, LocalDate handoverDate, Boolean dealershipRegisters) {
        return """
                {
                  "vin": "%s", "make": "Skoda", "model": "Fabia", "productionYear": 2026, "fuelType": "PETROL",
                  "plannedHandoverDate": %s, "dealershipRegisters": %s
                }
                """.formatted(vin, handoverDate == null ? "null" : "\"" + handoverDate + "\"", dealershipRegisters);
    }
}
