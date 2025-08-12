package UI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class GherkinGeneratorFromManualSteps {

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

            Cell gherkinCell = row.getCell(4); // Column E (0-based index)
            if (gherkinCell != null && !gherkinCell.getStringCellValue().trim().isEmpty()) {
                continue; // Skip if Gherkin already exists
            }

            Cell testStepCell = row.getCell(7); // Column H: Test Case Description
            Cell expectedCell = row.getCell(8); // Column I: Expected Outcome

            if (testStepCell == null || expectedCell == null) continue;

            String testSteps = testStepCell.getStringCellValue().trim();
            String expected = expectedCell.getStringCellValue().trim();

            if (testSteps.isEmpty() || expected.isEmpty()) continue;

            try {
                String gherkin = generateGherkinFromManual(testSteps, expected);
                row.createCell(4).setCellValue(gherkin); // Column E
            } catch (Exception e) {
                row.createCell(4).setCellValue("❌ Failed: " + e.getMessage());
                System.err.println("❌ Failed to generate Gherkin at row " + (i + 1) + ": " + e.getMessage());
            }
        }

        fis.close();
        FileOutputStream fos = new FileOutputStream("StoryInput.xlsx");
        workbook.write(fos);
        fos.close();
        workbook.close();

        System.out.println("✔️ Gherkin generation completed.");
    }

    private static String generateGherkinFromManual(String testSteps, String expectedOutcome) throws Exception {

    	String prompt = 
    		    "You are a QA engineer. Convert the following manual test case into a single Gherkin-style BDD test case.\n\n" +
    		    "Strictly follow these rules:\n" +
    		    "- Output only standard Gherkin syntax (Feature, Scenario, Given, When, Then, And).\n" +
    		    "- Do NOT use Markdown formatting like asterisks (**).\n" +
    		    "- Do NOT use hyphens (-) before steps.\n" +
    		    "- Do NOT add any notes or explanations outside of the Gherkin format.\n" +
    		    "- Do NOT include multiple scenarios or features.\n" +
    		    "- Do NOT include negative or edge cases.\n\n" +
    		    "Test Steps: " + testSteps + "\n" +
    		    "Expected Outcome: " + expectedOutcome;
        
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