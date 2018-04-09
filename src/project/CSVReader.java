package project;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CSVReader: for reading and parsing the given csv files (price_stocks.csv and qty_stocks.csv).
 * (Referenced https://www.mkyong.com/java/how-to-read-and-parse-csv-file-in-java/)
 */
class CSVReader {

    private static final char DEFAULT_SEPARATOR = ',';
    private static final char DEFAULT_QUOTE = '"';

    private BufferedReader br;
    private String currentLine;
    private int currentLineIndex;

    /**
     * Constructor: Instantiates a new CSVReader.
     *
     * @param csvFile the csv file to be read
     * @throws FileNotFoundException the file not found exception
     */
    CSVReader(String csvFile) throws FileNotFoundException {
        this.br = new BufferedReader(new FileReader(csvFile));
        this.currentLineIndex = 0;
    }

    /**
     * Whether there are more lines to be read
     *
     * @return true if there are more lines, false otherwise
     * @throws IOException the io exception
     */
    boolean hasNextLine() throws IOException {
        return (this.currentLine = this.br.readLine()) != null;
    }

    /**
     * Reads a line.
     *
     * @return the list of "words" in this line
     */
    List<String> readLine() {
        this.currentLineIndex++;
        return parseLine(this.currentLine);
    }

    void close() throws IOException {
        this.br.close();
    }

    /**
     * Gets current line index.
     *
     * @return the current line index
     */
    int getCurrentLineIndex() {
        return this.currentLineIndex;
    }

    /**
     * Parses a line of the csv file.
     *
     * @param cvsLine    a line in the csv file
     * @return the list of "words" in this line
     */
    private static List<String> parseLine(String cvsLine) {
        return parseLine(cvsLine, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
    }

    /**
     * Parses a line of the csv file with given separator
     * (for handling cases where a "cell" contains comma or quotation marks)
     *
     * @param cvsLine     a line in the csv file
     * @param separators  the separator to be used to parse the line such as comma
     * @param customQuote the quotation mark that should be parsed correctly in a "cell"
     * @return the line of "words" in this line
     */
    private static List<String> parseLine(String cvsLine, char separators, char customQuote) {

        List<String> result = new ArrayList<>();

        // if empty, return!
        if (cvsLine == null && cvsLine.isEmpty()) {
            return result;
        }

        if (customQuote == ' ') {
            customQuote = DEFAULT_QUOTE;
        }

        if (separators == ' ') {
            separators = DEFAULT_SEPARATOR;
        }

        StringBuffer curVal = new StringBuffer();
        boolean inQuotes = false;
        boolean startCollectChar = false;
        boolean doubleQuotesInColumn = false;

        char[] chars = cvsLine.toCharArray();

        for (char ch : chars) {

            if (inQuotes) {
                startCollectChar = true;
                if (ch == customQuote) {
                    inQuotes = false;
                    doubleQuotesInColumn = false;
                } else {
                    // Fixed : allow "" in custom quote enclosed
                    if (ch == '\"') {
                        if (!doubleQuotesInColumn) {
                            curVal.append(ch);
                            doubleQuotesInColumn = true;
                        }
                    } else {
                        curVal.append(ch);
                    }
                }
            } else {
                if (ch == customQuote) {

                    inQuotes = true;

                    // Fixed : allow "" in empty quote enclosed
                    if (chars[0] != '"' && customQuote == '\"') {
                        curVal.append('"');
                    }

                    // double quotes in column will hit this!
                    if (startCollectChar) {
                        curVal.append('"');
                    }

                } else if (ch == separators) {

                    result.add(curVal.toString());

                    curVal = new StringBuffer();
                    startCollectChar = false;

                } else if (ch == '\r') {
                    // ignore LF characters
                    continue;
                } else if (ch == '\n') {
                    // the end, break!
                    break;
                } else {
                    curVal.append(ch);
                }
            }
        }
        result.add(curVal.toString());

        return result;
    }
}
