package com.example;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;

public class StatusCodeInterceptionTest {
    Playwright playwright;
    Browser browser;
    Page page;

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();
        
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();
        options.setHeadless(true);
        options.setArgs(java.util.Arrays.asList(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu"
        ));
        
        browser = playwright.chromium().launch(options);
        BrowserContext context = browser.newContext();
        page = context.newPage();
        
        page.setDefaultTimeout(30000);
        page.setDefaultNavigationTimeout(60000);

        context.route("**/status_codes/404", route -> {
            route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setBody("Mocked Success Response")
            );
        });
    }

    @Test
    void testMockedStatusCode() {
        page.navigate("https://the-internet.herokuapp.com/status_codes");
        
        page.waitForSelector("a[href='status_codes/404']");
        
        page.click("a[href='status_codes/404']");
        
        page.waitForLoadState();

        String content = page.content();
        assertTrue(content.contains("Mocked Success Response"), 
            "Должен содержать 'Mocked Success Response'. Получено: " + content);
    }

    @AfterEach
    void tearDown() {
        if (page != null) page.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}