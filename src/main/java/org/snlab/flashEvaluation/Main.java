package org.snlab.flashEvaluation;

import org.apache.commons.cli.ParseException;

public class Main {
    public static void main(String[] args) {
        EvalOptions options = new EvalOptions();
        try {
            options.parse(args);
        } catch (ParseException e) {
            if (!e.getMessage().equals("help")) {
                System.out.println(e.getMessage());
                options.showUsage();
            }
            System.exit(1);
        }
        
        Runner runner = new Runner(options);
        runner.run();
    }
}
