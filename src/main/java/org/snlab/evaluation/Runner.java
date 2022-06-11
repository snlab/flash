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
            Overall.run();
        } else if (Main.evalOptions.eval.equals("BatchSize")) {
            BatchSize.run();
        } else if (Main.evalOptions.eval.equals("Breakdown")) {
            Overall.breakdown();
        } else if (Main.evalOptions.eval.equals("DeadSettings")) {
            Overall.dead();
        } else {
            System.out.println("Unknown evaluation name: " + Main.evalOptions.eval);
        }
    }
}
