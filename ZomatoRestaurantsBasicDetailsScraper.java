package com.devnologix; // Package name

// Jsoup imports → for parsing restaurant card HTML
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

// Selenium imports → to automate browser actions
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

// WebDriverManager → automatically downloads correct ChromeDriver version
import io.github.bonigarcia.wdm.WebDriverManager;

// Java imports
import java.io.FileWriter;   // Write to CSV
import java.io.IOException;  // Handle IO errors
import java.util.List;       // Handle multiple restaurants

public class ZomatoRestaurantsBasicDetailsScraper {

    public static void main(String[] args) throws InterruptedException, IOException {

        // 1️⃣ Setup ChromeDriver automatically
        WebDriverManager.chromedriver().setup();

        // 2️⃣ Launch Chrome browser
        WebDriver driver = new ChromeDriver();
        String cityName = "Your City Name";
         String fileName = "zomato_restaurant_names_" + cityName + ".csv";
        // 3️⃣ Create a CSV file for storing restaurant details
        FileWriter csvWriter = new FileWriter(fileName);
        csvWriter.append("ID,Name,Rating,Cuisine,CostForOne,DeliveryTime\n"); // CSV header row

        try {
            // 4️⃣ Open Zomato Your City restaurants page
            driver.manage().window().maximize();
            driver.get("https://www.zomato.com/"
                       + cityName +
                       "/restaurants");

            // Wait for initial page load (basic sleep for simplicity)
            Thread.sleep(5000);

            // 5️⃣ Auto-scroll until all restaurants load
            JavascriptExecutor js = (JavascriptExecutor) driver;
            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

            while (true) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);"); // scroll down
                Thread.sleep(2000); // wait for lazy-loaded restaurants

                long newHeight = (long) js.executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) {
                    Thread.sleep(5000); // wait extra 5 sec
                    long finalHeight = (long) js.executeScript("return document.body.scrollHeight");
                    if (finalHeight == newHeight) break; // stop when no new content loads
                }
                lastHeight = newHeight;
            }

            // 6️⃣ Get the main container div
            WebElement rootDiv = driver.findElement(By.id("root"));

            // 7️⃣ Find all restaurant blocks (each card has this class)
            List<WebElement> restaurantDivs = rootDiv.findElements(By.cssSelector("div.sc-1mo3ldo-0.sc-jGkVzM.BXbKf"));
            System.out.println("Total restaurants found: " + restaurantDivs.size());

            int id = 1; // counter for CSV

            // 8️⃣ Loop through each restaurant
            for (WebElement restaurantDiv : restaurantDivs) {
                List<WebElement> jumboDivs = restaurantDiv.findElements(By.className("jumbo-tracker"));

                for (WebElement jumbo : jumboDivs) {
                    // Parse inner HTML of each restaurant card with Jsoup
                    Document doc = Jsoup.parse(jumbo.getAttribute("innerHTML"));

                    // Extract details safely (if not found → "N/A")
                    String name = doc.selectFirst("h4.sc-1hp8d8a-0.sc-iqtXtF") != null
                            ? doc.selectFirst("h4.sc-1hp8d8a-0.sc-iqtXtF").text() : "N/A";

                    String rating = doc.selectFirst("div.sc-1q7bklc-1.cILgox") != null
                            ? doc.selectFirst("div.sc-1q7bklc-1.cILgox").text() : "N/A";

                    String cuisine = doc.select("p.sc-jtEaiv.iXNvdz").first() != null
                            ? doc.select("p.sc-jtEaiv.iXNvdz").first().text() : "N/A";

                    String cost = doc.select("p.sc-jtEaiv.fIHvpg").first() != null
                            ? doc.select("p.sc-jtEaiv.fIHvpg").first().text() : "N/A";

                    String deliveryTime = doc.select("div.min-basic-info-right p").first() != null
                            ? doc.select("div.min-basic-info-right p").first().text() : "N/A";

                    // Print to console (for debugging)
                    System.out.println("Name: " + name);
                    System.out.println("Rating: " + rating);
                    System.out.println("Cuisine: " + cuisine);
                    System.out.println("Cost for One: " + cost);
                    System.out.println("Delivery Time: " + deliveryTime);
                    System.out.println("-----------------------------");

                    // Write data to CSV
                    csvWriter.append(id + "," + escapeCsv(name) + "," + rating + "," 
                            + escapeCsv(cuisine) + "," + cost + "," + deliveryTime + "\n");
                    id++;
                }
            }

        } finally {
            // 9️⃣ Always close resources
            csvWriter.flush();
            csvWriter.close();
            driver.quit(); // close browser
        }
    }

    // 🔹 Helper method: escape commas/quotes in CSV values
    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"")) {
            value = value.replace("\"", "\"\""); // escape quotes
            value = "\"" + value + "\"";        // wrap in quotes
        }
        return value;
    }
}
