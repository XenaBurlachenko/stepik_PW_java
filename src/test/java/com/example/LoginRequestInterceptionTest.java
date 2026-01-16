package com.example;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class LoginRequestInterceptionTest {
    Playwright playwright;
    Browser browser;
    Page page;

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
            .setHeadless(true)
            .setArgs(java.util.Arrays.asList(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu"
            )));
        
        BrowserContext context = browser.newContext();
        page = context.newPage();
        page.setDefaultTimeout(30000);
        page.setDefaultNavigationTimeout(30000);
    }

    @Test
    void loginRequestInterceptionTest() {
        final boolean[] requestIntercepted = {false};

        page.route("**/authenticate", route -> {
            requestIntercepted[0] = true;
            System.out.println("Запрос перехвачен!");
            
            String originalData = route.request().postData();
            System.out.println("Было: " + originalData);
            
            String modifiedData = originalData.replace("username=tomsmith", "username=HACKED_USER");
            System.out.println("Стало: " + modifiedData);
            
            route.resume(new Route.ResumeOptions()
                .setPostData(modifiedData)
            );
        });

        try {
            page.navigate("https://the-internet.herokuapp.com/login");
            page.waitForSelector("#username");
            
            page.fill("#username", "tomsmith");
            page.fill("#password", "SuperSecretPassword!");
            page.click("button[type='submit']");

            page.waitForResponse("**/authenticate", () -> {});

            assertTrue(requestIntercepted[0], "Запрос должен быть перехвачен");

            page.waitForSelector("#flash", new Page.WaitForSelectorOptions().setTimeout(5000));
            String flashText = page.locator("#flash").textContent();
            assertTrue(flashText.contains("invalid") || flashText.contains("error") || flashText.contains("username"),
                "Ожидалось сообщение об ошибке, получено: " + flashText);
            
            System.out.println("✓ Test 2 passed: Request intercepted and modified");
        } catch (Exception e) {
            System.out.println("Test 2 error: " + e.getMessage());
            throw e;
        }
    }

    @AfterEach
    void tearDown() {
        try {
            if (page != null) page.close();
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
        } catch (Exception e) {
        }
    }
}