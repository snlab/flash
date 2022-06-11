package org.snlab.evaluation;

public class Runner {

    static public void run() {
        if (Main.evalOptions.eval.equals("I2LongTail")) {
            I2LongTail.run();
        } else if (Main.evalOptions.eval.equals("I2CE2D")) {
            I2CE2D.run();
        } else if (Main.evalOptions.eval.equals("I2EarlyDetection")) {
            I2EarlyDetection.run();
        } else if (Main.evalOptions.eval.equals("LNet1AllPair")) {
            LNet1AllPair.run();
        } else if (Main.evalOptions.eval.equals("OverallPerformance")) {
            Table3.run();
        } else if (Main.evalOptions.eval.equals("BatchSize")) {
            Figure9.run();
        } else if (Main.evalOptions.eval.equals("Breakdown")) {
            Table3.breakdown();
        } else if (Main.evalOptions.eval.equals("DeadSettings")) {
            Table3.dead();
        } else {
            System.out.println("Unknown evaluation name: " + Main.evalOptions.eval);
        }
    }
}
