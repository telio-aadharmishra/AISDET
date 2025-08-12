package UI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class PageObjectClassifier {

    private static final String OPENAI_API_KEY = "OPENAI_API_KEY";
    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("StoryInput.xlsx");
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet("Input");
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Cell gherkinCell = row.getCell(4); // Column E: Gherkin
            Cell pageObjCell = row.getCell(9); // Column J (0-based index)

            if (gherkinCell == null || gherkinCell.getStringCellValue().trim().isEmpty()) continue;
            if (pageObjCell != null && !pageObjCell.getStringCellValue().trim().isEmpty()) continue;

            String gherkin = gherkinCell.getStringCellValue().trim();

            try {
                String pageObject = identifyPageObjectFromGherkin(gherkin);
                row.createCell(9).setCellValue(pageObject);
            } catch (Exception e) {
                row.createCell(9).setCellValue("❌ Failed: " + e.getMessage());
                System.err.println("❌ Failed at row " + (i + 1) + ": " + e.getMessage());
            }
        }

        fis.close();
        FileOutputStream fos = new FileOutputStream("StoryInput.xlsx");
        workbook.write(fos);
        fos.close();
        workbook.close();

        System.out.println("✔️ Page Object classification completed.");
    }

    private static String identifyPageObjectFromGherkin(String gherkin) throws Exception {
        String prompt =
            "You are a QA Automation Architect.\n\n" +
            "Given the following Gherkin test case, determine the most relevant Page Object class name " +
            "(such as HomePage, DashboardPage, LoginPage, SettingsPage, etc.) where the majority of the steps would belong.\n\n" +
            "Return only the page object class name, without any explanation.\n\n" +
            "Gherkin Test Case:\n" + gherkin;

        ObjectMapper mapper = new ObjectMapper();

        String requestBody = mapper.writeValueAsString(
                Map.of(
                        "model", "gpt-4",
                        "messages", List.of(
                                Map.of("role", "user", "content", prompt)
                        )
                )
        );

        URL url = URI.create("https://api.openai.com/v1/chat/completions").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        if (conn.getResponseCode() != 200) {
            Scanner errorScanner = new Scanner(conn.getErrorStream());
            StringBuilder errorResponse = new StringBuilder();
            while (errorScanner.hasNext()) errorResponse.append(errorScanner.nextLine());
            errorScanner.close();
            throw new RuntimeException("OpenAI API error: " + errorResponse);
        }

        Scanner sc = new Scanner(conn.getInputStream());
        StringBuilder response = new StringBuilder();
        while (sc.hasNext()) response.append(sc.nextLine());
        sc.close();

        JsonNode root = mapper.readTree(response.toString());
        return root.path("choices").get(0).path("message").path("content").asText().trim();
    }
}
