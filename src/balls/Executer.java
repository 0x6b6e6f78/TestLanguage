package balls;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Executer {
    public static final Pattern RETURN = Pattern.compile("((return)i?)\\s+.+");
    public static final Pattern IF = Pattern.compile("((if)i?)");
    public static final Pattern ELSE_IF = Pattern.compile("((elif|elseif|else if)i?)");
    public static final Pattern ELSE = Pattern.compile("((else)i?)");
    public static final Pattern NULL = Pattern.compile("((null)i?)");
    public static final Pattern BOOLEAN = Pattern.compile("((true|false)i?)");
    public static final Pattern STRING = Pattern.compile("(\"[^\"]*\")");
    public static final Pattern NUMBER = Pattern.compile("(((-)?\\d+(\\.\\d+)?)|((-)?\\.\\d+))");
    public static final Pattern INT = Pattern.compile("((-)?\\d+(\\.0)?)");
    public static final Pattern VARIABLE = Pattern.compile("(\\$[A-Za-z_]\\w*)");
    public static final Pattern SPACE = Pattern.compile("(\\s+)");
    public static final List<String> OPERATORS_LIST_HIGH = List.of("*", "/");
    public static final List<String> OPERATORS_LIST = List.of("==", "!=", ">=", "<=", ">", "<", "+", "-", "*", "/", "^", "%", "!", "&", "|", "#");
    public static final Pattern OPERATORS = Pattern.compile(listToRegex(OPERATORS_LIST));
    public static final Pattern ASSIGNMENT = Pattern.compile("(" + VARIABLE + SPACE + "=[^=].+)");

    public static String listToRegex(List<String> list) {
        return "(" + list.stream().map(s -> "(" + (s.equals("+") || s.equals("*") || s.equals("^") || s.equals("|") ? "\\" : "") + s + ")").collect(Collectors.joining("|")) + ")";
    }

    private static final Executer EMPTY = new Executer(new ArrayList<>(), null, null);

    public static Executer empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return this.lines.isEmpty();
    }

    private final Evaluator evaluator;
    private final Executer executer;
    private final Interpreter interpreter;
    private final List<String> lines;
    private int programCounter = 0;
    private boolean seeElse = false;
    private boolean returnResult = false;

    private final Map<String, String> variables;

    public Executer(List<String> lines, Executer executer, Interpreter interpreter) {
        this.lines = lines;
        this.variables = new HashMap<>();
        this.evaluator = new Evaluator();
        this.executer = executer;
        this.interpreter = interpreter;
    }

    public String interpret() {
        try {
            while (lines.size() > programCounter) {
                String line = lines.get(programCounter);
                String result = instruction(line, true);
                if (returnResult) {
                    return result;
                }
                programCounter++;
            }
            programCounter = 0;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error at " + interpreter.name + ":" + programCounter);
        }
        return null;
    }

    private String instruction(String instruction, boolean beginOfLine) {
        if (instruction == null || instruction.isEmpty() || SPACE.matcher(instruction).matches()) {
            return null;
        }
        if (instruction.trim().startsWith("//")) {
            return null;
        }

        // IF ELSE
        if (beginOfLine) {
            Executer executer = checkIfStatement(instruction);
            if (executer != null) {
                if (!executer.isEmpty()) {
                    return executer.interpret();
                }
                return null;
            }
        }
        instruction = instruction.trim();
        if (RETURN.matcher(instruction).matches()) {
            this.returnResult();
            return instruction.substring(6).trim();
        }
        if (ASSIGNMENT.matcher(instruction).matches()) {
            return assign(instruction);
        }
        boolean log = false;
        if (instruction.startsWith("log")) {
            instruction = instruction.substring(4);
            log = true;
        }
        String result = startEquation(evaluateOperands(instruction));
        if (log) {
            System.out.println(result);
        }
        return result;
    }

    private String evaluateOperands(String instruction) {
        if (instruction == null) {
            return null;
        }
        instruction = instruction.trim();
        String left = findNextOperand(instruction);
        if (left != null) {
            String result, operator = null, rest = instruction.substring(left.length()).trim();
            boolean negate = left.startsWith("!");
            if (negate) {
                left = left.substring(1);
            }
            if (left.startsWith("$")) {
                result = getAsPrimitive(left);
            } else if (left.startsWith("(")) {
                result = startEquation(evaluateOperands(left.substring(1, left.length() - 1)));
            } else if (Interpreter.CALL.matcher(left).matches()) {
                result = evaluateFunction(left);
            } else {
                result = left;
            }
            if (negate) {
                if (BOOLEAN.matcher(result).matches()) {
                    result = negateBoolean(result);
                } else {
                    return null;
                }
            }
            if (!rest.isEmpty() && rest.matches(OPERATORS.pattern() + ".+")) {
                operator = OPERATORS_LIST.stream().filter(rest::startsWith).findFirst().orElse(null);
            }
            if (operator != null) {
                rest = rest.substring(operator.length()).trim();
                return result + operator + evaluateOperands(rest);
            }
            return result;
        }
        return null;
    }

    private String findNextOperand(String instruction, String separatorMatch) {
        String left = null;
        if (instruction.matches("![^=].*")) {
            String result = findNextOperand(instruction.substring(1));
            return "!" + result;
        }
//        if (instruction.matches("(\\(" + OPERATORS.pattern() + "-\\)|-).+")) {
//            String result = findNextOperand(instruction.substring(1));
//            return "-" + result;
//        }
        String nextOperand = findNextBracketOperand(instruction);
        if (nextOperand != null) {
            if (!nextOperand.startsWith("(")) {
                if (Interpreter.CALL.matcher(nextOperand).matches()) {
                    left = nextOperand;
                }
                if (Interpreter.METHOD.matcher(nextOperand).matches()) {
                    return null;
                }
            } else {
                left = nextOperand;
            }
        } else {
            if (instruction.length() >= 2 && instruction.startsWith("\"")) {
                StringBuilder followingString = new StringBuilder("\"");
                for (int i = 1; i < instruction.length(); i++) {
                    char c = instruction.charAt(i);
                    followingString.append(c);
                    if (c == '"') {
                        break;
                    }
                }
                left = followingString.toString();
            } else {
                left = instruction.trim().split(separatorMatch)[0];
                if (!left.startsWith("$")) {
                    return getAsPrimitive(left);
                }
            }
        }
        return left;
    }
    private String findNextOperand(String instruction) {
        return findNextOperand(instruction, "(\\s+)|" + OPERATORS.pattern());
    }

    private String findNextBracketOperand(String instruction) {
        if (instruction.length() <= 2 || !instruction.contains("(")) {
            return null;
        }
        instruction = instruction.trim();
        String bracket = "", left = "";
        for (int i = 0; i < instruction.length(); i++) {
            left = instruction.substring(0, i);
            if (SPACE.matcher(left).find()) {
                return null;
            }
            bracket = instruction.substring(i);
            if (bracket.startsWith("(")) {
                break;
            }
        }
        int open = 0;
        int i = 0;
        while (true) {
            if (open == 0 && i != 0) {
                return left + bracket.substring(0, i);
            }
            if (bracket.length() <= i) {
                break;
            }
            if (bracket.charAt(i) == '(') {
                open++;
            }
            if (bracket.charAt(i) == ')') {
                open--;
            }
            i++;
        }
        return null;
    }

    /**
     * Returns Executer of code that should run, if the if-statement is true
     * Returns Executer.empty() if it is a if-statement, but it should not run
     * Returns null if the program can go on
     */
    private Executer checkIfStatement(String instruction) {
        int indentation = getIndentation(instruction);
        if (indentation % 2 == 1) {
            return empty();
        }
        boolean stop = false;
        boolean goInside = false;

        if (instruction.matches(IF.pattern() + ".+")) {
            String statement = instruction(instruction.replaceFirst(IF.pattern(), ""), false);
            if (statement.equalsIgnoreCase("true")) {
                goInside = true;
            } else {
                this.seeElse = true;
            }
            stop = true;
        }
        if (this.seeElse && instruction.matches(ELSE_IF.pattern() + ".+")) {
            String statement = instruction(instruction.replaceFirst(ELSE_IF.pattern(), ""), false);
            if (statement.equalsIgnoreCase("true")) {
                goInside = true;
            }
            stop = true;
        }
        if (this.seeElse && ELSE.matcher(instruction).matches()) {
            goInside = true;
            stop = true;
        }

        if (goInside) {
            this.seeElse = false;
            int allowedIndentation = indentation + 2;
            List<String> lines = new ArrayList<>();
            int i = programCounter + 1;
            while (i < this.lines.size()) {
                String line = this.lines.get(i);
                if (getIndentation(line) < allowedIndentation) {
                    break;
                }
                lines.add(line.substring(allowedIndentation));
                i++;
            }
            programCounter = i - 1;
            return new Executer(lines, this, this.interpreter);
        }
        if (indentation > 0 || stop) {
            return empty();
        }
        return null;
    }

    public String evaluateFunction(String instruction) {
        String name = instruction.replaceAll("\\(.*\\).*", "");
        List<String> parameters = fetchParameters(instruction.substring(name.length()));
        return this.interpreter.runFunction(name, parameters);
    }

    private List<String> fetchParameters(String bracket) {
        if (bracket.length() <= 2) {
            return null;
        }
        bracket = bracket.substring(1, bracket.length() - 1);
        List<String> parameters = new ArrayList<>();

        while (!bracket.isEmpty()) {
            String left = findNextParameter(bracket);
            if (left != null) {
                String rest = bracket.substring(left.length()).trim();
                if (!rest.isEmpty()) {
                    if (rest.matches("\\s*,.+")) {
                        rest = rest.trim().substring(1).trim();
                    }
                    if (rest.matches(OPERATORS.pattern() + ".+")) {
                        String operator = OPERATORS_LIST.stream().filter(rest::startsWith).findFirst().orElse(null);
                        if (operator != null) {
                            left += operator;
                            rest = rest.substring(operator.length());

                        }
                    }
                }
                bracket = rest;
                parameters.add(evaluateOperands(left));
            }
        }

        return parameters.stream().filter(p -> p != null && !p.isEmpty()).toList();
    }

    private String findNextParameter(String bracket) {
        String left = findNextOperand(bracket, "(\\s*,\\s*)");
        if (left != null) {
            String rest = bracket.substring(left.length()).trim();
            if (!rest.isEmpty()) {
                if (rest.matches("\\s*,.+")) {
                    rest = rest.trim().substring(1).trim();
                }
            }
            bracket = rest;
        }
        String result = evaluateOperands(left);
        if (left != null && !bracket.substring(left.length()).trim().matches("(\\s*,\\s*).+")) {
            String operator = OPERATORS_LIST.stream().filter(bracket::startsWith).findFirst().orElse(null); //TODO
            if (operator != null) {
                bracket = bracket.substring(operator.length());
                result += operator + findNextParameter(bracket);
            }
        }
        return result;
    }

    private String startEquation(String instruction) {
        if (instruction == null) {
            return null;
        }
        List<String> operators = new ArrayList<>();
        List<String> operands = new ArrayList<>();
        while (!instruction.isEmpty()) {
            String left = findNextOperand(instruction);
            if (left != null) {
                String rest = instruction.substring(left.length()).trim();
                if (!rest.isEmpty() && rest.matches(OPERATORS.pattern() + ".+")) {
                    String operator = OPERATORS_LIST.stream().filter(rest::startsWith).findFirst().get();
                    operators.add(operator);
                    rest = rest.substring(operator.length()).trim();
                }
                instruction = rest;
                operands.add(left);
            }
        }
        return evaluatePair(operands, operators);
    }

    private Match findFirstOperator(String string, String match) {
        int i = 0;
        while (i < string.length()) {
            if (string.startsWith(match, i)) {
                return new Match(match, i, string.substring(0, i), string.substring(i + match.length()));
            }
            i++;
        }
        return null;
    }

    private String evaluatePair(List<String> operands, List<String> operators) {
        if (operators.isEmpty()) {
            if (!operands.isEmpty()) {
                return getAsPrimitive(operands.get(0));
            }
            return null;
        }
        int operatorIndex = 0;
        if (operators.stream().anyMatch(OPERATORS_LIST_HIGH::contains)) {
            while (!OPERATORS_LIST_HIGH.contains(operators.get(operatorIndex))) {
                operatorIndex++;
            }
        }
        List<String> newOperands = new ArrayList<>();
        List<String> newOperators = new ArrayList<>();
        for (int i = 0; i < operators.size(); i++) {
            if (i != operatorIndex) {
                newOperators.add(operators.get(i));
            }
        }
        for (int i = 0; i < operands.size(); i++) {
            if (i != operatorIndex && i != operatorIndex + 1 && (operators.size() <= i || !operators.get(i).equals("!"))) {
                newOperands.add(operands.get(i));
            }
        }
        String left = operands.get(operatorIndex);
        String operator = operators.get(operatorIndex);
        String result;
        result = handleEquation(left, operands.get(operatorIndex + 1), operator);
        newOperands.add(operatorIndex, result);
        return evaluatePair(newOperands, newOperators);
    }

    private String handleEquation(String left, String right, String operator) {
        left = getAsPrimitive(left);
        right = getAsPrimitive(right);

        if (operator.equals("==") || operator.equals("!=")) {
            boolean equal = equals(left, right);
            if (operator.equals("!=")) {
                equal = !equal;
            }
            return Boolean.toString(equal);
        }
        if (operator.equals("#")) {
            boolean eq = equals(left, right);
            if (!eq) {
                System.err.println("FAILED (" + this.interpreter.name + ":" + (programCounter + 1) + "): " + lines.get(programCounter) + " => " + left + " # " + right);
            }
            return left;
        }
        if (right == null) {
            return null;
        }

        if (left != null && INT.matcher(left).matches() && INT.matcher(right).matches() && (operator.equals("|") || operator.equals("&"))) {
            return evaluator.evaluateBitOperations(operator, Integer.parseInt(left), Integer.parseInt(right));
        }

        if ((left == null || BOOLEAN.matcher(left).matches()) && BOOLEAN.matcher(right).matches()) {
            if (left == null) {
                left = "false";
                operator = "==";
                return handleEquation(left, right, operator);
            }
            return evaluator.evaluateBoolean(operator, Boolean.parseBoolean(left), Boolean.parseBoolean(right));
        }
        if (NUMBER.matcher(right).matches() && (left == null || NUMBER.matcher(left).matches())) {
            if (left == null) {
                left = "0";
            }
            return evaluator.evaluateMath(operator, Double.parseDouble(left), Double.parseDouble(right));
        }
        if (left == null) {
            return null;
        }
        if (STRING.matcher(left).matches()) {
            return evaluator.evaluateString(operator, left, right);
        }
        return null;
    }

    private String assign(String assignment) {
        Match operator = findFirstOperator(assignment, "=");
        if (operator != null) {
            if (operator.text.equals("=")) {
                String left = assignment.substring(0, operator.index).trim();
                String right = instruction(assignment.substring(operator.index + 1), false);
                putVariable(left.trim(), right);
                return right;
            }
        }
        return null;
    }

    private String getAsPrimitive(String instruction) {
        if (instruction == null) {
            return null;
        }
        instruction = instruction.trim();

        if (VARIABLE.matcher(instruction).matches()) {
            return getVariable(instruction);
        }
        if (STRING.matcher(instruction).matches()) {
            return instruction;
        }
        if (NUMBER.matcher(instruction).matches()) {
            return instruction;
        }
        if (BOOLEAN.matcher(instruction).matches()) {
            return instruction.toLowerCase();
        }
        if (NULL.matcher(instruction).matches()) {
            return instruction.toLowerCase();
        }
        return null;
    }

    private String negateBoolean(String string) {
        if ("false".equals(string)) {
            return "true";
        } else if ("true".equals(string)) {
            return "false";
        }
        return null;
    }

    private boolean equals(String string1, String string2) {
        return (string1 == null && string2 == null) || (string1 != null && string1.equals(string2));
    }

    public Map<String, String> getVariables() {
        Map<String, String> vars = new HashMap<>(this.variables);
        if (this.executer != null) {
            vars.putAll(this.executer.getVariables());
        }
        return vars;
    }

    public String getVariable(String name) {
        return variables.containsKey(name) ? variables.get(name) :
                (executer == null ? null : executer.getVariable(name));
    }

    public void putVariable(String name, String value) {
        Executer executer = this;
        while (executer != null) {
            if (executer.variables.containsKey(name)) {
                executer.variables.put(name, value);
                return;
            }
            executer = executer.executer;
        }
        variables.put(name, value);
    }

    public static int getIndentation(String instruction) {
        int indentation = 0, i = 0;
        while (instruction.length() > i) {
            if (instruction.charAt(i) == '\t') {
                indentation += 4;
            } else if (instruction.charAt(i) == ' ') {
                indentation++;
            } else {
                break;
            }
            i++;
        }
        return indentation;
    }

    private void returnResult() {
        Executer executer = this;
        executer.returnResult = true;
        while (executer.executer != null) {
            executer.returnResult = true;
            executer = executer.executer;
        }
    }
}
