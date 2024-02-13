package balls;

import java.util.*;
import java.util.regex.Pattern;

public class Interpreter {

    public static final Pattern SIGNATURE = Pattern.compile("\\(\\s*(.+(\\s*,\\s*.+)*)?\\s*\\)");
    public static final Pattern FUNCTION = Pattern.compile("((function)i?)\\s+\\w+" + SIGNATURE);
    public static final Pattern TRIGGER = Pattern.compile("((trigger)i?)\\s+\\w+" + SIGNATURE);
    public static final Pattern METHOD = Pattern.compile("((trigger|function)i?)\\s+\\w+" + SIGNATURE);
    public static final Pattern CALL = Pattern.compile("\\w+" + SIGNATURE);

    public final String name;
    private final List<String> lines;
    private final Map<String, Function> functions;
    private final Map<String, List<String>> trigger;
    private final Executer classExecuter;

    public Interpreter(String name, List<String> lines) {
        this.name = name;
        this.lines = lines;
        this.functions = new HashMap<>();
        this.trigger = new HashMap<>();
        this.classExecuter = new Executer(lines, null, this);
        this.loadFunctions();
        this.classExecuter.interpret();
    }

    public void loadFunctions() {
        TempState tempState = TempState.NONE;
        String head = "";
        List<String> lines = new ArrayList<>();
        for (String line : this.lines) {
            if (line.isEmpty() || Executer.SPACE.matcher(line).matches()) {
                continue;
            }
            int indentation = Executer.getIndentation(line);
            if (indentation == 0) {
                if (tempState != TempState.NONE) {
                    addMethod(tempState, head, lines);
                    tempState = TempState.NONE;
                    head = "";
                    lines = new ArrayList<>();
                }
                if (FUNCTION.matcher(line).matches()) {
                    head = line.substring(8).trim();
                    tempState = TempState.FUNCTION;
                } else if (TRIGGER.matcher(line).matches()) {
                    head = line.substring(8).trim();
                    tempState = TempState.TRIGGER;
                } else {
                    continue;
                }
                continue;
            }
            if (tempState != TempState.NONE) {
                lines.add(line.substring(2));
            }
        }
        if (tempState != TempState.NONE) {
            addMethod(tempState, head, lines);
        }
    }

    private void addMethod(TempState tempState, String head, List<String> lines) {
        String name = head.replaceAll("\\(.*\\)", "");
        List<String> parameters = Arrays.stream(head.substring(name.length() + 1, head.length() - 1).split("\\s*,\\s*"))
                .map(parameter -> parameter.replaceAll("\\s+", "")).filter(e -> !e.isEmpty()).toList();
        if (tempState == TempState.FUNCTION) {
            functions.put(name, new Function(name, lines, parameters));
        }
        if (tempState == TempState.TRIGGER) {
            trigger.put(head, lines);
        }
    }

    public String runFunction(String name, List<String> parameters) {
        Executer executer = functions.entrySet().stream().filter(e -> e.getKey().equals(name)).map(e -> {
            Executer exe = new Executer(e.getValue().lines, this.classExecuter, this);
            for (int i = 0; i < e.getValue().parameters.size(); i++) {
                exe.putVariable(e.getValue().parameters.get(i), parameters.get(i));
            }
            return exe;
        }).findFirst().orElse(null);
        if (executer != null) {
            return executer.interpret();
        }
        return null;
    }

    public void runTrigger(String name) {
        trigger.entrySet().stream().filter(e -> e.getKey().startsWith(name + "(")).forEach(e -> new Executer(e.getValue(), this.classExecuter, this).interpret());
    }

    private enum TempState {
        NONE, FUNCTION, TRIGGER
    }
}
