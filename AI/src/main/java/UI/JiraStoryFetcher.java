package UI;
import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.Scanner;
import java.util.Map;
import java.util.List;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class JiraStoryFetcher {

    private static final String JIRA_URL = "https://penielsolutions.atlassian.net";
    private static final String EMAIL = "aadhar.mishra@teliolabs.com";
    private static final String API_TOKEN = "JIRA_API_KEY"; 
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
            if (row == null || row.getCell(0) == null) continue;

            String storyId = row.getCell(0).getStringCellValue().trim();
            if (storyId.isEmpty()) continue;

            Cell statusCell = row.getCell(1);
            if (statusCell != null && "Executed".equalsIgnoreCase(statusCell.getStringCellValue().trim())) {
                continue; // Skip already executed
            }

            try {
                JiraStory story = fetchStoryFromJira(storyId);

                row.createCell(1).setCellValue("Executed"); // Status
                row.createCell(2).setCellValue(story.summary); // Summary
                row.createCell(3).setCellValue(story.description); // Description
                row.createCell(4).setCellValue(generateGherkin(story.summary, story.description));

            } catch (Exception e) {
                row.createCell(1).setCellValue("Failed");
                System.err.println("❌ Failed to process " + storyId + ": " + e.getMessage());
            }
        }

        fis.close();

        FileOutputStream fos = new FileOutputStream("StoryInput.xlsx");
        workbook.write(fos);
        fos.close();
        workbook.close();

        System.out.println("Stories processed and saved back to input sheet.");
        
    }

    private static JiraStory fetchStoryFromJira(String storyId) throws Exception {
        String auth = EMAIL + ":" + API_TOKEN;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        URL url = URI.create(JIRA_URL + "/rest/api/3/issue/" + storyId).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("HTTP " + conn.getResponseCode());
        }

        Scanner sc = new Scanner(conn.getInputStream());
        StringBuilder response = new StringBuilder();
        while (sc.hasNext()) response.append(sc.nextLine());
        sc.close();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.toString());

        String summary = root.path("fields").path("summary").asText();
        StringBuilder descBuilder = new StringBuilder();

        JsonNode descriptionNode = root.path("fields").path("description").path("content");
        if (descriptionNode.isArray()) {
            for (JsonNode contentNode : descriptionNode) {
                JsonNode paragraph = contentNode.path("content");
                for (JsonNode textNode : paragraph) {
                    descBuilder.append(textNode.path("text").asText()).append(" ");
                }
            }
        }

        return new JiraStory(summary, descBuilder.toString().trim());
    }

    private static String generateGherkin(String summary, String description) throws Exception {
        String prompt = "You are a QA engineer. Write comprehensive end-to-end positive test cases "
                + "in BDD Gherkin format based on the following Jira story:\n\n"
                + "Summary: " + summary + "\n\n"
                + "Description: " + description + "\n\n"
                + "Only include scenarios that represent successful user flows. "
                + "Do not include edge cases or negative paths.";

        ObjectMapper mapper = new ObjectMapper();

        // Build JSON structure using Jackson
        String requestBody = mapper.writeValueAsString(
                Map.of(
                        "model", "gpt-4",
                        "messages", List.of(
                                Map.of(
                                        "role", "user",
                                        "content", prompt
                                )
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



    static class JiraStory {
        String summary;
        String description;

        JiraStory(String summary, String description) {
            this.summary = summary;
            this.description = description;
        }
    }
}
