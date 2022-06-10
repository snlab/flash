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
        } else if (Main.evalOptions.eval.equals("overall-placeholder")) { // TBD
            Table3.run();
        } else if (Main.evalOptions.eval.equals("VerifierHealthCheck")) { // TBD
            HealthCheck.run();
        } else if (Main.evalOptions.eval.equals("BatchSize")) { // TBD
            Figure9.run();
        } else {
            System.out.println("Unknown evaluation name: " + Main.evalOptions.eval);
        }
    }

    public static void main(String[] args) {
        // HealthCheck.run();
        Table3.run();
    }
}
