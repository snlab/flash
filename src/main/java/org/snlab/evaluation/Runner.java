package org.snlab.evaluation;

public class Runner {

    static public void run() {
        if (Main.evalOptions.eval.equals("I2LongTail")) {
            I2LongTail.run();
        } else if (Main.evalOptions.eval.equals("I2CE2D")) {
            I2CE2D.run();
        } else if (Main.evalOptions.eval.equals("I2EarlyDetection")) {
            I2EarlyDetection.run();
        } else {
            System.out.println("Unknown evaluation name: " + Main.evalOptions.eval);
        }
    }

}
