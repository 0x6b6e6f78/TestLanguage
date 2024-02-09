import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Objects;

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
            new Interpreter(lines).interpret();
        }
    }
}
