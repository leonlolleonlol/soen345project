package com.example.backend.integration;

import com.example.backend.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends BaseIntegrationTest {

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL = "/api/auth/login";

    @Test
    void register_withValidEmail_returns201AndUserFields() throws Exception {
        String body = """
            {
                "firstName": "Jane",
                "lastName": "Doe",
                "email": "jane@example.com",
                "phoneNumber": null,
                "password": "securePass1"
            }
            """;

        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("jane@example.com"))
            .andExpect(jsonPath("$.firstName").value("Jane"))
            .andExpect(jsonPath("$.role").value("CUSTOMER"))
            .andExpect(jsonPath("$.userId").isNumber());
    }

    @Test
    void register_withValidPhoneOnly_returns201() throws Exception {
        String body = """
            {
                "firstName": "Bob",
                "lastName": "Smith",
                "email": null,
                "phoneNumber": "+15141234567",
                "password": "securePass1"
            }
            """;

        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.phoneNumber").value("+15141234567"))
            .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        createUser("duplicate@example.com", null, UserRole.CUSTOMER);

        String body = """
            {
                "firstName": "Alice",
                "lastName": "Smith",
                "email": "duplicate@example.com",
                "phoneNumber": null,
                "password": "securePass1"
            }
            """;

        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void register_withDuplicatePhone_returns409() throws Exception {
        createUser(null, "+15140000001", UserRole.CUSTOMER);

        String body = """
            {
                "firstName": "Alice",
                "lastName": "Smith",
                "email": null,
                "phoneNumber": "+15140000001",
                "password": "securePass1"
            }
            """;

        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void register_withNoEmailAndNoPhone_returns400() throws Exception {
        String body = """
            {
                "firstName": "Alice",
                "lastName": "Smith",
                "email": null,
                "phoneNumber": null,
                "password": "securePass1"
            }
            """;

        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_withShortPassword_returns400() throws Exception {
        String body = """
            {
                "firstName": "Alice",
                "lastName": "Smith",
                "email": "alice@example.com",
                "phoneNumber": null,
                "password": "short"
            }
            """;

        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_withValidEmailAndPassword_returns200AndUserFields() throws Exception {
        createUser("login@example.com", null, UserRole.CUSTOMER);

        String body = """
            {
                "email": "login@example.com",
                "password": "Password1!"
            }
            """;

        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("login@example.com"))
            .andExpect(jsonPath("$.role").value("CUSTOMER"))
            .andExpect(jsonPath("$.userId").isNumber());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        createUser("user@example.com", null, UserRole.CUSTOMER);

        String body = """
            {
                "email": "user@example.com",
                "password": "WrongPassword99"
            }
            """;

        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withNonExistentUser_returns401() throws Exception {
        String body = """
            {
                "email": "ghost@example.com",
                "password": "Password1!"
            }
            """;

        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }
}
