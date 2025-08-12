package UI;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import java.util.Set;

public class BddScriptGenerator {

	private static final String OPENAI_API_KEY = "OPENAI_API_KEY";
	private static final String EXCEL_FILE = "StoryInput.xlsx";
	private static final String ROOT_DIR = "GeneratedBDDProject";
	private static final String MAIN_JAVA = ROOT_DIR + "/src/main/java";
	private static final String TEST_JAVA = ROOT_DIR + "/src/test/java";
	private static final String TEST_RESOURCES = ROOT_DIR + "/src/test/resources";
	private static final String PAGE_DIR = MAIN_JAVA + "/pages";
	private static final String UTILS_DIR = MAIN_JAVA + "/utils";
	private static final String STEPS_DIR = MAIN_JAVA + "/stepdefinitions";
	private static final String RUNNER_DIR = TEST_JAVA + "/runners";
	private static final String FEATURES_DIR = TEST_RESOURCES + "/features";

	public static void main(String[] args) throws Exception {
		System.out.println("Starting BDD Script Generation.\n");
		Files.createDirectories(Paths.get(PAGE_DIR));
		Files.createDirectories(Paths.get(UTILS_DIR));
		Files.createDirectories(Paths.get(STEPS_DIR));
		Files.createDirectories(Paths.get(RUNNER_DIR));
		Files.createDirectories(Paths.get(FEATURES_DIR));

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

			saveJavaClassesFromResponse(javaCode);
//			if (!CodeValidator.validateStepDefinition()) {
//				System.out.println("Regenerating StepDefinitions due to validation failure...");
//				String stepOnlyPrompt = buildStepOnlyPrompt(gherkin);
//				String regeneratedStep = callGPTWithPrompt(stepOnlyPrompt);
//				saveStepOnly(regeneratedStep);
//			}

			row.createCell(6).setCellValue("BDD Script Generated");
		}

		fis.close();
		FileOutputStream fos = new FileOutputStream(EXCEL_FILE);
		workbook.write(fos);
		fos.close();
		workbook.close();

		System.out.println("BDD test scripts generated using GPT and saved.\n");
	}

	private static String callOpenAIGPT(String gherkin) throws Exception {
		String prompt = "You are a senior Java QA automation engineer.\n\n"
				+ "Generate Java UI automation scripts using the BDD Cucumber framework. "
				+ "Use Java, Selenium WebDriver, WebDriverManager, Cucumber, and JUnit as the tech stack.\n\n"
				+ "Based on the following Gherkin-style test case:\n\n" + gherkin + "\n\n"

				+ "Generate the following 7 files as complete, compilable, and clean Java classes ready to run inside a Maven project with proper packaging and imports:\n"
				+ "- All classes compile without errors or missing imports.\n"
				+ "- All page objects are initialized properly and consistently use the WebDriver instance.\n"
				+ "- WebDriver is handled through a central DriverFactory using the Singleton pattern, and reused correctly across tests.\n"
				+ "- No class (like a Page object) should expose the WebDriver directly (no getDriver() method in page classes).\n"
				+ "- All step definitions must initialize required objects inside the @Before hook to avoid NullPointerExceptions.\n"
				+ "- Every step method should assume required objects (like ExamplePage) are ready to use.\n"
				+ "- No steps, locators, or methods should be referenced unless they are defined.\n"
				+ "- Page classes must define all methods called in the step definition.\n\n"

//				+ "1. ExamplePage.java:\n" + "- Package: pages\n" + "- Add: import utils.Locators;\n"
//				+ "- Use @FindBy annotations for elements.\n"
//				+ "- Assign the passed driver to a private field (this.driver = driver) and initialize elements using PageFactory.initElements(driver, this);"
//				+ "- Create methods for actions like enterUsername(), clickLoginButton(), etc.\n\n"

				+ "1. ExamplePage.java:\n" + "- Package: pages\n" + "- Add: import utils.Locators;\n"
				+ "- Use @FindBy annotations to locate elements using selectors from Locators.java.\n"
				+ "- Assign the passed driver to a private field (this.driver = driver).\n"
				+ "- Initialize web elements using PageFactory.initElements(driver, this).\n"
				+ "- Create public methods for UI interactions such as:\n" + "  - enterUsername(String username)\n"
				+ "  - enterPassword(String password)\n" + "  - clickLoginButton()\n"
				+ "- For methods that return a value (e.g., boolean or String), **ensure they include a proper return statement** (e.g., return true, return element.isDisplayed()).\n"
				+ "- Do not include empty methods or unimplemented stubs—every method must compile and be fully implemented.\n"
				+ "- The entire class must compile with no syntax or semantic errors (e.g., no missing return values, unresolved variables, or missing imports).\n"

				+ "2. DriverFactory.java:\n" + "- Package: utils\n" + "- Use WebDriverManager to setup ChromeDriver.\n"
				+ "- Implement singleton pattern with getDriver() and quitDriver() methods.\n"
				+ "- WebDriver should be reusable across steps.\n\n"

				+ "3. Locators.java:\n" + "- Package: utils\n"
				+ "- Define public static final String constants for XPaths or CSS selectors.\n\n"

				+ "4. Constants.java:\n" + "- Package: utils\n"
				+ "- Define public static final fields like BASE_URL, USERNAME, PASSWORD, etc.\n\n"

//				+ "5. ExampleSteps.java:\n" + "- Package: stepdefinitions\n"
//				+ "- Import: utils.Constants, utils.DriverFactory, utils.Locators, org.junit.Assert, io.cucumber.java annotations, pages.ExamplePage, org.openqa.selenium, import org.junit.Before, import org.junit.After etc.*\n"
//				+ "- Annotate methods with @Given, @When, @Then, @And using the **exact Gherkin step texts**.\n"
//				+ "- Implement **ALL steps** from **ALL scenarios**, even if text differs slightly.\n"
//				+ "- Include @Before and @After hooks to setup and close WebDriver.\n"
//				+ "- Use assertions where applicable.\n\n" + "- Ensure all required imports are present."

				+ "5. ExampleSteps.java:\n" + "- Package: stepdefinitions\n"
				+ "- Must include **ALL necessary imports** such as:\n" + "  - import utils.Constants;\n"
				+ "  - import utils.DriverFactory;\n" + "  - import utils.Locators;\n"
				+ "  - import org.openqa.selenium.*;\n" + "  - import org.junit.Assert;\n"
				+ "  - import org.junit.Before;\n" + "  - import org.junit.After;\n"
				+ "  - import io.cucumber.java.en.*;\n" + "  - import pages.ExamplePage;\n"
				+ "  - import io.cucumber.java.en.Given;\n" + "- import io.cucumber.java.en.When;\n"
				+ "  - import io.cucumber.java.en.And;\n" + "  - import io.cucumber.java.en.Then;\n\n"
				+ "- Define a private WebDriver field and a private ExamplePage field.\n"
				+ "- In a method annotated with @Before, use DriverFactory.getDriver() to initialize the WebDriver and the ExamplePage.\n"

				+ "- In a method annotated with @Before:\n"
				+ "  - Use DriverFactory.getDriver() to initialize the WebDriver and the ExamplePage.\n"
				+ "  - Open the application using driver.get(Constants.BASE_URL);\n\n"

				+ "- In a method annotated with @After, call DriverFactory.quitDriver().\n\n" +

				"- Annotate step methods with @Given, @When, @Then, @And, matching the Gherkin step texts **exactly** (case-sensitive).\n"
				+ "- For every method called on ExamplePage (e.g., clickLogin(), enterUsername()), ensure those methods are already defined in ExamplePage.java.\n"
				+ "- All method calls inside the steps should reference the `examplePage` object (e.g., examplePage.clickLogin()).\n"
				+

				"- Do NOT assume any object is globally available unless initialized in @Before.\n"
				+ "- Avoid declaring unused imports or referencing missing methods.\n"
				+ "- Keep method signatures clean and aligned with the Gherkin lines.\n\n" +

				"- The entire ExampleSteps.java class must compile **without any missing imports**, **missing methods**, or **missing hooks**."

				+ "6. TestRunner.java:\n" + "- Package: runners\n" + "- Annotate with @RunWith(Cucumber.class)\n"
				+ "- Use @CucumberOptions with:\n" + "  features = \"src/test/resources/features\",\n"
				+ "  glue = \"stepdefinitions\",\n"
				+ "  plugin = {\"pretty\", \"html:target/cucumber-reports.html\"}\n\n"

				+ "7. ExampleFeature.feature:\n" + "- Location: src/test/resources/features/ExampleFeature.feature\n"
				+ "- Begin with `Feature:` followed by the title.\n"
				+ "- Include all scenarios and steps **exactly as provided**.\n\n"

				+ "IMPORTANT:\n" + "- Output format MUST be:\n" + "### ExamplePage.java\n// Java class code\n\n"
				+ "### DriverFactory.java\n// Java class code\n\n" + "### Locators.java\n// Java class code\n\n"
				+ "### Constants.java\n// Java class code\n\n" + "### ExampleSteps.java\n// Java class code\n\n"
				+ "### TestRunner.java\n// Java class code\n\n" + "### ExampleFeature.feature\n// Gherkin text\n\n"
				+ "- DO NOT include triple backticks (```), ```java, or any markdown formatting.\n"
				+ "- DO NOT include comments like 'Here is the code'. Just output raw code.\n"
				+ "- Ensure ALL classes are syntactically correct and compile successfully.\n"
				+ "- Ensure that all necessary imports are in place, all classes are in sync, and the generated code is completely error-free and ready to compile and run.\n"
				+ "- DO NOT skip any step or scenario and DO NOT leave anything pending. Generated scripts should be ready to import and run on eclipse.";

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

		return mapper.readTree(json).get("choices").get(0).get("message").get("content").asText();
	}

	private static void saveJavaClassesFromResponse(String gptOutput) throws IOException {
		String[] sections = gptOutput.split("### ");
		for (String section : sections) {
			String content;
			int newlineIndex = section.indexOf("\n");
			if (newlineIndex != -1 && newlineIndex < section.length()) {
				content = section.substring(newlineIndex).trim();
				// Remove markdown code block if present
				content = content.replaceFirst("^```java", "").trim();
				content = content.replaceFirst("```$", "").trim();
				content = content.replaceAll("(?m)^```$", "");
			} else {
				continue;
			}

			if (section.contains("ExamplePage.java")) {
				writeToFile(PAGE_DIR + "/ExamplePage.java", content);
			} else if (section.contains("DriverFactory.java")) {
				writeToFile(UTILS_DIR + "/DriverFactory.java", content);
			} else if (section.contains("Locators.java")) {
				writeToFile(UTILS_DIR + "/Locators.java", content);
			} else if (section.contains("Constants.java")) {
				writeToFile(UTILS_DIR + "/Constants.java", content);
			} else if (section.contains("ExampleSteps.java")) {
				writeToFile(STEPS_DIR + "/ExampleSteps.java", content);
			} else if (section.contains("TestRunner.java")) {
				writeToFile(RUNNER_DIR + "/TestRunner.java", content);
			} else if (section.contains("ExampleFeature.feature")) {
				writeToFile(FEATURES_DIR + "/ExampleFeature.feature", content);
			}
		}
	}

	private static void generatePomXml() throws Exception {
		String prompt = "Generate a Maven pom.xml for a Java Cucumber BDD framework using:\n"
				+ "Do NOT include triple backticks or any Markdown formatting like ```java."
				+ "Return only valid XML of a Maven pom.xml file. Do NOT include backticks, code fences, notes, or explanations. Start from <project> and end with </project>. No additional commentary like ```."
				+ "- Java 17\n" + "- Selenium Java 4.20.0\n" + "- JUnit 4.13.2\n" + "- WebDriverManager 5.7.0\n"
				+ "- Apache POI 5.2.3\n" + "- Jackson Databind 2.17.1\n" + "- Cucumber-java 7.14.0\n"
				+ "- Cucumber-junit 7.14.0\n" + "- REST-assured 5.4.0\n" + "GroupId: com.bdd.generated\n"
				+ "ArtifactId: BddAutoFramework\n"
				+ "Ensure compatibility with Cucumber 7+ package structure (io.cucumber.*)\n"
				+ "Do NOT use cucumber.api.*\n" + "Do NOT mark scope in any Dependency.";

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
			throw new RuntimeException("GPT Error (pom.xml): " + (err.hasNext() ? err.next() : "Unknown error"));
		}

		Scanner sc = new Scanner(conn.getInputStream()).useDelimiter("\\A");
		String json = sc.hasNext() ? sc.next() : "";
		sc.close();

		String gptOutput = mapper.readTree(json).get("choices").get(0).get("message").get("content").asText();
		String pomContent = gptOutput.replaceFirst("(?s)^.*?<project", "<project")
				.replaceFirst("</project>.*", "</project>").replaceAll("[`]+", "") // Remove all backtick (``` or single
																					// `)
				.replaceAll("[^\\x20-\\x7E\\n\\r\\t]", "").trim();

		Files.write(Paths.get(ROOT_DIR + "/pom.xml"), pomContent.getBytes());
		System.out.print("pom.xml Generated.\n");
	}

	private static String buildStepOnlyPrompt(String gherkin) {
		Set<String> missingMethods = CodeValidator.getMissingMethods();
		Set<String> missingImports = CodeValidator.getMissingImports();
		boolean missingBefore = CodeValidator.isMissingBeforeHook();
		boolean missingAfter = CodeValidator.isMissingAfterHook();

		try {
			String stepCode = new String(Files
					.readAllBytes(Paths.get("GeneratedBDDProject/src/main/java/stepdefinitions/ExampleSteps.java")));

			StringBuilder prompt = new StringBuilder();

			prompt.append("You are a Java QA Automation expert.\n\n").append(
					"Your task is to **rectify the following Step Definition class** to resolve structural and compilation issues.\n")
					.append("Make only the necessary changes. Do **not rewrite** or reformat existing valid code.\n\n")
					.append("Here is the existing StepDefinition:\n\n").append("### ExampleSteps.java\n")
					.append(stepCode).append("\n\n");

			prompt.append("Now, fix the following problems ONLY:\n");

			if (!missingMethods.isEmpty()) {
				prompt.append("\n Missing method references (assume these exist now in PageObject):\n");
				for (String m : missingMethods)
					prompt.append("- ").append(m).append("\n");
			}

			if (!missingImports.isEmpty()) {
				prompt.append("\n Missing imports (add these):\n");
				for (String i : missingImports)
					prompt.append("import ").append(i).append(";\n");
			}

			if (missingBefore)
				prompt.append("\n Add a @Before method to initialize WebDriver using DriverFactory.\n");
			if (missingAfter)
				prompt.append("⚠ Add an @After method to quit WebDriver.\n");

			prompt.append("\n Final Instructions:\n").append("- Keep everything else exactly the same.\n")
					.append("- Return only the corrected raw Java code for ExampleSteps.java.\n")
					.append("- Do NOT include any markdown, backticks, or extra files.\n");

			return prompt.toString();
		} catch (IOException e) {
			System.err.println("Failed to read StepDefinition file: " + e.getMessage());
			e.printStackTrace();
			return "";
		}
	}

	private static String callGPTWithPrompt(String prompt) throws Exception {
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

		return mapper.readTree(json).get("choices").get(0).get("message").get("content").asText().trim();
	}

	private static void saveStepOnly(String content) throws IOException {
		// Remove any markdown or formatting if present
		content = content.replaceFirst("^```java", "").trim();
		content = content.replaceFirst("```$", "").trim();
		content = content.replaceAll("(?m)^```$", "");

		writeToFile(STEPS_DIR + "/ExampleSteps.java", content);
	}

	private static void writeToFile(String filePath, String content) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writer.write(content);
		}
	}
}
