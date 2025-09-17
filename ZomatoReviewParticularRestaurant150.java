package com.devnologix;

// ✅ GSON for parsing JSON reviews embedded inside <script> tags
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// ✅ WebDriverManager automatically handles ChromeDriver installation
import io.github.bonigarcia.wdm.WebDriverManager;

// ✅ Selenium imports
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

// ✅ Java imports
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ZomatoReviewParticularRestaurant150 {
    public static void main(String[] args) {
        // 1️⃣ Setup ChromeDriver
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        String cityName = "Your City Name";
        String fileName = "zomato_reviews_" + cityName + ".csv";
        try {
            // 2️⃣ Create or open CSV file to save reviews
            File file = new File(fileName);
            boolean fileExists = file.exists(); // check if already present
            FileWriter csvWriter = new FileWriter(file, true); // append mode

            // 3️⃣ Write header if file is new
            if (!fileExists) {
                csvWriter.append("Restaurant,Author,Rating,Review\n");
            }
            String readFileName = "";
            // 4️⃣ Read restaurant names (from previously scraped file)
            List<String> restaurantNames = readRestaurantNames("zomato_restaurant_names_"+ cityName +".csv");

            // ⚠️ Example: scraping restaurants from index 0 to 2 (2 restaurants only)
            for (int id = 0; id < 2; id++) {
                String restaurantName = restaurantNames.get(id);

                // Loop over 30 review pages per restaurant
                for (int i = 1; i <= 30; i++) {
                    // Construct review page URL
                    String url = "https://www.zomato.com/" + 
                            cityName+ "/"
                            + restaurantName
                            + "-" + cityName + "-locality-" + cityName + "/reviews?page="
                            + i + "&sort=dd&filter=reviews-dd";

                    // Save reviews for this page
                    boolean checker = saveReviews(driver, url, csvWriter);

                    // If no reviews found → stop going further for this restaurant
                    if (checker) {
                        System.out.println("⚠️ No Review or End of Reviews for: " + restaurantName);
                        break;
                    }
                }
            }

            // 5️⃣ Flush & close writer after all scraping
            csvWriter.flush();
            csvWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 6️⃣ Quit ChromeDriver once all scraping is done
            driver.quit();
        }
    }

    /**
     * 👉 Visits a Zomato review page, extracts JSON-LD reviews,
     *    and writes them to CSV.
     * @return true if no reviews found (stop further pages), false otherwise
     */
    public static boolean saveReviews(WebDriver driver, String url, FileWriter csvWriter) {
        try {
            // 1️⃣ Open the given review page
            driver.get(url);
            Thread.sleep(5000); // wait for JS to render page

            // 2️⃣ Get all <script type="application/ld+json"> tags
            List<WebElement> scripts = driver.findElements(By.xpath("//script[@type='application/ld+json']"));

            String jsonString = null;

            // 3️⃣ Find the script that contains "reviews"
            for (WebElement script : scripts) {
                String content = script.getAttribute("innerHTML").trim();
                if (content.contains("\"reviews\"")) {
                    jsonString = content;
                    break;
                }
            }

            // 4️⃣ If JSON not found → stop further pages
            if (jsonString == null) {
                System.out.println("❌ Reviews JSON not found on: " + url);
                return true;
            }

            // 5️⃣ Parse JSON string into object
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(jsonString);
            JsonObject restaurant = element.getAsJsonObject();

            // Get restaurant name
            String restaurantName = restaurant.has("name")
                    ? restaurant.get("name").getAsString()
                    : "Unknown";

            // 6️⃣ If no "reviews" field → stop further pages
            if (!restaurant.has("reviews")) {
                System.out.println("❌ No reviews in JSON on: " + url);
                return true;
            }

            // Random delay (5–10 sec) to mimic human behavior
            Random random = new Random();
            int delay = 5000 + random.nextInt(5000);
            Thread.sleep(delay);

            // 7️⃣ Extract reviews array
            JsonArray reviews = restaurant.getAsJsonArray("reviews");

            // 8️⃣ Loop through each review object
            for (JsonElement r : reviews) {
                JsonObject review = r.getAsJsonObject();

                // Extract review fields safely
                String author = review.get("author").getAsString().replace(",", " ");
                String description = review.get("description").getAsString().replace(",", " ");
                int rating = review.getAsJsonObject("reviewRating").get("ratingValue").getAsInt();

                // Write review into CSV file
                csvWriter
                        .append(restaurantName).append(",")
                        .append(author).append(",")
                        .append(String.valueOf(rating)).append(",")
                        .append(description).append("\n");
            }

            System.out.println("✅ Reviews appended from: " + url);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // keep scraping next page
    }

    /**
     * 👉 Reads restaurant names from CSV file and formats them
     *    for use in Zomato URLs.
     */
    public static List<String> readRestaurantNames(String filePath) {
        List<String> names = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                // skip header row
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                // Split CSV line
                String[] values = line.split(",", -1);
                if (values.length < 2) continue; // need at least ID + Name

                // Replace spaces with "-" to make URL-compatible name
                String name = values[1].trim().replace(" ", "-");
                names.add(name);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return names;
    }
}
