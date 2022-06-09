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
        } else if (Main.evalOptions.eval.equals("overall-placeholder")) {
            Table3.run(); // TODO: control batch-size
        } else {
            System.out.println("Unknown evaluation name: " + Main.evalOptions.eval);
        }
    }

    public static void main(String[] args) {
        Table3.run();
    }
}
