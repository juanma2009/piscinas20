package e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ChecklistE2ETest {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeAll
    void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    void tearDown() {
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void setUpEach() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void tearDownEach() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("Checklist page loads without LazyInitializationException")
    void testChecklistPageLoadsSuccessfully() {
        // Navigate to the checklist page
        // Note: This assumes there's a test pedido with ID 1
        // In a real scenario, you would set up test data first
        page.navigate("http://localhost:8080/workshop/checklist/1");

        // Wait for the page to load completely
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Verify the page title contains "Checklist"
        String title = page.title();
        assertTrue(title.contains("Checklist") || title.contains("Taller"),
                "Page title should indicate checklist functionality");

        // Verify no error messages are present
        // Check for common error indicators
        boolean hasError = page.locator("text=/error|exception|500/i").count() > 0;
        assertFalse(hasError, "Page should not contain error messages");

        // Verify checklist tabs are present (indicates template data loaded)
        Locator tabs = page.locator(".nav-tabs .nav-link");
        assertTrue(tabs.count() > 0, "Checklist tabs should be present");

        // Verify checklist items are rendered
        Locator checklistItems = page.locator(".checklist-item");
        assertTrue(checklistItems.count() > 0, "Checklist items should be rendered");

        // Verify template names are displayed in tabs
        String firstTabText = tabs.first().textContent().trim();
        assertFalse(firstTabText.isEmpty(), "First tab should have template name");
    }

    @Test
    @DisplayName("Checklist template codes are accessible")
    void testTemplateCodesAreAccessible() {
        page.navigate("http://localhost:8080/workshop/checklist/1");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Verify that tab IDs contain template codes (no LazyInitializationException)
        Locator tabs = page.locator(".nav-tabs .nav-link");

        for (int i = 0; i < tabs.count(); i++) {
            String tabId = tabs.nth(i).getAttribute("id");
            assertNotNull(tabId, "Tab should have an ID");
            assertTrue(tabId.endsWith("-tab"), "Tab ID should end with '-tab'");

            // Verify aria attributes
            String ariaControls = tabs.nth(i).getAttribute("aria-controls");
            assertNotNull(ariaControls, "Tab should have aria-controls attribute");
            assertFalse(ariaControls.isEmpty(), "aria-controls should not be empty");

            String ariaSelected = tabs.nth(i).getAttribute("aria-selected");
            assertNotNull(ariaSelected, "Tab should have aria-selected attribute");
        }

        // Verify that tab content divs have IDs based on template codes
        Locator tabPanes = page.locator(".tab-pane");

        for (int i = 0; i < tabPanes.count(); i++) {
            String paneId = tabPanes.nth(i).getAttribute("id");
            assertNotNull(paneId, "Tab pane should have an ID");
            assertFalse(paneId.isEmpty(), "Tab pane ID should not be empty");

            // Verify aria-labelledby attribute
            String ariaLabelledBy = tabPanes.nth(i).getAttribute("aria-labelledby");
            assertNotNull(ariaLabelledBy, "Tab pane should have aria-labelledby attribute");
            assertTrue(ariaLabelledBy.endsWith("-tab"), "aria-labelledby should reference tab ID");
        }
    }

    @Test
    @DisplayName("No duplicate checklists - one per template type")
    void testNoDuplicateChecklists() {
        page.navigate("http://localhost:8080/workshop/checklist/1");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Get all tab names
        Locator tabs = page.locator(".nav-tabs .nav-link");
        java.util.List<String> tabNames = new java.util.ArrayList<>();
        java.util.List<String> tabIds = new java.util.ArrayList<>();

        for (int i = 0; i < tabs.count(); i++) {
            String tabText = tabs.nth(i).textContent().trim();
            String tabId = tabs.nth(i).getAttribute("id");
            tabNames.add(tabText);
            tabIds.add(tabId);
            System.out.println("Tab " + i + ": " + tabText + " (ID: " + tabId + ")");
        }

        // Verify we have the expected number of unique checklist types
        String[] expectedTypes = {"A. Recepción", "B. Talla y Medidas", "C. Metales del Cliente", "D. Piedras", "E. Diseño y Aprobación", "F. Producción", "G. Control de Calidad", "H. Entrega"};

        // Check exact count
        assertEquals(expectedTypes.length, tabNames.size(),
                String.format("Should have exactly 8 checklist tabs, but found %d: %s", 
                    tabNames.size(), String.join(", ", tabNames)));

        // Check for duplicates by comparing tab names
        java.util.Set<String> uniqueTabNames = new java.util.HashSet<>(tabNames);
        assertEquals(tabNames.size(), uniqueTabNames.size(),
                String.format("Found duplicate tabs. Tabs: %s, Unique: %s", 
                    String.join(", ", tabNames), String.join(", ", uniqueTabNames)));
        
        // Check for duplicate tab IDs
        java.util.Set<String> uniqueTabIds = new java.util.HashSet<>(tabIds);
        assertEquals(tabIds.size(), uniqueTabIds.size(),
                "Tab IDs should be unique");
        
        // Verify all expected types are present
        for (String expected : expectedTypes) {
            assertTrue(tabNames.contains(expected),
                    "Missing expected tab: " + expected);
        }
        
        // Verify no duplicate template codes in tab IDs
        java.util.Set<String> templateCodes = new java.util.HashSet<>();
        for (String tabId : tabIds) {
            String code = tabId.replace("-tab", "");
            assertFalse(templateCodes.contains(code), 
                "Duplicate template code found: " + code);
            templateCodes.add(code);
        }
    }
}