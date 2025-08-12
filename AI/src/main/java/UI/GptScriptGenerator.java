package UI;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class GptScriptGenerator {

	private static final String OPENAI_API_KEY = "OPENAI_API_KEY";
	private static final String EXCEL_FILE = "StoryInput.xlsx";
	private static final String OUTPUT_DIR = "generated";
	private static final String ROOT_DIR = "GeneratedAutomationProject";
	private static final String SRC_MAIN_JAVA = ROOT_DIR + "/src/main/java";
	private static final String PAGE_DIR = SRC_MAIN_JAVA + "/pages";
	private static final String UTILS_DIR = SRC_MAIN_JAVA + "/utils";
	private static final String SRC_TEST_JAVA = ROOT_DIR + "/src/test/java";
	private static final String TEST_DIR = SRC_TEST_JAVA + "/tests";

	public static void main(String[] args) throws Exception {
//        Files.createDirectories(Paths.get(OUTPUT_DIR + "/tests"));
//        Files.createDirectories(Paths.get(OUTPUT_DIR + "/pages"));
//        Files.createDirectories(Paths.get(OUTPUT_DIR + "/utils"));
		Files.createDirectories(Paths.get(TEST_DIR));
		Files.createDirectories(Paths.get(PAGE_DIR));
		Files.createDirectories(Paths.get(UTILS_DIR));
		Files.createDirectories(Paths.get(SRC_TEST_JAVA));

		generatePomXml();

		FileInputStream fis = new FileInputStream(EXCEL_FILE);
		Workbook workbook = new XSSFWorkbook(fis);
		Sheet sheet = workbook.getSheet("Input");

		for (int i = 1; i <= sheet.getLastRowNum(); i++) {
			Row row = sheet.getRow(i);
			if (row == null || row.getCell(4) == null)
				continue;

			String gherkin = row.getCell(4).getStringCellValue().trim();
			if (gherkin.isEmpty())
				continue;

			String javaCode = callOpenAIGPT(gherkin);

			// Save to 4 different files based on markers
			saveJavaClassesFromResponse(javaCode, i);
			row.createCell(5).setCellValue("Script Generated");
		}

		fis.close();
		FileOutputStream fos = new FileOutputStream(EXCEL_FILE);
		workbook.write(fos);
		fos.close();
		workbook.close();

		System.out.println("✅ All test scripts generated using GPT and saved locally.");
	}

	private static String callOpenAIGPT(String gherkin) throws Exception {
		String prompt = "You are a senior Java QA automation engineer.\n\n"
				+ "Generate Java UI automation scripts using the Page Object Model (POM) framework. "
				+ "Use Java, Selenium WebDriver, TestNG, and WebDriverManager as the tech stack.\n\n"
				+ "Based on the following Gherkin-style test case:\n\n" + gherkin + "\n\n"
				+ "Generate 4 complete and clean Java classes ready to run inside a Maven project with proper packaging and imports:\n\n"
				+

				"1. **ExamplePage.java**:\n" + "- Add `package pages;` at the top.\n" + "- Use `@FindBy` annotations.\n"
				+ "- Include required imports like `org.openqa.selenium.WebDriver`, `org.openqa.selenium.support.FindBy`, `import utils.Locators;` etc.\n"
				+ "- Initialize elements with `PageFactory.initElements(driver, this);`.\n"
				+ "- Ensure to find each element using xpath only fetching from Locators Util.\n\n" +

				"2. **ExampleTest.java**:\n" + "- Add `package tests;` at the top.\n"
				+ "- Import all necessary classes like `org.testng.annotations.BeforeClass`, `org.testng.annotations.Test`, etc.\n"
				+ "- Import `pages.ExamplePage` and `utils.Constants`.\n"
				+ "- Include `@BeforeClass` and `@AfterClass` methods to manage driver.\n"
				+ "- Use WebDriverManager to set up ChromeDriver.\n" + "- In the `@BeforeClass` method:\n"
				+ "  - Add `WebDriverManager.chromedriver().setup();` before initializing `ChromeDriver()`.\n"
				+ "  - Initialize ChromeDriver.\n" + "  - Navigate to a URL using `driver.get(Constants.BASE_URL);`\n"
				+ "  - Instantiate the ExamplePage class.\n"
				+ "- For each scenario step in the Gherkin, implement matching Java code inside the appropriate `@Test` method using ExamplePage methods. \r\n"
				//+ "- Name `@Test` methods based on the Gherkin scenario titles (e.g., `successfulLoginTest`)\n"
				+ "- Implement the Assertion in comment form so that User can edit it later. \n\n" +

				"3. **Locators.java**:\n" + "- Add `package utils;` at the top.\n"
				+ "- Use public static final strings for XPaths.\n"
				+ "- Leave values blank like `public static final String LOGIN_BUTTON = \"\";`.\n\n" +

				"4. **Constants.java**:\n" + "- Add `package utils;` at the top.\n"
				+ "- Use `public static final` fields for credentials and URLs.\n\n" +

				"IMPORTANT:\n" + "- DO NOT include triple backticks or Markdown formatting like ```java.\n"
				+ "- Only output raw Java code for each class.\n" + "- Output exactly in this format:\n"
				+ "### ExamplePage.java\n// class code here\n\n" + "### ExampleTest.java\n// class code here\n\n"
				+ "### Locators.java\n// class code here\n\n" + "### Constants.java\n// class code here";

		// Construct JSON request body using Jackson
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode requestJson = mapper.createObjectNode();
		requestJson.put("model", "gpt-4");

		ArrayNode messages = mapper.createArrayNode();
		ObjectNode userMessage = mapper.createObjectNode();
		userMessage.put("role", "user");
		userMessage.put("content", prompt);
		messages.add(userMessage);

		requestJson.set("messages", messages);

		String requestBody = mapper.writeValueAsString(requestJson);

		URL url = new URL("https://api.openai.com/v1/chat/completions");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);
		conn.setRequestProperty("Content-Type", "application/json");

		try (OutputStream os = conn.getOutputStream()) {
			os.write(requestBody.getBytes("utf-8"));
		}

		if (conn.getResponseCode() != 200) {
			Scanner err = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
			throw new RuntimeException("GPT Error: " + (err.hasNext() ? err.next() : "Unknown error"));
		}

		Scanner sc = new Scanner(conn.getInputStream()).useDelimiter("\\A");
		String json = sc.hasNext() ? sc.next() : "";
		sc.close();

		JsonNode jsonNode = mapper.readTree(json);
		return jsonNode.get("choices").get(0).get("message").get("content").asText();
	}

	private static void generatePomXml() throws Exception {
//		String pomPrompt = "Generate a valid and clean Maven pom.xml file for a Java Selenium TestNG automation project. " +
//		        "Do NOT include triple backticks or any markdown formatting. " +
//		        "Return only valid XML of a Maven pom.xml file. Do NOT include backticks, code fences, notes, or explanations. Start from <project> and end with </project>. No additional commentary. " +
//		        "Include dependencies for Selenium, TestNG, Apache POI, Jackson, WebDriverManager, and REST-assured. " +
//		        "Use Java 17. Set groupId as 'com.generated' and artifactId as 'AutoTestGen'.";
//		
		String pomPrompt = "Generate a valid and clean Maven pom.xml file for a Java Selenium TestNG automation project. "
				+ "Do NOT include triple backticks or any markdown formatting. "
				+ "Return only valid XML of a Maven pom.xml file. Do NOT include backticks, code fences, notes, or explanations. Start from <project> and end with </project>. No additional commentary. "
				+ "Include the following dependencies with versions:\n" + "- Selenium Java version 4.20.0\n"
				+ "- TestNG version 7.9.0\n" + "- WebDriverManager version 5.7.0\n" + "- Apache POI version 5.2.3\n"
				+ "- Jackson Databind version 2.17.1\n" + "- REST-assured version 5.4.0\n"
				+ "Use Java 17. Set groupId as 'com.generated' and artifactId as 'AutoTestGen'.";

		// JSON request
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode requestJson = mapper.createObjectNode();
		requestJson.put("model", "gpt-4");

		ArrayNode messages = mapper.createArrayNode();
		ObjectNode userMessage = mapper.createObjectNode();
		userMessage.put("role", "user");
		userMessage.put("content", pomPrompt);
		messages.add(userMessage);

		requestJson.set("messages", messages);
		String requestBody = mapper.writeValueAsString(requestJson);

		// Call GPT
		URL url = new URL("https://api.openai.com/v1/chat/completions");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);
		conn.setRequestProperty("Content-Type", "application/json");

		try (OutputStream os = conn.getOutputStream()) {
			os.write(requestBody.getBytes("utf-8"));
		}

		if (conn.getResponseCode() != 200) {
			Scanner err = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
			throw new RuntimeException("GPT Error (pom.xml): " + (err.hasNext() ? err.next() : "Unknown error"));
		}

		Scanner sc = new Scanner(conn.getInputStream()).useDelimiter("\\A");
		String json = sc.hasNext() ? sc.next() : "";
		sc.close();

		String gptOutput = mapper.readTree(json).get("choices").get(0).get("message").get("content").asText();

		String pomContent = gptOutput.replaceFirst("(?s)^.*?<project", "<project") // Keep from <project> onward
				.replaceFirst("</project>.*", "</project>") // Cut everything after </project>
				.replaceAll("[`]+", "") // Remove all backtick (``` or single `)
				.replaceAll("[^\\x20-\\x7E\\n\\r\\t]", "") // Remove any stray non-printable unicode
				.trim();

		Files.write(Paths.get(ROOT_DIR + "/pom.xml"), pomContent.getBytes());
	}

	private static void saveJavaClassesFromResponse(String gptOutput, int rowId) throws IOException {
		String[] sections = gptOutput.split("### ");
		for (String section : sections) {
			String content = "";
			int newlineIndex = section.indexOf("\n");

			if (newlineIndex != -1 && newlineIndex < section.length()) {
				content = section.substring(newlineIndex).trim();
				// Remove markdown code block if present
				content = content.replaceFirst("^```java", "").trim();
				content = content.replaceFirst("```$", "").trim();
				content = content.replaceAll("(?m)^```$", "");
			} else {
				continue; // Skip malformed section
			}

			if (section.contains("ExamplePage.java")) {
				writeToFile(PAGE_DIR + "/ExamplePage.java", content);
			} else if (section.contains("ExampleTest.java")) {
				writeToFile(TEST_DIR + "/ExampleTest.java", content);
			} else if (section.contains("Locators.java")) {
				writeToFile(UTILS_DIR + "/Locators.java", content);
			} else if (section.contains("Constants.java")) {
				writeToFile(UTILS_DIR + "/Constants.java", content);
			}
		}
	}

	private static void writeToFile(String filePath, String content) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
			bw.write(content);
		}
	}
}
