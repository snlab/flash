package org.snlab.flashEvaluation;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Main {
    public static void main(String[] args) {
        Options options = new Options();
        JCommander jc = JCommander.newBuilder().addObject(options).build();
        jc.setProgramName("flash");
        try {
            jc.parse(args);
            if (options.help) {
                jc.usage();
                System.exit(0);
            }
        } catch (ParameterException e) {
            e.printStackTrace();
            jc.usage();
        }
        Runner runner = new Runner(options);
        runner.run();
    }
}
