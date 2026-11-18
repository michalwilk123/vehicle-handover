package com.example.dealership.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.dealership.FixedClockTestConfig;
import com.example.dealership.vehicle.FuelType;
import com.example.dealership.vehicle.Vehicle;
import com.example.dealership.vehicle.VehicleRepository;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Runs the document acceptance checks from INTENT.md against the real database. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockTestConfig.class)
class VehicleDocumentIntegrationTest {

    private static final AtomicInteger VIN_SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Clock clock;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleDocumentRepository documentRepository;

    @Test
    void documentLifecycleFollowsTheStatusRules() throws Exception {
        long vehicleId = newVehicle();
        LocalDate today = LocalDate.now(clock);

        long coc = addDocument(vehicleId, "COC", today.toString(), 201);
        mockMvc.perform(get(documentsUrl(vehicleId)))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
        addDocument(vehicleId, "COC", today.toString(), 409);
        changeStatus(vehicleId, coc, "VERIFIED", 200);
        addDocument(vehicleId, "COC", today.toString(), 409);
        addDocument(vehicleId, "INVOICE", today.plusDays(1).toString(), 400);

        long invoice = addDocument(vehicleId, "INVOICE", today.toString(), 201);
        changeStatus(vehicleId, invoice, "REJECTED", 200);
        changeStatus(vehicleId, invoice, "VERIFIED", 409);
        changeStatus(vehicleId, invoice, "REJECTED", 409);
        long replacement = addDocument(vehicleId, "INVOICE", today.toString(), 201);
        mockMvc.perform(get(documentsUrl(vehicleId)))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].id").value(invoice))
                .andExpect(jsonPath("$[1].status").value("REJECTED"))
                .andExpect(jsonPath("$[2].id").value(replacement))
                .andExpect(jsonPath("$[2].status").value("PENDING"));

        changeStatus(vehicleId, coc, "REJECTED", 200);
        addDocument(vehicleId, "COC", today.toString(), 201);

        long otherVehicleId = newVehicle();
        changeStatus(otherVehicleId, coc, "VERIFIED", 404);
        changeStatus(999_999, coc, "VERIFIED", 404);

        mockMvc.perform(delete("/api/vehicles/" + vehicleId)).andExpect(status().isNoContent());
        assertThat(documentRepository.findByVehicleIdOrderByIdAsc(vehicleId)).isEmpty();
        mockMvc.perform(get(documentsUrl(vehicleId))).andExpect(status().isNotFound());
    }

    @Test
    void parallelAddsOfTheSameTypeCreateOneActiveDocument() throws Exception {
        long vehicleId = newVehicle();
        int attempts = 8;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        try {
            List<Callable<Integer>> calls = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                calls.add(() -> mockMvc.perform(post(documentsUrl(vehicleId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(documentBody("OC_POLICY", "2026-01-10")))
                        .andReturn().getResponse().getStatus());
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : executor.invokeAll(calls)) {
                statuses.add(result.get());
            }

            assertThat(statuses).containsOnly(201, 409);
            assertThat(statuses).filteredOn(code -> code == 201).hasSize(1);
            assertThat(documentRepository.findByVehicleIdOrderByIdAsc(vehicleId)).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private long newVehicle() {
        String vin = "TESTDOC%010d".formatted(VIN_SEQUENCE.incrementAndGet());
        return vehicleRepository.save(new Vehicle(vin, "Skoda", "Fabia", 2026, FuelType.PETROL, null)).getId();
    }

    private long addDocument(long vehicleId, String type, String issueDate, int expectedStatus) throws Exception {
        String body = mockMvc.perform(post(documentsUrl(vehicleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentBody(type, issueDate)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return expectedStatus == 201 ? ((Number) JsonPath.read(body, "$.id")).longValue() : -1;
    }

    private void changeStatus(long vehicleId, long documentId, String status, int expectedStatus) throws Exception {
        mockMvc.perform(put(documentsUrl(vehicleId) + "/" + documentId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"" + status + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private static String documentsUrl(long vehicleId) {
        return "/api/vehicles/" + vehicleId + "/documents";
    }

    private static String documentBody(String type, String issueDate) {
        return """
                { "type": "%s", "number": "N-1", "issueDate": "%s" }
                """.formatted(type, issueDate);
    }
}
