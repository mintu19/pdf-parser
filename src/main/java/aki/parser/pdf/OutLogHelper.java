package aki.parser.pdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

class OutLogHelper {

    private final static String FORMATTER = "%-10s %-5s %-35s %-50s %s\n";

    File outFolder;
    PrintWriter trackWriter, csvWriter;

    public OutLogHelper(File outFolder) throws FileNotFoundException {
        this.outFolder = outFolder;

        setupWriter();
    }

    private void setupWriter() throws FileNotFoundException {
        File trackFile = new File(this.outFolder, "track.txt");
        File csvFile = new File(this.outFolder, "generated.csv");

        trackWriter = new PrintWriter(trackFile);
        csvWriter = new PrintWriter(csvFile);

        // clear files
        this.close();

        trackWriter = new PrintWriter(trackFile);   // close at end
        csvWriter = new PrintWriter(csvFile);   // close at end
    }

    public void close() {
        trackWriter.close();
        csvWriter.close();
    }

    public void writeLog(long count, boolean result, String builderName, String projectName, String path) {
        trackWriter.printf(FORMATTER, count, result, builderName, projectName, path);
        csvWriter.print(count + "," + result + "," + builderName + "," + projectName + "," + path);
    }
}