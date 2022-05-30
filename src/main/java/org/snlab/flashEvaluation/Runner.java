package org.snlab.flashEvaluation;

public class Runner {
    private Options options;

    public Runner(Options options) {
        this.options = options;
    }

    public void run() {
        if (options.eval.equals("I2LongTail")) {
            I2LongTail.run();
        }
    }

    
}
