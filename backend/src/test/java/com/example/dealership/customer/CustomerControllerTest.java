package com.example.dealership.customer;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.example.dealership.error.ConflictException;
import com.example.dealership.error.NotFoundException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    private static final String EMAIL = "jan.kowalski@example.com";
    private static final String VALID_BODY = """
            {
              "firstName": "Jan",
              "lastName": "Kowalski",
              "email": "jan.kowalski@example.com",
              "phone": "+48 600 100 200",
              "nip": "1234567890"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void findAllReturnsCustomers() throws Exception {
        when(customerService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Jan"))
                .andExpect(jsonPath("$[0].lastName").value("Kowalski"))
                .andExpect(jsonPath("$[0].email").value(EMAIL))
                .andExpect(jsonPath("$[0].phone").value("+48 600 100 200"))
                .andExpect(jsonPath("$[0].nip").value("1234567890"));
    }

    @Test
    void findByIdReturnsCustomer() throws Exception {
        when(customerService.findById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void findByIdReturnsNotFoundProblemWhenCustomerDoesNotExist() throws Exception {
        when(customerService.findById(99L)).thenThrow(new NotFoundException("Customer with id 99 not found"));

        mockMvc.perform(get("/api/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Customer with id 99 not found"));
    }

    @Test
    void createReturnsCreatedCustomer() throws Exception {
        when(customerService.create(any(CustomerRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/customers/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createTrimsValuesAndTreatsBlankNipAsNone() throws Exception {
        when(customerService.create(any(CustomerRequest.class))).thenReturn(response());
        String body = """
                {
                  "firstName": "  Jan ",
                  "lastName": " Kowalski",
                  "email": " Jan.Kowalski@Example.com  ",
                  "phone": " 600-100-200 ",
                  "nip": "   "
                }
                """;

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        ArgumentCaptor<CustomerRequest> captor = ArgumentCaptor.forClass(CustomerRequest.class);
        verify(customerService).create(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue()).isEqualTo(
                new CustomerRequest("Jan", "Kowalski", "Jan.Kowalski@Example.com", "600-100-200", null));
    }

    @Test
    void createAppliesLengthLimitsAfterTrimming() throws Exception {
        String fiftyLetters = "A".repeat(50);
        String body = VALID_BODY.replace("\"Jan\"", "\"  " + fiftyLetters + "  \"");
        when(customerService.create(any(CustomerRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createReturnsBadRequestProblemWithFieldErrorsWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(invalidBody()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(5))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("must be a well-formed email address"))
                .andExpect(jsonPath("$.errors[1].field").value("firstName"))
                .andExpect(jsonPath("$.errors[1].message").value("must not be blank"))
                .andExpect(jsonPath("$.errors[2].field").value("lastName"))
                .andExpect(jsonPath("$.errors[2].message").value("size must be between 0 and 50"))
                .andExpect(jsonPath("$.errors[3].field").value("nip"))
                .andExpect(jsonPath("$.errors[3].message").value("must be 10 digits"))
                .andExpect(jsonPath("$.errors[4].field").value("phone"))
                .andExpect(jsonPath("$.errors[4].message")
                        .value("must contain a digit and only digits, spaces, +, -, . and parentheses"));
        verifyNoInteractions(customerService);
    }

    @Test
    void createReturnsBadRequestWhenPhoneHasNoDigit() throws Exception {
        String body = VALID_BODY.replace("+48 600 100 200", "+ - ()");

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("phone"));
        verifyNoInteractions(customerService);
    }

    @Test
    void createReturnsBadRequestWhenEmailIsLongerThan254Characters() throws Exception {
        String longEmail = "a".repeat(60) + "@" + ("b".repeat(60) + ".").repeat(3) + "example.com";
        String body = VALID_BODY.replace(EMAIL, longEmail);

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
        verifyNoInteractions(customerService);
    }

    @Test
    void createReturnsBadRequestProblemWhenBodyIsNotJson() throws Exception {
        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value(nullValue()))
                .andExpect(jsonPath("$.errors[0].message").value("request body must be valid JSON"));
        verifyNoInteractions(customerService);
    }

    @Test
    void createReturnsConflictProblemWhenEmailAlreadyExists() throws Exception {
        when(customerService.create(any(CustomerRequest.class)))
                .thenThrow(new ConflictException("Customer with email " + EMAIL + " already exists"));

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Customer with email " + EMAIL + " already exists"));
    }

    @Test
    void createReturnsConflictProblemWhenDatabaseConstraintIsViolated() throws Exception {
        when(customerService.create(any(CustomerRequest.class)))
                .thenThrow(new DataIntegrityViolationException("uq_customer_email_key"));

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Request conflicts with existing data"));
    }

    @Test
    void updateReturnsUpdatedCustomer() throws Exception {
        when(customerService.update(eq(1L), any(CustomerRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/customers/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void updateReturnsNotFoundProblemWhenCustomerDoesNotExist() throws Exception {
        when(customerService.update(eq(99L), any(CustomerRequest.class)))
                .thenThrow(new NotFoundException("Customer with id 99 not found"));

        mockMvc.perform(put("/api/customers/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Customer with id 99 not found"));
    }

    @Test
    void updateReturnsBadRequestProblemWithFieldErrorsWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(put("/api/customers/1").contentType(MediaType.APPLICATION_JSON).content(invalidBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(5));
        verifyNoInteractions(customerService);
    }

    @Test
    void updateReturnsConflictProblemWhenEmailBelongsToAnotherCustomer() throws Exception {
        when(customerService.update(eq(1L), any(CustomerRequest.class)))
                .thenThrow(new ConflictException("Customer with email " + EMAIL + " already exists"));

        mockMvc.perform(put("/api/customers/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isNoContent());

        verify(customerService).delete(1L);
    }

    @Test
    void deleteReturnsNotFoundProblemWhenCustomerDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Customer with id 99 not found")).when(customerService).delete(99L);

        mockMvc.perform(delete("/api/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Customer with id 99 not found"));
    }

    private static String invalidBody() {
        return """
                {
                  "firstName": "   ",
                  "lastName": "%s",
                  "email": "not-an-email",
                  "phone": "call me",
                  "nip": "123-456-78"
                }
                """.formatted("K".repeat(51));
    }

    private static CustomerResponse response() {
        return new CustomerResponse(1L, "Jan", "Kowalski", EMAIL, "+48 600 100 200", "1234567890");
    }
}
