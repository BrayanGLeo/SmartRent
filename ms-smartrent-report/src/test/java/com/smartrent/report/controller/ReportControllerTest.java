package com.smartrent.report.controller;

import com.smartrent.report.service.ReportService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    void getKpis_returnsDailyRentals() throws Exception {
        Mockito.when(reportService.getDailyRentals()).thenReturn(10);

        mockMvc.perform(get("/api/report/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyRentals").value(10));
    }

    @Test
    void getTopServices_returnsList() throws Exception {
        Mockito.when(reportService.getTopServices()).thenReturn(List.of(
                Map.entry("Excavadora", 5)
        ));

        mockMvc.perform(get("/api/report/top-services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].Excavadora").value(5));
    }
}
