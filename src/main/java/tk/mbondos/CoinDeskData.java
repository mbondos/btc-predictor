package tk.mbondos;

import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;

public class CoinDeskData {
    private HttpsURLConnection connection;
    private BufferedReader reader;
    private StringBuilder stringBuilder;
    private URL url;

    private String stringUrl = "https://api.coindesk.com/v1/bpi/historical/close.json";


    public CoinDeskData() {
        this(1024);
    }

    public CoinDeskData(int initialCapacity) {
        stringBuilder = new StringBuilder(initialCapacity);
    }

    private void getBpi(URL url) {
        String line;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(0);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0");
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            stringBuilder.setLength(0);

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append(System.lineSeparator());
            }

        } catch (IOException e) {
            e.printStackTrace(); //TODO custom exception
        }

    }

    public String getBpiUsingDateRange(LocalDate startDate, LocalDate endDate) {
        setUrl(String.format("%s?start=%s&end=%s", stringUrl, startDate, endDate));
        getBpi(this.url);
        String filename = "data/btc_" + startDate + "_" + endDate + ".csv";
        writeDataToFile(filename);
        return filename;

    }

    public String getBpiLast31Days() {
        setUrl(stringUrl);
        getBpi(this.url);
        String filename = "data/btc_last_31_days.csv";
        writeDataToFile(filename);
        return filename;
    }

    public String getBpiLifetime() {
        setUrl(String.format("%s?start=%s&end=%s", stringUrl, LocalDate.of(2010, 7, 19) , LocalDate.now()));
        getBpi(this.url);
        String filename = "data/btc_lifetime.csv";
        writeDataToFile(filename);
        return filename;
    }

    private String getResponse() {
        return stringBuilder.toString();
    }

    private void writeDataToFile(String filename) {
        if (stringBuilder.length() == 0) {
            throw new RuntimeException();
        }
        JSONObject jsonObject = new JSONObject(getResponse());
        JSONObject bpi = jsonObject.getJSONObject("bpi");

        String bpiString = bpi.toString();
        bpiString = bpiString.substring(1, bpiString.length() - 1);
        String[] tokens = bpiString.split(",");
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filename));

            for (String token : tokens) {
                bufferedWriter.write(token);
                bufferedWriter.newLine();
            }

            bufferedWriter.flush();
            bufferedWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public URL getUrl() {
        return url;
    }

    private void setUrl(String urlString) {
        if (this.url == null || !urlString.equals(url.toString())) {
            try {
                this.url = new URL(urlString);
            } catch (MalformedURLException e) {
                e.printStackTrace(); //TODO custom exception
            }
        }
    }
}
