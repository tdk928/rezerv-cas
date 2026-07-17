package bg.rezerv.cas.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bg.rezerv.cas.service.CompanyAssignmentService;
import bg.rezerv.cas.service.UserQueryService;
import bg.rezerv.cas.web.dto.UserResponse;
import bg.rezerv.cas.web.dto.UserSummaryResponse;
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

    @MockitoBean
    private UserQueryService userQueryService;

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

    @Test
    void getUserReturnsSummary() throws Exception {
        when(userQueryService.getById(5L))
                .thenReturn(new UserSummaryResponse(5L, "o@example.bg", "Оля", "Петрова"));

        mockMvc.perform(get("/internal/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("o@example.bg"))
                .andExpect(jsonPath("$.firstName").value("Оля"));
    }

    @Test
    void lookupUsersReturnsList() throws Exception {
        when(userQueryService.findByIds(List.of(1L, 2L)))
                .thenReturn(List.of(new UserSummaryResponse(1L, "a@b.bg", "A", "B")));

        mockMvc.perform(post("/internal/users/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
