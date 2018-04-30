package tk.mbondos;

import org.json.JSONObject;
import tk.mbondos.dl4j.ExchangeRateData;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoinDeskData {
    private StringBuilder stringBuilder;
    private URL url;

    private String stringUrl = "https://api.coindesk.com/v1/bpi/historical/close.json";
    private String filenamePrefix = "src/main/resources/";


    public CoinDeskData() {
        this(1024);
    }

    public CoinDeskData(int initialCapacity) {
        stringBuilder = new StringBuilder(initialCapacity);
    }

    private void getBpi(URL url) {
        String line;
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(0);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            stringBuilder.setLength(0);

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append(System.lineSeparator());
            }

        } catch (IOException e) {
            e.printStackTrace(); //TODO custom exception
        }

    }

    public String getClosePriceDateRange(LocalDate startDate, LocalDate endDate) {
        setUrl(String.format("%s?start=%s&end=%s", stringUrl, startDate, endDate));
        getBpi(this.url);
        String filename = "data/btc_close_range.csv";
        writeDataToFile(filename);
        return filename;

    }

    public String getClosePriceLast31Days() {
        setUrl(stringUrl);
        getBpi(this.url);
        String filename = "data/btc_last_31_days.csv";
        writeDataToFile(filename);
        return filename;
    }

    public String getClosePriceLifetime() {
        setUrl(String.format("%s?start=%s&end=%s", stringUrl, LocalDate.of(2010, 7, 19), LocalDate.now()));
        getBpi(this.url);
        String filename = "data/btc_lifetime.csv";
        writeDataToFile(filename);
        return filename;
    }

    public String getOhlcPriceLast31Days() {
        setUrl("https://api.coindesk.com/v1/bpi/historical/ohlc.json");
        getBpi(this.url);
        String filename = "data/btc_ohlc_last_31_days.csv";
        writeOhlcDataToFile(filename);
        return filename;
    }

    public String getOhlcPriceLifetime() {
        setUrl(String.format("%s?start=%s&end=%s", "https://api.coindesk.com/v1/bpi/historical/ohlc.json", LocalDate.of(2010, 7, 19), LocalDate.now()));
        getBpi(this.url);
        String filename = "data/btc_ohlc_lifetime.csv";
        writeOhlcDataToFile(filename);
        return filename;
    }

    public String getOhlcPriceDateRange(LocalDate startDate, LocalDate endDate) {
        setUrl(String.format("%s?start=%s&end=%s", "https://api.coindesk.com/v1/bpi/historical/ohlc.json", startDate, endDate));
        getBpi(this.url);
        String filename = "data/btc_ohlc_range.csv";
        writeOhlcDataToFile(filename);
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
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(filenamePrefix + filename)));

            for (String token : tokens) {
                token = token.replace(":", ",");
                bufferedWriter.write(token);
                bufferedWriter.newLine();
            }

            bufferedWriter.flush();
            bufferedWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeOhlcDataToFile(String filename) {
        if (stringBuilder.length() == 0) {
            throw new RuntimeException();
        }
        JSONObject jsonObject = new JSONObject(getResponse());
        JSONObject bpi = jsonObject.getJSONObject("bpi");

        String bpiString = bpi.toString();
        bpiString = bpiString.substring(1, bpiString.length() - 1);
        String[] tokens = bpiString.split("},");
        List<ExchangeRateData> data = new ArrayList<>();
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(filenamePrefix + filename)));

            for (String token : tokens) {
                String date = token.substring(1, 11);
                String ohlcString = token.substring(15, token.length() - 1);
                String[] ohlcArray = ohlcString.split(",");
                int i = 0;
                double[] priceArray = new double[4];
                for (String priceString : ohlcArray) {
                    String[] splitPrice = priceString.split(":");
                    if (splitPrice.length == 2)
                    priceArray[i] = Double.valueOf(splitPrice[1]);
                    i++;
                }
                //format : date, open, high, low, close
                data.add(new ExchangeRateData(date, priceArray[3], priceArray[0],  priceArray[1], priceArray[2]));
            }
            Collections.sort(data);
            for (ExchangeRateData line : data) {
                bufferedWriter.write(line.toString());
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
