package com.example.backend.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryIntegrationTest extends BaseIntegrationTest {

    private static final String CATEGORIES_URL = "/api/categories";

    @Test
    void getCategories_returnsAllCategoryNames() throws Exception {
        createCategory("Concert");
        createCategory("Sports");
        createCategory("Theatre");

        mockMvc.perform(get(CATEGORIES_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$", containsInAnyOrder("Concert", "Sports", "Theatre")));
    }

    @Test
    void getCategories_returnsEmptyListWhenNoCategories() throws Exception {
        mockMvc.perform(get(CATEGORIES_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
