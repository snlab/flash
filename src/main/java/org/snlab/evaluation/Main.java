package org.snlab.evaluation;

import org.apache.commons.cli.ParseException;
import org.snlab.flash.CE2D.Logger;

import java.io.IOException;

public class Main {
    static public EvalOptions evalOptions = new EvalOptions();

    public static void main(String[] args) {
        try {
            evalOptions.parse(args);
        } catch (ParseException e) {
            if (!e.getMessage().equals("help")) {
                System.out.println(e.getMessage());
                evalOptions.showUsage();
            }
            System.exit(1);
        }
        
        Runner.run();
    }
}
