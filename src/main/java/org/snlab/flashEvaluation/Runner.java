package org.snlab.flashEvaluation;

public class Runner {
    private EvalOptions options;

    public Runner(EvalOptions options) {
        this.options = options;
    }

    public void run() {
        if (options.eval.equals("I2LongTail")) {
            I2LongTail.run();
        }
    }

    
}
