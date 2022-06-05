package org.snlab.evaluation;

import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class EvalOptions {
    /**
     * The evaluation name
     */
    public String eval;
    public String dataset;
    public boolean enableFIMT = true;
    public boolean enablePAT = true;
    public boolean enableCE2D = true;
    public boolean debug = false;
    public String output;

    public String mode = "CE2D";

    private CommandLineParser parser = new DefaultParser();
    private Options options = new Options();

    public EvalOptions() {
        options.addRequiredOption("e", "eval", true, "The evaluation to be run");
        options.addOption("d", "dataset", true, "The dataset for evaluation");
        options.addOption("disableFIMT", false, "Disable Fast Inverse Model Transformation");
        options.addOption("disablePAT", false, "Disable Persistent Action Tree");
        options.addOption("disableCE2D", false, "Disable Consistent Efficient Early Detection");
        options.addOption("r", "req", true, "Verification requirement");
        options.addOption("o", "output", true, "Output report file");
        options.addOption("h", "help", false, "Print this message");
        options.addOption("debug", false, "Enable debug mode");

        options.addOption("mode", true, "XXX mode");
    }

    public void showUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java org.snlab.flashEvaluation.Main", options, true);
    }

    public void parse(String[] args) throws ParseException {
        if (Arrays.asList(args).contains("-h") || Arrays.asList(args).contains("--help")) {
            showUsage();
            throw new ParseException("help");
        }
        CommandLine c = parser.parse(options, args);

        if (!c.hasOption("e")) {
            throw new ParseException("Missing required option: ee");
        }
        eval = c.getOptionValue("e");
        dataset = c.getOptionValue("d");
        
        if (c.hasOption("disableFIMT")) {
            enableFIMT = false;
        }
        if (c.hasOption("disablePAT")) {
            enablePAT = false;
        }
        if (c.hasOption("disableCE2D")) {
            enableCE2D = false;
        }
        if (c.hasOption("debug")) {
            debug = true;
        }
        if (c.hasOption("mode")) {
            mode = c.getOptionValue("mode");
        }
    }
}
