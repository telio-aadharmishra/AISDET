package UI;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.ImportDeclaration;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class CodeValidator {

    private static Set<String> missingMethods = new HashSet<>();
    private static Set<String> missingImports = new HashSet<>();
    private static boolean missingBeforeHook = false;
    private static boolean missingAfterHook = false;

    public static Set<String> getMissingMethods() { return missingMethods; }
    public static Set<String> getMissingImports() { return missingImports; }
    public static boolean isMissingBeforeHook() { return missingBeforeHook; }
    public static boolean isMissingAfterHook() { return missingAfterHook; }

    private static final String STEP_FILE = "GeneratedBDDProject/src/main/java/stepdefinitions/ExampleSteps.java";
    private static final String PAGE_FILE = "GeneratedBDDProject/src/main/java/pages/ExamplePage.java";

    public static boolean validateStepDefinition() {
        try {
            JavaParser parser = new JavaParser();

            ParseResult<CompilationUnit> stepResult = parser.parse(new File(STEP_FILE));
            ParseResult<CompilationUnit> pageResult = parser.parse(new File(PAGE_FILE));

            if (!stepResult.isSuccessful() || !pageResult.isSuccessful()) {
                System.err.println("❌ Parsing failed. Check if the files are syntactically correct.");
                return false;
            }

            CompilationUnit stepCU = stepResult.getResult().orElseThrow(() -> new RuntimeException("Step CU missing"));
            CompilationUnit pageCU = pageResult.getResult().orElseThrow(() -> new RuntimeException("Page CU missing"));

            // Reset previous errors
            missingMethods.clear();
            missingImports.clear();
            missingBeforeHook = false;
            missingAfterHook = false;

            boolean valid = true;

            // 1. Validate method usage
            Set<String> pageMethods = pageCU.findAll(MethodDeclaration.class)
                    .stream()
                    .map(MethodDeclaration::getNameAsString)
                    .collect(Collectors.toSet());

            List<MethodCallExpr> methodCalls = stepCU.findAll(MethodCallExpr.class);
            for (MethodCallExpr call : methodCalls) {
                String method = call.getNameAsString();
                if (!pageMethods.contains(method)) {
                    System.err.println("❌ Missing method in Page Object: " + method);
                    missingMethods.add(method);
                    valid = false;
                }
            }

            // 2. Validate imports
            List<String> requiredImports = Arrays.asList(
                    // Selenium
                    "org.openqa.selenium.WebDriver",
                    "org.openqa.selenium.WebElement",
                    "org.openqa.selenium.By",
                    "org.openqa.selenium.support.ui.WebDriverWait",
                    "org.openqa.selenium.support.ui.ExpectedConditions",

                    // JUnit
                    "org.junit.Assert",
                    "org.junit.Before",
                    "org.junit.After",

                    // Cucumber (Step Annotations)
                    "io.cucumber.java.en.Given",
                    "io.cucumber.java.en.When",
                    "io.cucumber.java.en.Then",
                    "io.cucumber.java.en.And",
                    "io.cucumber.java.en.But",

                    // Cucumber (Hooks)
                    "io.cucumber.java.Before",
                    "io.cucumber.java.After",

                    // PageFactory (Optional)
                    "org.openqa.selenium.support.FindBy",
                    "org.openqa.selenium.support.PageFactory"
            );

            List<String> actualImports = stepCU.findAll(ImportDeclaration.class)
                    .stream()
                    .map(i -> i.getNameAsString())
                    .collect(Collectors.toList());

            for (String req : requiredImports) {
                if (!actualImports.contains(req)) {
                    System.err.println("❌ Missing import: " + req);
                    missingImports.add(req);
                    valid = false;
                }
            }

            // 3. Validate presence of @Before and @After hooks
            boolean hasBeforeHook = stepCU.findAll(MethodDeclaration.class).stream()
                    .anyMatch(m -> m.getAnnotations().stream()
                            .anyMatch(a -> a.getNameAsString().equals("Before")));

            boolean hasAfterHook = stepCU.findAll(MethodDeclaration.class).stream()
                    .anyMatch(m -> m.getAnnotations().stream()
                            .anyMatch(a -> a.getNameAsString().equals("After")));

            if (!hasBeforeHook) {
                System.err.println("❌ Missing @Before hook in StepDefinition.");
                missingBeforeHook = true;
                valid = false;
            }

            if (!hasAfterHook) {
                System.err.println("❌ Missing @After hook in StepDefinition.");
                missingAfterHook = true;
                valid = false;
            }

            return valid;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
