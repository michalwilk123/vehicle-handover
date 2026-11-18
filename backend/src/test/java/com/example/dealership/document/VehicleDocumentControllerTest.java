package com.example.dealership.document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.example.dealership.config.ClockConfig;
import com.example.dealership.error.ConflictException;
import com.example.dealership.error.NotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VehicleDocumentController.class)
@Import(VehicleDocumentControllerTest.FixedClock.class)
class VehicleDocumentControllerTest {

    @TestConfiguration
    static class FixedClock {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ClockConfig.DEALERSHIP_ZONE);
        }
    }

    private static final String VALID_BODY = """
            {
              "type": "OC_POLICY",
              "number": "OC/2026/001",
              "issueDate": "2026-01-10",
              "expiryDate": "2027-01-09"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleDocumentService documentService;

    @Test
    void findAllReturnsDocumentsOfVehicle() throws Exception {
        when(documentService.findAll(1L)).thenReturn(List.of(response(DocumentStatus.PENDING)));

        mockMvc.perform(get("/api/vehicles/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].type").value("OC_POLICY"))
                .andExpect(jsonPath("$[0].number").value("OC/2026/001"))
                .andExpect(jsonPath("$[0].issueDate").value("2026-01-10"))
                .andExpect(jsonPath("$[0].expiryDate").value("2027-01-09"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void findAllReturnsNotFoundProblemWhenVehicleDoesNotExist() throws Exception {
        when(documentService.findAll(99L)).thenThrow(new NotFoundException("Vehicle with id 99 not found"));

        mockMvc.perform(get("/api/vehicles/99/documents"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Vehicle with id 99 not found"));
    }

    @Test
    void createReturnsCreatedDocument() throws Exception {
        when(documentService.create(eq(1L), any(VehicleDocumentRequest.class)))
                .thenReturn(response(DocumentStatus.PENDING));

        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/vehicles/1/documents/5"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createReturnsBadRequestProblemWithFieldErrorsWhenRequestIsInvalid() throws Exception {
        String body = """
                { "number": "  ", "expiryDate": "2027-01-09" }
                """;

        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[0].field").value("issueDate"))
                .andExpect(jsonPath("$.errors[1].field").value("number"))
                .andExpect(jsonPath("$.errors[2].field").value("type"));
        verifyNoInteractions(documentService);
    }

    @Test
    void createReturnsBadRequestWhenNumberIsLongerThan100CharactersAfterTrimming() throws Exception {
        String body = VALID_BODY.replace("OC/2026/001", "  " + "N".repeat(101) + "  ");

        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("number"))
                .andExpect(jsonPath("$.errors[0].message").value("size must be between 0 and 100"));
        verifyNoInteractions(documentService);
    }

    @Test
    void createAcceptsIssueDateOfToday() throws Exception {
        when(documentService.create(eq(1L), any(VehicleDocumentRequest.class)))
                .thenReturn(response(DocumentStatus.PENDING));
        String body = VALID_BODY.replace("2026-01-10", "2026-06-15");

        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createReturnsBadRequestProblemWhenIssueDateIsInTheFuture() throws Exception {
        String body = VALID_BODY.replace("2026-01-10", "2026-06-16");

        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("issueDate"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be after today"));
        verifyNoInteractions(documentService);
    }

    @Test
    void createReturnsConflictProblemWhenTypeHasActiveDocument() throws Exception {
        when(documentService.create(eq(1L), any(VehicleDocumentRequest.class)))
                .thenThrow(new ConflictException("Vehicle 1 already has an active OC_POLICY document"));

        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Vehicle 1 already has an active OC_POLICY document"));
    }

    @Test
    void createReturnsBadRequestProblemWhenTypeIsUnknown() throws Exception {
        String body = VALID_BODY.replace("OC_POLICY", "PASSPORT");

        mockMvc.perform(post("/api/vehicles/1/documents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("type"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("must be one of: SALES_AGREEMENT, INVOICE, COC, OC_POLICY, REGISTRATION_POA"));
        verifyNoInteractions(documentService);
    }

    @Test
    void changeStatusReturnsDocumentWithNewStatus() throws Exception {
        when(documentService.changeStatus(eq(1L), eq(5L), any(DocumentStatusRequest.class)))
                .thenReturn(response(DocumentStatus.VERIFIED));

        mockMvc.perform(put("/api/vehicles/1/documents/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"VERIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    void changeStatusReturnsBadRequestWhenStatusIsMissingOrUnknown() throws Exception {
        mockMvc.perform(put("/api/vehicles/1/documents/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be null"));
        mockMvc.perform(put("/api/vehicles/1/documents/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"LOST\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"))
                .andExpect(jsonPath("$.errors[0].message").value("must be one of: PENDING, VERIFIED, REJECTED"));
        verifyNoInteractions(documentService);
    }

    @Test
    void changeStatusReturnsNotFoundProblemForDocumentOfAnotherVehicle() throws Exception {
        when(documentService.changeStatus(eq(2L), eq(5L), any(DocumentStatusRequest.class)))
                .thenThrow(new NotFoundException("Document with id 5 not found for vehicle 2"));

        mockMvc.perform(put("/api/vehicles/2/documents/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"VERIFIED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Document with id 5 not found for vehicle 2"));
    }

    @Test
    void changeStatusReturnsConflictProblemWhenServiceRejectsIt() throws Exception {
        when(documentService.changeStatus(eq(1L), eq(5L), any(DocumentStatusRequest.class)))
                .thenThrow(new ConflictException("conflict"));

        mockMvc.perform(put("/api/vehicles/1/documents/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"VERIFIED\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private static VehicleDocumentResponse response(DocumentStatus status) {
        return new VehicleDocumentResponse(5L, DocumentType.OC_POLICY, "OC/2026/001",
                LocalDate.of(2026, 1, 10), LocalDate.of(2027, 1, 9), status);
    }
}
