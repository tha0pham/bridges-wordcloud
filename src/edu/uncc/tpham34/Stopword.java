package edu.uncc.tpham34;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Stopword {
    private Set<String> stopwords;

    public Stopword() {
        this("stopwords.txt");
    }

    public Stopword(String filepath) {
        stopwords = new HashSet<>();
        try {
            File file = new File(filepath);
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                stopwords.add(scanner.nextLine());
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.err.println("Stop words text file must be placed in " +
                    "project root folder!");
            e.printStackTrace();
        }
    }

    public boolean isStopWord(String word) {
        if (word.length() < 2) {
            return true;
        }

        if (word.charAt(0) >= '0' && word.charAt(0) <= '9') {
            return true;
        } //remove numbers, "25th", etc

        if (stopwords.contains(word)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "stopwords={" + stopwords + '}';
    }
}