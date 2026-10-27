package com.opslog;

import com.opslog.common.SpaController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SpaRoutesTests {

    @Autowired MockMvc mvc;

    @Test
    void everyReactRouteForwardsToIndexWithoutAToken() throws Exception {
        for(String route : SpaController.ROUTES) {
            mvc.perform(get(route))
                    .andExpect(status().isOk())
                    .andExpect(forwardedUrl("/index.html"));
        }
    }

    @Test
    void apiStillRequiresAToken() throws Exception {
        mvc.perform(get("/api/locations")).andExpect(status().isUnauthorized());
    }
}
