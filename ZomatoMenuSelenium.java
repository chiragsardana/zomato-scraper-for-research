package com.devnologix;  // Package name for organizing your project files

// Jsoup imports (not actually used here, but useful if parsing static HTML was needed)
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// Selenium imports
import org.openqa.selenium.*;  // Core WebDriver classes (WebDriver, WebElement, etc.)
import org.openqa.selenium.chrome.ChromeDriver;  // To control Google Chrome
import org.openqa.selenium.chrome.ChromeOptions; // Optional: To set browser options
import org.openqa.selenium.support.ui.ExpectedConditions; // Used for explicit waits
import org.openqa.selenium.support.ui.WebDriverWait;      // For waiting until elements load

// WebDriverManager import to automatically handle ChromeDriver binary
import io.github.bonigarcia.wdm.WebDriverManager;

// Java utility & IO imports
import java.io.FileWriter;   // To write extracted data into CSV file
import java.io.IOException;  // Handles file writing exceptions
import java.time.Duration;   // For wait durations
import java.util.List;       // To store multiple dish elements

public class ZomatoMenuSelenium {

    public static void main(String[] args) throws InterruptedException, IOException {

        // 1️⃣ Setup ChromeDriver automatically (no need to download manually)
        WebDriverManager.chromedriver().setup();

        // 2️⃣ Create a new browser window using Chrome
        WebDriver driver = new ChromeDriver();

        // 3️⃣ Create a CSV file to store menu data
        FileWriter csvWriter = new FileWriter("zomato_full_menu.csv");

        // Write header row to CSV
        csvWriter.append("ID,Name,Price,Description,ImageURL,DishType\n");
        String cityName = "Your City Name";
        String restaurantNameWithHypens = "Restaurant-Name"
        try {
            // 4️⃣ Open the Zomato restaurant menu page
            String url = "https://www.zomato.com/" + 
              + cityName + "/" + restaurantNameWithHypens 
              +"-"+ cityName +"-locality-"+ cityName +"/order";
            
          driver.get(url);

            // 5️⃣ Wait until menu items load (looking for <h4> which contains dish names)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h4")));

            // 6️⃣ Handle infinite scroll to load all dishes
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Get initial scroll height
            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

            while (true) {
                // Scroll down to bottom
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(2000); // wait for lazy loading

                // Get new scroll height
                long newHeight = (long) js.executeScript("return document.body.scrollHeight");

                // If no more new content is loaded → break loop
                if (newHeight == lastHeight) {
                    Thread.sleep(3000); // wait extra in case something loads late
                    if ((long) js.executeScript("return document.body.scrollHeight") == newHeight) break;
                }

                // Update last height and continue scrolling
                lastHeight = newHeight;
            }

            // 7️⃣ Find all dish containers (divs that have <h4> inside them)
            List<WebElement> dishCards = driver.findElements(By.xpath("//div[h4]"));

            // ID counter for CSV
            int id = 1;

            // 8️⃣ Loop through each dish card and extract data
            for (WebElement card : dishCards) {
                try {
                    String name = "";
                    String price = "";
                    String description = "";
                    String imageUrl = "";
                    String dishType = "";

                    // Dish Name
                    try { 
                        name = card.findElement(By.tagName("h4")).getText(); 
                    } catch (Exception e) {}

                    // Dish Price
                    try { 
                        price = card.findElement(By.tagName("span")).getText(); 
                    } catch (Exception e) {}

                    // Dish Description
                    try { 
                        description = card.findElement(By.tagName("p")).getText(); 
                    } catch (Exception e) {}

                    // Dish Image URL
                    try { 
                        imageUrl = card.findElement(By.tagName("img")).getAttribute("src"); 
                    } catch (Exception e) {}

                    // Dish Type (Veg / Non-Veg)
                    try { 
                        // Find veg/non-veg icon (svg inside a specific div)
                        WebElement typeElem = card.findElement(By.xpath(".//div[contains(@class,'sc-bUyWVT')]//svg/use"));
                        String href = typeElem.getAttribute("href");

                        if (href.contains("veg")) dishType = "Veg";
                        else if (href.contains("non-veg")) dishType = "Non-Veg";
                    } catch (Exception e) {}

                    // ✅ Only write if dish name is found
                    if (!name.isEmpty()) {
                        csvWriter.append(id + "," 
                            + escapeCsv(name) + "," 
                            + escapeCsv(price) + "," 
                            + escapeCsv(description) + "," 
                            + escapeCsv(imageUrl) + "," 
                            + escapeCsv(dishType) + "\n");
                        id++;
                    }

                } catch (Exception e) {
                    // Skip this card if anything fails
                }
            }

            // Print success message after scraping
            System.out.println("Scraping completed! Check zomato_full_menu.csv");

        } finally {
            // 9️⃣ Save and close file, quit browser
            csvWriter.flush();
            csvWriter.close();
            driver.quit();
        }
    }

    // 🔹 Utility method to handle commas/quotes inside CSV
    private static String escapeCsv(String value) {
        if (value == null) value = "";
        if (value.contains(",") || value.contains("\"")) {
            value = value.replace("\"", "\"\""); // escape quotes
            value = "\"" + value + "\"";        // wrap in quotes
        }
        return value;
    }
}
