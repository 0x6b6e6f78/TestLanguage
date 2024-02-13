package balls;

import java.util.List;
import java.util.stream.Collectors;

public class Function {

    public String name;
    public List<String> lines;
    public List<String> parameters;

    public Function(String name, List<String> lines, List<String> parameters) {
        this.name = name;
        this.lines = lines;
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "Function{" +
                "name='" + name + '\'' +
                ", parameters=" + parameters +
                ", lines=\n" + String.join("\n", lines) +
                "\n}";
    }
}
