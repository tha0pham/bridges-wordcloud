package edu.uncc.tpham34;

import bridges.base.Label;
import bridges.base.SymbolCollection;
import bridges.connect.Bridges;
import bridges.connect.DataSource;
import bridges.data_src_dependent.Shakespeare;
import edu.uncc.tpham34.stemmer.PorterStemmer;
import edu.uncc.tpham34.stemmer.Stemmer;
import org.lemurproject.kstem.KrovetzStemmer;

import java.util.*;
import java.util.stream.Collectors;

import static edu.uncc.tpham34.Config.*;


public class Main {

    static final String DELIMITERS = " !@#$%^&*()-=_+;'\":,.\\/<>?[]{}\n\r";
    private static final Random rnd = new Random(Integer.MAX_VALUE);
    static int noOfWords = 15;

    public static void main(String[] args) throws Exception {
        //create Bridges object
        Bridges bridges = new Bridges(ASSIGNMENT_NUMBER, USERNAME, API_KEY);

        // use the clone server
        bridges.setServer("clone");

        // title, description
        bridges.setTitle("Word Cloud Demo");
        bridges.setDescription("Display word cloud using labels");

        // create some symbols and add to symbol collection
        SymbolCollection sc = new SymbolCollection();

        // Get a List of Shakespeare objects from Bridges
        DataSource ds = bridges.getDataSource();
        List<Shakespeare> mylist = ds.getShakespeareData();

        // Filter only plays from Shakespeare's work
        List<Shakespeare> plays = mylist.stream().filter(shakespeare -> shakespeare.getType().equals("play")).collect(Collectors.toList());

        // Get text from the first play and convert to lowercase
        String document = plays.get(0).getText().toLowerCase();

        /*
         * Text processing:
         * 1. Tokenize
         * 2. Filter stop words
         * 3. Filter stem words
         */

        // Tokenize document, ignoring delimiters as results
        StringTokenizer tokenizer = new StringTokenizer(document, DELIMITERS, false);

        // Use stopwords.txt if no file path is given
        Stopword stopword = new Stopword();

        // Use Porter's algorithm for English by default
        Stemmer stemmer = new PorterStemmer();
        KrovetzStemmer preStemmer = new KrovetzStemmer();

        //stem => list of original words
        Map<String, List<String>> stemMap = new HashMap<>();

        //word => frequency
        Map<String, Integer> frequencyMap = new HashMap<>();

        while (tokenizer.hasMoreTokens()) {
            // read the next word
            String word = tokenizer.nextToken();

            // filter stop words
            if (stopword.isStopWord(word)) {
                continue;
            }

            // stem the word
            String stem = stemmer.stem(preStemmer.stem(word));
            if (stopword.isStopWord(stem)) {
                continue;
            }

            if (!stemMap.containsKey(stem)) {
                stemMap.put(stem, new ArrayList<>());
            }
            stemMap.get(stem).add(word);
        }

        //restore the most popular word variant
        for (String s : stemMap.keySet()) {
            Map<String, Integer> variants = new HashMap<>();
            int frequency = 0;

            for (String w : stemMap.get(s)) {
                if (!variants.containsKey(w)) {
                    variants.put(w, 0);
                }

                variants.put(w, variants.get(w) + 1);
                frequency++;
            }

            String bestVariant = null;
            for (String variant : variants.keySet()) {
                if (bestVariant == null || variants.get(variant) > variants.get(bestVariant)) {
                    bestVariant = variant;
                }
            }

            // set frequency to the total of all variants
            frequencyMap.put(bestVariant, frequency);
        }

        // Sort map and limit to the given number of words
        Map<String, Integer> sortedMap = limit(sortByValue(frequencyMap), noOfWords);

        // Remember max frequency to scale font size
        int maxFrequency = sortedMap.values().iterator().next();

        // Create a map of word-fontSize
        Map<String, Float> wordMap = scaleFontSize(sortedMap, maxFrequency);


        /*
         * Layout - Wordle's algorithm:
         *
         * place the word where it wants to be
         * while it intersects any of the previously placed words
         *     move it one step along an ever-increasing spiral
         *
         * Source: https://stackoverflow.com/a/1478314
         */

        // Populate list of labels
        List<Label> labelList = new ArrayList<>();
        for (String word : wordMap.keySet()) {
            Label l = new Label(word);
            l.setFontSize(wordMap.get(word));

            labelList.add(l);
        }

        List<Label> placedLabelList = new ArrayList<>();
        double radius = 1; // starting radius

        for (Label label : labelList) {
            makeInitialPosition(label);

            while (checkCollision(label, placedLabelList)) {
                updatePosition(label, radius++);
            }

            // update placed label list
            placedLabelList.add(label);

            // add the placed label to symbol collection
            sc.addSymbol(label);
        }

        // set visualizer type
        bridges.setDataStructure(sc);

        // visualize the JSON and Collection
        bridges.visualize();
    }

    private static void updatePosition(Label label, double radius) {
        float[] boundingBox = label.getBoundingBox();

        // randomly spiral out
        double theta = rnd.nextDouble() * 2 * Math.PI;
        double x = (radius * Math.cos(theta));
        double y = (radius * Math.sin(theta));

        label.setLocation(boundingBox[0] + x, boundingBox[1] + y);

    }

    private static boolean checkCollision(Label label, List<Label> placedLabelList) {
        for (Label each : placedLabelList) {
            if (checkCollision(label, each)) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkCollision(Label l1, Label l2) {
        float[] bb1 = l1.getBoundingBox(); // centerX, centerY, width, height
        float[] bb2 = l2.getBoundingBox(); // centerX, centerY, width, height

        float l1_x = bb1[0];
        float l1_y = bb1[1];
        float l1_w = bb1[2] - bb1[0];
        float l1_h = bb1[3] - bb1[1];

        float l2_x = bb2[0];
        float l2_y = bb2[1];
        float l2_w = bb2[2] - bb2[0];
        float l2_h = bb2[3] - bb2[1];

        return l2_x + l2_w > l1_x && l2_y + l2_h > l1_y && l1_x + l1_w > l2_x && l1_y + l1_h > l2_y;
    }

    /**
     * Randomly set the location of the given label
     *
     * @param label Label whose location is to be set
     */
    private static void makeInitialPosition(Label label) {
        float x, y;
        int randomNumber = 20;

        x = (float) (rnd.nextDouble() * randomNumber);
        y = (float) (rnd.nextDouble() * randomNumber);

        label.setLocation(x, y);
    }

    /**
     * Sort a map by values in descending order
     *
     * @param unsortedMap Map to be sorted by values
     *
     * @return sorted map Map sorted by values
     */
    private static Map<String, Integer> sortByValue(Map<String, Integer> unsortedMap) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(unsortedMap.entrySet());
        list.sort(Map.Entry.comparingByValue((o1, o2) -> o2 - o1));

        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Limit a map to a number of entries. Map must be pre-sorted.
     *
     * @param sortedMap Map sorted by values
     * @param limit Number of elements after shorting the map
     *
     * @return A new map with limited number of entries
     */
    private static Map<String, Integer> limit(Map<String, Integer> sortedMap, int limit) {
        int count = 0;
        List<Map.Entry<String, Integer>> list = new ArrayList<>(sortedMap.entrySet());

        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : list) {
            if (count == limit) {
                break;
            } else {
                result.put(entry.getKey(), entry.getValue());
                count++;
            }
        }
        return result;
    }

    /**
     * This method takes in a sorted map of word-frequency and scales the
     * frequencies to the allowed font size range in BRIDGES. Minimum frequency
     * is always set to 1. The font size range used in this method is [10, 80].
     *
     * @param map The sorted map of word-frequency
     * @param max The maximum frequency
     *
     * @return A map of word-fontSize
     */
    private static Map<String, Float> scaleFontSize(Map<String, Integer> map, int baseMax) {
        float maxAllowed = 80f;
        float minAllowed = 10f;

        float baseMin = 1f;

        Map<String, Float> result = new LinkedHashMap<>();
        for (String key : map.keySet()) {
            result.put(key, scale(map.get(key), baseMin, baseMax, minAllowed, maxAllowed));
        }
        return result;
    }

    /**
     * This method scales a number to a new range given the previous max and min
     * values.
     *
     * @param unscaled   The number to be scaled
     * @param baseMin    The original min value
     * @param baseMax    The original max value
     * @param minAllowed The min value of the new range
     * @param maxAllowed The max value of the new range
     *
     * @return A scaled number in range [minAllowed, maxAllowed] that is
     * proportional to the unscaled number in range [baseMin, baseMax]
     */
    private static float scale(float unscaled, final float baseMin, final float baseMax, final float minAllowed, final float maxAllowed) {
        return ((maxAllowed - minAllowed) * (unscaled - baseMin) / (baseMax - baseMin)) + minAllowed;
    }
}
