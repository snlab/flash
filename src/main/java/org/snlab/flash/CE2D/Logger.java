package org.snlab.flash.CE2D;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Logger {
    private List<String> buffer = new ArrayList<>();
    private String filename;
    public long startAt; // for benchmark

    public Logger(String filename) {
        this.filename = filename;
    }

    public void setLogFile(String filename) {
        this.filename = filename;
    }

    public void log(String str) {
        this.buffer.add(str);
    }

    public void logPrintln(String str) {
        this.buffer.add(str);
        System.out.println(str);
    }

    public void writeFile() {
        if(!Files.exists(Path.of(filename))) {
            File f = new File(filename);
            f.getParentFile().mkdirs();
        }
        
        try {
            FileWriter writer = new FileWriter(filename, false);
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            for (String str : buffer) {
                bufferedWriter.write(str);
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
