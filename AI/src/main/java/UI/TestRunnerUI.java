package UI;

import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


import java.awt.Desktop;
import java.io.*;

public class TestRunnerUI extends Application {

	private final String excelPath = "StoryInput.xlsx";
	private final String scriptPath = "C:\\Users\\MF-LLP\\eclipse-workspace\\AI\\src\\main\\java\\UI\\BddScriptGenerator.java";

	@Override
	public void start(Stage primaryStage) {
		// Title
		Label title = new Label("Telio Automation Script Generator");
		title.setFont(Font.font("Arial", 22));
		title.setTextFill(Color.web("#f8fafc"));
		title.setStyle("-fx-font-weight: bold;");
		
		// Logo image (upper left)
		ImageView logo = new ImageView(new Image("file:telioimg.jpg")); 
		logo.setFitHeight(40); 
		logo.setPreserveRatio(true);
		logo.setSmooth(true);

		// Underline
		Rectangle underline = new Rectangle(290, 4, Color.web("#60a5fa"));
		underline.setArcWidth(10);
		underline.setArcHeight(10);
		
		// Horizontal box with logo and title
		HBox titleContent = new HBox(10, logo, title);
		titleContent.setAlignment(Pos.CENTER_LEFT);
		
		VBox titleBox = new VBox(10, titleContent, title, underline);
		titleBox.setAlignment(Pos.CENTER);

		// Gradient background header inside StackPane
		StackPane header = new StackPane(titleBox);
		header.setStyle("-fx-background-color: linear-gradient(to right, #4929D4, #6C4CE5); " + "-fx-padding: 30 0;");
		header.setPrefHeight(100);

		// Wrapper to force full width
		HBox headerWrapper = new HBox(header);
		headerWrapper.setPrefWidth(Double.MAX_VALUE);
		HBox.setHgrow(header, Priority.ALWAYS);

		// Status label
//		// Label statusLabel = new Label("Ready");
//		statusLabel.setFont(Font.font("Arial", 14));
//		statusLabel.setTextFill(Color.web("#64748b"));
		
		TextArea statusArea = new TextArea("Ready\n");
		statusArea.setWrapText(true);
		statusArea.setEditable(false);
		statusArea.setPrefHeight(150); // adjust height as needed
		statusArea.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 13px; -fx-text-fill: #334155;");

		

		// Buttons
		Button excelBtn = createStyledButton("Input Jira IDs", "#3b82f6", "#2563eb");
		Button scriptBtn = createStyledButton("Generate BDD Scripts", "#6b7280", "#374151");
		Button fetchJiraBtn = createStyledButton("Generate Test Cases", "#10b981", "#059669");
		Button testngBtn = createStyledButton("Generate TestNG Scripts", "#eab308", "#ca8a04");
		
		
		

		excelBtn.setPrefWidth(220);
		scriptBtn.setPrefWidth(220);
		fetchJiraBtn.setPrefWidth(220);
		testngBtn.setPrefWidth(220);

		excelBtn.setOnAction(e -> openExcel(statusArea));
		scriptBtn.setOnAction(e -> runScript(statusArea));
		fetchJiraBtn.setOnAction(e -> fetchJira(statusArea));
		testngBtn.setOnAction(e -> runTestNGGenerator(statusArea));

		// VBox buttonBox = new VBox(20, excelBtn, scriptBtn);
//		VBox buttonBox = new VBox(20, excelBtn, fetchJiraBtn, scriptBtn, testngBtn);
//		buttonBox.setAlignment(Pos.CENTER);
		
		Label arrow1 = new Label(">>");
		Label arrow2 = new Label(">>");
		Label arrow3 = new Label(">>");

		arrow1.setStyle("-fx-font-size: 20px; -fx-text-fill: #64748b;");
		arrow2.setStyle("-fx-font-size: 20px; -fx-text-fill: #64748b;");
		arrow3.setStyle("-fx-font-size: 20px; -fx-text-fill: #64748b;");

		HBox buttonBox = new HBox(15, excelBtn, arrow1, fetchJiraBtn, arrow2, scriptBtn, arrow3, testngBtn);
		buttonBox.setAlignment(Pos.CENTER);

		VBox card = new VBox(40, headerWrapper, buttonBox, statusArea);
		card.setAlignment(Pos.TOP_CENTER);
		card.setStyle("-fx-background-color: #ffffff; -fx-padding: 40; -fx-background-radius: 16; "
				+ "-fx-border-color: rgba(0,0,0,0.05); -fx-border-radius: 16;");
		card.setPrefWidth(420);

		StackPane root = new StackPane(card);
		root.setStyle("-fx-background-color: #f1f5f9;");

		Scene scene = new Scene(root, 2080, 1080);
		primaryStage.setScene(scene);
		primaryStage.setTitle("File & Script Manager");
		primaryStage.setWidth(1200);
		primaryStage.setHeight(500);
		primaryStage.setResizable(false);
		primaryStage.show();
	}

	private Button createStyledButton(String text, String color, String hoverColor) {
		Button btn = new Button(text);
		btn.setStyle("-fx-background-color: " + color + ";" + "-fx-text-fill: white;" + "-fx-font-size: 14px;"
				+ "-fx-font-weight: bold;" + "-fx-padding: 12 24;" + "-fx-background-radius: 10;");

		btn.setOnMouseEntered(e -> animateHover(btn, hoverColor));
		btn.setOnMouseExited(e -> btn
				.setStyle("-fx-background-color: " + color + ";" + "-fx-text-fill: white;" + "-fx-font-size: 14px;"
						+ "-fx-font-weight: bold;" + "-fx-padding: 12 24;" + "-fx-background-radius: 10;"));

		btn.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> btn.setScaleX(0.97));
		btn.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> btn.setScaleX(1.0));

		return btn;
	}

	private void animateHover(Button btn, String hoverColor) {
		btn.setStyle("-fx-background-color: " + hoverColor + ";" + "-fx-text-fill: white;" + "-fx-font-size: 14px;"
				+ "-fx-font-weight: bold;" + "-fx-padding: 12 24;" + "-fx-background-radius: 10;");

		ScaleTransition scale = new ScaleTransition(Duration.millis(200), btn);
		scale.setToX(1.03);
		scale.setToY(1.03);
		scale.play();
	}

	private void openExcel(TextArea statusArea) {
		try {
			Desktop.getDesktop().open(new File(excelPath));
			statusArea.appendText("Opened Excel file.\n");
		} catch (IOException e) {
			statusArea.appendText("Failed to open Excel.\n");
			e.printStackTrace();
		}
	}

	private void runScript(TextArea statusArea) {
		try {
			statusArea.appendText("Generating scripts...\n");

			// Console log step 1
			System.out.println("[INFO] Starting Maven process...\n");

			ProcessBuilder runPB = new ProcessBuilder(
				    "C:\\apache-maven-3.9.10\\bin\\mvn.cmd",
				    "-q",                        // Quiet mode: suppress INFO logs
				    "-B",                        // Batch mode: cleaner output, no progress bars
				    "exec:java",
				    "-Dexec.mainClass=UI.BddScriptGenerator",
				    "-Dlog4j2.simplelogLevel=OFF" // Optional: suppress Log4j2 simple logger
				);


//            ProcessBuilder runPB = new ProcessBuilder(
//                    "java",
//                    "-cp",
//                    "AI-0.0.1-SNAPSHOT-jar-with-dependencies.jar",
//                    "UI.BddScriptGenerator"
//                );

			runPB.directory(new File(System.getProperty("user.dir")));
			runPB.redirectErrorStream(true); // combine stdout and stderr

			Process runProcess = runPB.start();

			System.out.println("[INFO] Maven process started.");

			new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()))) {

					String line;
					while ((line = reader.readLine()) != null) {
						System.out.println("[MAVEN] " + line + "\n"); // print to console
						String finalLine = line;
						Platform.runLater(() -> statusArea.appendText(finalLine+"\n"));
					}

					int exitCode = runProcess.waitFor();
					System.out.println("[INFO] Maven process finished with exit code " + exitCode + "\n");
					Platform.runLater(() -> statusArea.appendText("Script generation completed.\n"));
				} catch (IOException | InterruptedException e) {
					System.err.println("[ERROR] Error reading Maven output:" + "\n");
					e.printStackTrace();
					Platform.runLater(() -> statusArea.appendText("Error during script generation.\n"));
				}
			}).start();

		} catch (Exception e) {
			System.err.println("[ERROR] Failed to start Maven:");
			e.printStackTrace();
			Platform.runLater(() -> statusArea.appendText("Script failed to start.\n"));
		}
	}

	private void fetchJira(TextArea statusArea) {
		try {
			statusArea.appendText("Fetching stories from Jira...\n");

			System.out.println("[INFO] Starting Jira fetch process...\n");
			
			ProcessBuilder fetchPB = new ProcessBuilder(
				    "C:\\apache-maven-3.9.10\\bin\\mvn.cmd",
				    "-q",                        // Quiet mode: suppress INFO logs
				    "-B",                        // Batch mode: cleaner output, no progress bars
				    "exec:java",
				    "-Dexec.mainClass=UI.JiraStoryFetcher",
				    "-Dlog4j2.simplelogLevel=OFF" // Optional: suppress Log4j2 simple logger
				);
			
//			ProcessBuilder fetchPB = new ProcessBuilder(
//				    "mvn",
//				    "-q",                        // Suppresses most logs
//				    "-B",                        // Non-interactive (batch mode)
//				    "exec:java",
//				    "-Dexec.mainClass=UI.JiraStoryFetcher"
//				);
			
			fetchPB.directory(new File(System.getProperty("user.dir")));
			fetchPB.redirectErrorStream(true);

			Process fetchProcess = fetchPB.start();

			System.out.println("[INFO] Jira fetch process started.\n");

			new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(fetchProcess.getInputStream()))) {

					String line;
					while ((line = reader.readLine()) != null) {
						System.out.println("[MAVEN] " + line + "\n");
						String finalLine = line;
						Platform.runLater(() -> statusArea.appendText(finalLine+"\n"));
					}

					int exitCode = fetchProcess.waitFor();
					System.out.println("[INFO] Jira fetch finished with exit code " + exitCode + "\n");
					Platform.runLater(() -> statusArea.appendText("Jira fetch completed.\n"));
				} catch (IOException | InterruptedException e) {
					System.err.println("[ERROR] Error during Jira fetch:\n");
					e.printStackTrace();
					Platform.runLater(() -> statusArea.appendText("Error fetching from Jira.\n"));
				}
			}).start();

		} catch (Exception e) {
			System.err.println("[ERROR] Failed to start Jira fetch:\n");
			e.printStackTrace();
			Platform.runLater(() -> statusArea.appendText("Failed to start Jira fetch.\n"));
		}
	}
	
	private void runTestNGGenerator(TextArea statusArea) {
		try {
			statusArea.appendText("Generating TestNG scripts...\n");

			ProcessBuilder testngPB = new ProcessBuilder(
					"C:\\apache-maven-3.9.10\\bin\\mvn.cmd",
				    "-q",                        // Quiet mode: suppress INFO logs
				    "-B",                        // Batch mode: cleaner output, no progress bars
				    "exec:java",
				    "-Dexec.mainClass=UI.GptScriptGenerator",
				    "-Dlog4j2.simplelogLevel=OFF"
			);

			testngPB.directory(new File(System.getProperty("user.dir")));
			testngPB.redirectErrorStream(true);

			Process process = testngPB.start();
			System.out.println("[INFO] TestNG generator process started.\n");

			new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						System.out.println("[MAVEN] " + line);
						String finalLine = line;
						Platform.runLater(() -> statusArea.appendText(finalLine + "\n"));
					}

					int exitCode = process.waitFor();
					System.out.println("[INFO] TestNG generator finished with exit code " + exitCode + "\n");
					Platform.runLater(() -> statusArea.appendText("TestNG script generation completed.\n"));
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
					Platform.runLater(() -> statusArea.appendText("Error during TestNG script generation.\n"));
				}
			}).start();

		} catch (Exception e) {
			e.printStackTrace();
			Platform.runLater(() -> statusArea.appendText("Failed to start TestNG script generator.\n"));
		}
	}


	public static void main(String[] args) {
		try {
			File logFile = new File("output_log.txt");
			PrintStream fileOut = new PrintStream(logFile);
			System.setOut(fileOut);
			System.setErr(fileOut);
		} catch (IOException e) {
			e.printStackTrace();
		}

		launch(args);
	}

}