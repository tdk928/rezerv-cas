package bg.rezerv.cas.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bg.rezerv.cas.service.CompanyAssignmentService;
import bg.rezerv.cas.web.dto.UserResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternalUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyAssignmentService companyAssignmentService;

    @Test
    void assignCompanyReturnsUpdatedUser() throws Exception {
        when(companyAssignmentService.assignCompany(eq(42L), eq(100L)))
                .thenReturn(new UserResponse(
                        42L, "ivan@example.bg", null, "Иван", "Иванов", 100L,
                        List.of(100L), "ACTIVE", List.of("CLIENT", "BUSINESS_OWNER"),
                        Instant.parse("2026-07-15T00:00:00Z")));

        mockMvc.perform(post("/internal/users/42/assign-company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(100))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles.length()").value(2));
    }
}
