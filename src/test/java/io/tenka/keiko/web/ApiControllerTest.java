package io.tenka.keiko.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end smoke test through the REST layer. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void poolSummaryReturnsBothDeckCounts() throws Exception {
        // Bumps in lockstep with CLAUDE.md §5.2 — every PR adds 50 cards.
        mvc.perform(get("/api/pool-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.default").value(350))
                .andExpect(jsonPath("$.experimental").value(350));
    }

    @Test
    void nextReturnsACardWithStrippedIsCorrectFlag() throws Exception {
        mvc.perform(get("/api/next"))
                .andExpect(status().isOk())
                // Front face must NOT leak is_correct on choices
                .andExpect(jsonPath("$.choices").isArray())
                .andExpect(jsonPath("$.choices[0].is_correct").doesNotExist())
                // Stem and id must be present
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.stem").exists());
    }

    @Test
    void motivationReturnsSomethingFromZhPool() throws Exception {
        mvc.perform(get("/api/motivation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").exists());
    }
}
