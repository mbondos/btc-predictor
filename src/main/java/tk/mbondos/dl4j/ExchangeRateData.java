package tk.mbondos.dl4j;

import java.time.LocalDate;

public class ExchangeRateData implements Comparable<ExchangeRateData>{
    private String date;
    private double open;
    private double high;
    private double low;
    private double close;

    public ExchangeRateData() {
    }

    public ExchangeRateData(String date, double open, double high, double low, double close) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    @Override
    public int compareTo(ExchangeRateData data) {
        LocalDate date1 = LocalDate.parse(getDate());
        LocalDate date2 = LocalDate.parse(data.getDate());

        return date1.compareTo(date2);
    }

    @Override
    public String toString() {
        return date  + "," +  open  + "," +  high  + "," +  low  + "," +  close;
    }
}
