package com.tukac.controller;

import com.tukac.dto.ApiResponse;
import com.tukac.repository.TransactionRepository;
import com.tukac.repository.UserRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('CHAIRPERSON','VICE-CHAIRPERSON','TREASURER','SECRETARY')")
public class ReportController {

    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;

    @GetMapping("/users")
    public ResponseEntity<byte[]> generateUsersReport() {
        try {
            InputStream reportStream = getClass().getResourceAsStream("/reports/users_report.jrxml");
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
            
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(userRepository.findAll());
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("ReportTitle", "TUKAC - User Membership List");
            parameters.put("GeneratedBy", "System Admin");
            parameters.put("GeneratedDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            byte[] data = JasperExportManager.exportReportToPdf(jasperPrint);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users_report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/finances")
    public ResponseEntity<byte[]> generateFinanceReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            InputStream reportStream = getClass().getResourceAsStream("/reports/finance_report.jrxml");
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
            
            java.util.List<com.tukac.model.Transaction> dataList;
            if (startDate != null && endDate != null && !startDate.isBlank() && !endDate.isBlank()) {
                dataList = transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(startDate, endDate);
            } else {
                dataList = transactionRepository.findAllByOrderByTransactionDateDesc();
            }

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(dataList);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("ReportTitle", "TUKAC - Financial Statement");
            parameters.put("GeneratedBy", "Treasury Office");
            parameters.put("GeneratedDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
            
            Double balance = transactionRepository.calculateBalance();
            parameters.put("NetBalance", balance != null ? balance : 0.0);

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            byte[] data = JasperExportManager.exportReportToPdf(jasperPrint);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=finance_report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
