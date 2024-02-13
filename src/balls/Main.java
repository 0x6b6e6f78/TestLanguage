package balls;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        new Main();
    }

    public Main() throws FileNotFoundException {
        File dir = new File("").getAbsoluteFile();
        for (File file : dir.listFiles()) {
            if (!file.getName().endsWith(".fmb")) {
                continue;
            }
            BufferedReader reader = new BufferedReader(new FileReader(file));
            List<String> lines = reader.lines().toList();
            new Interpreter(file.getName().substring(0, file.getName().length() - 4), lines);
        }
    }
}
