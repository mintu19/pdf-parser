package aki.parser.pdf;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

class CmdHelper {

    private boolean commandFlags[] = new boolean[3];
    // Indexed - 0: init, 1: clean, 3: help

    private final static Options options = new Options();
    private final CommandLineParser parser;
    private final CommandLine cmd;

    private final String[] args;

    static {
        setOptions();
    }

    private final static String header = "Simple PDF Parser";
    private final static String footer = "a tool developed by Akshit Bhandari (Github: mintu19) based on PDFBox";

    CmdHelper(String args[]) throws ParseException {
        this.parser = new DefaultParser();
        this.cmd = parser.parse(options, args, false);
        this.args = args;

        this.processArgs();
    }

    private void processArgs() throws ParseException {
        System.out.println(args[0]);
        if (args.length < 2) {
            throw new ParseException("Invalid size of arguments: " + args.length);
        }

        if (args[0].equals("init")) {
            commandFlags[0] = true;
            if ( ! (cmd.hasOption("s") || cmd.hasOption("i")) ) {
                throw new ParseException("No directory to init");
            }
        } else if (args[0].equals("clean")) {
            commandFlags[1] = true;
            if ( ! cmd.hasOption("i") ) {
                throw new ParseException("No directory to clean");
            }
        } else if (args[0].equals("help")) {
            commandFlags[2] = true;
        } else {
            throw new ParseException("Invalid command: " + args[0]);
        }

    }

    private static void setOptions() {
        Option singleFileOption = new Option("s", "single", true, "Run only for single pdf file. Use -d for output dir");
        Option noLog = new Option("nl", "no-log", false, "Do not log output");
        Option minW = new Option("mw", "min-width", true, "Min Width of file in px");
        Option minH = new Option("mh", "min-height", true, "Min Height of file in px");
        Option aspectRatio = new Option("ar", "aspect-ratio", true, "Max Aspect Ratio to maintain (width:height)");
        Option aspectRatioDual = new Option("ard", false, "Max Aspect Ratio to maintain both width and height");
        Option outDir = new Option("d", "out-dir", true, "Change Output  Directory (Default is in place of file)");
        Option inDir = new Option("i", "in-dir", true, "Input  Directory");
        Option verbose = new Option("v", "verbose", false, "Print to console");
        Option help = new Option("h", "help", false, "Aspect Ratio to maintain both ways");

        options.addOption(singleFileOption);
        options.addOption(noLog);
        options.addOption(minW);
        options.addOption(minH);
        options.addOption(aspectRatio);
        options.addOption(aspectRatioDual);
        options.addOption(outDir);
        options.addOption(inDir);
        options.addOption(verbose);
        options.addOption(help);
    }

    public CommandLine getCmd() {
        return cmd;
    }

    public int getLength() {
        return args.length;
    }

    public boolean isInit() {
        return commandFlags[0];
    }

    public boolean isClean() {
        return commandFlags[1];
    }

    public boolean isVerbose() {
        return cmd.hasOption("v");
    }

    public boolean isHelp() {
        return commandFlags[2] || cmd.hasOption("h");
    }

    public boolean isLogging() {
        return ( ! cmd.hasOption("nl") );
    }

    public File getDir() throws ParseException {
        if ( cmd.hasOption("i") ) {
            File file = new File(cmd.getOptionValue("i"));
            if (file != null && file.isDirectory()) {
                return file;
            }  else {
                throw new ParseException("Input Directory not found!");
            }
        }
        return null;
    }

    public File getSingleFile() throws ParseException {
        if (cmd.hasOption("s") && this.getOutDir() != null) {
            File file = new File(cmd.getOptionValue("s"));
            if (file != null && file.isFile()) {
                return file;
            } else {
                throw new ParseException("Input file not found");
            }
        }
        return null;
    }

    public int getMinWidth() {
        try {
            return Integer.valueOf(cmd.getOptionValue("mw", "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getMinHeight() {
        try {
            return Integer.valueOf(cmd.getOptionValue("mh", "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Float getAspectRatio() {
        try {
            return Float.valueOf(cmd.getOptionValue("ar", "j"));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean isARDual() {
        return cmd.hasOption("ard");
    }

    public File getOutDir() throws ParseException {
        if (cmd.hasOption("d")) {
            File file = new File(cmd.getOptionValue("d"));
            if (file != null && file.isDirectory()) {
                return file;
            } else {
                throw new ParseException("Output Directory does not exists!");
            }
        }
        return null;
    }

    public static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("pdf-parser <init | clean | help> [options, arguments]", header, options, footer, true);
    }
}