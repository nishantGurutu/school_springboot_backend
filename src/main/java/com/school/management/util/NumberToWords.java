package com.school.management.util;

public class NumberToWords {

    public static String convert(long n) {
        if (n == 0) return "Zero";
        String[] ones = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
                "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
                "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
        if (n < 20) return ones[(int) n];
        if (n < 100) return tens[(int) (n / 10)] + (n % 10 > 0 ? " " + ones[(int) (n % 10)] : "");
        if (n < 1000) return ones[(int) (n / 100)] + " Hundred" + (n % 100 > 0 ? " " + convert(n % 100) : "");
        if (n < 100000) return convert(n / 1000) + " Thousand" + (n % 1000 > 0 ? " " + convert(n % 1000) : "");
        if (n < 10000000) return convert(n / 100000) + " Lakh" + (n % 100000 > 0 ? " " + convert(n % 100000) : "");
        return convert(n / 10000000) + " Crore" + (n % 10000000 > 0 ? " " + convert(n % 10000000) : "");
    }
}
