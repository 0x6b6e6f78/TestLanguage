import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Interpreter {

    public static final Pattern BOOLEAN = Pattern.compile("((true|false)i?)");
    public static final Pattern STRING = Pattern.compile("(\"[^\"]*\")");
    public static final Pattern NUMBER = Pattern.compile("((-)?\\d+(\\.\\d+)?)");
    public static final Pattern INT = Pattern.compile("((-)?\\d+(\\.0)?)");
    public static final Pattern VARIABLE = Pattern.compile("(\\$[A-Za-z_]\\w*)");
    public static final Pattern TEMP_VARIABLE = Pattern.compile("(\\$[0-9]\\w*)");
    public static final Pattern SPACE = Pattern.compile("(\\s*)");
//    public static final Pattern EQUATION = Pattern.compile("((\\$\\w+)|(\"[^\"]*\")|((-)?\\d+(\\.\\d+)?))\\s*");
    public static final Pattern VAR = Pattern.compile("(" + VARIABLE + "|" + STRING + "|" + NUMBER + ")");
    public static final List<String> OPERATORS_LIST_HIGH = List.of("*", "/");
    public static final List<String> OPERATORS_LIST = List.of("==", "!=", ">=", "<=", ">", "<", "+", "-", "*", "/");
    public static final Pattern OPERATORS_HIGH = Pattern.compile(listToRegex(OPERATORS_LIST_HIGH));
    public static final Pattern OPERATORS = Pattern.compile(listToRegex(OPERATORS_LIST));
    public static final Pattern ASSIGNMENT = Pattern.compile("(" + VARIABLE + SPACE + "=[^=].+)");
    public static final Pattern EQUATION = Pattern.compile("(" + VAR + SPACE + OPERATORS + ".+)");

    public static String listToRegex(List<String> list) {
        return "(" + list.stream().map(s -> "(" + (s.equals("+") || s.equals("*") ? "\\" : "") + s + ")").collect(Collectors.joining("|")) + ")";
    }

    private List<String> lines;
    private int programCounter = 0;

    private Map<String, String> variables;
    private Map<String, String> tempVariables;

    public Interpreter(List<String> lines) {
        this.lines = lines;
        this.variables = new HashMap<>();
        this.tempVariables = new HashMap<>();
    }

    public void interpret() {
        while (lines.size() > programCounter) {
            //System.out.println("-+- NEW LINE -+-");
            String line = lines.get(programCounter);
            instruction(line);
            programCounter++;
        }
        System.out.println(variables);
    }

    private String instruction(String instruction) {
        tempVariables.clear();
        if (instruction == null) {
            return null;
        }
        instruction = instruction.trim();
        //System.out.println("instr: " + instruction);

        if (instruction.startsWith("//")) {
            return null;
        }
        if (ASSIGNMENT.matcher(instruction).matches()) {
            return assign(instruction);
        }

        instruction = tempBrackets(instruction);
        //System.out.println("brackets: " + instruction);

        List<String> operands = new ArrayList<>();
        List<String> operators = new ArrayList<>();

        String[] op = instruction.split(OPERATORS.pattern());
        String cutInstruction = instruction;
        for (int i = 0; i < op.length - 1; i++) {
            Match operator = findFirst(instruction, OPERATORS_LIST);
            if (operator == null) {
                return null;
            }
            String opp = cutInstruction.substring(op[i].length()).trim();
            operators.add(opp.substring(0, operator.text.length()));
            cutInstruction = opp.substring(operator.text.length());
        }

        return evaluate(Arrays.stream(op).map(e -> e.trim()).toList(), operators);
    }

    private String evaluate(List<String> operands, List<String> operators) {
        if (operators.isEmpty()) {
            if (!operands.isEmpty()) {
                return operands.get(0);
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
            if (i != operatorIndex && i != operatorIndex + 1) {
                newOperands.add(operands.get(i));
            }
        }
        String left = operands.get(operatorIndex);
        String right = operands.get(operatorIndex + 1);
        String operator = operators.get(operatorIndex);
        String result = handleEquation(left, right, operator);
        newOperands.add(operatorIndex, result);
        //System.out.println("oops " + operands);
        //System.out.println("oopr " + operators);
        //System.out.println("res " + result);
        //System.out.println("nops " + newOperands);
        //System.out.println("nopr " + newOperators);
        return evaluate(newOperands, newOperators);
    }

    private Match findFirst(String string, List<String> matches) {
        return matches.stream().map(match -> findFirst(string, match)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private Match findFirst(String string, String match) {
        int i = 0;
        while (i < string.length()) {
            if (string.startsWith(match, i)) {
                return new Match(match, i, string.substring(0, i), string.substring(i + match.length(), string.length() - 1));
            }
            i++;
        }
        return null;
    }

    public Tuple<String, String> getSides(String equation) {
        Match operator = findFirst(equation, OPERATORS_LIST);
        if (operator == null) {
            return null;
        }
        String left = equation.substring(0, operator.index);
        String right = equation.substring(operator.index + operator.text.length());

        return null;
    }

    private String handleEquation(String left, String right, String operator) {
//        Match match = findFirst(equation, );
//
//        right = instruction(right);
//
////        if (operator.text.equals("=")) {
////            variables.put(left.trim(), right);
////            return right;
////        }
//
//        left = instruction(left);
        left = left.trim();
        right = right.trim();
        //System.out.println("OP: " + left + operator + right);
//
        if (operator.equals("==")) {
            return Boolean.toString(left.equals(right));
        } else if (operator.equals("!=")) {
            return Boolean.toString(!left.equals(right));
        }

        if (NUMBER.matcher(left).matches() && NUMBER.matcher(right).matches()) {
            //System.out.println(interpretMath(operator, Double.parseDouble(left), Double.parseDouble(right)));
            return interpretMath(operator, Double.parseDouble(left), Double.parseDouble(right));
        }
        if (STRING.matcher(left).matches() && STRING.matcher(right).matches() && operator.equals("+")) {
            return "\"" + left.substring(1, left.length() - 1) + right.substring(1, right.length() - 1) + "\"";
        }
//        if (BOOLEAN.matcher(left).matches() && BOOLEAN.matcher(right).matches() && operator.text.equals("+")) {
//            return "\"" + left.substring(1, left.length() - 1) + right.substring(1, right.length() - 1) + "\"";
//        }
        return null;
    }

    private String tempBrackets(String instruction) {
        if (instruction == null) {
            return null;
        }
        for (int i = 0; i < instruction.length(); i++) {
            String sub = instruction.substring(i);
            if (sub.startsWith("(")) {
                String left = "";
                if (i != 0) {
                    left = instruction.substring(0, i);
                }
                return left + tempBrackets(handleBrackets(sub));
            }
        }
        return instruction;
    }

    private String handleBrackets(String brackets) {
        if (brackets.length() <= 2) {
            return null;
        }
        int open = 1;
        int i = 1;
        while (true) {
            if (open == 0) {
                String bracket = brackets.substring(1, i - 1);
//                String name = rndTempName();
//                tempVariables.put(name, bracket);
                String newInstruction = instruction(bracket) + brackets.substring(i);
                //System.out.println("newInstruction: " + newInstruction);
                return newInstruction;
            }
            if (brackets.length() <= i) {
                break;
            }
            if (brackets.charAt(i) == '(') {
                open++;
            }
            if (brackets.charAt(i) == ')') {
                open--;
            }
            i++;
        }
        return null;
    }

    private String assign(String assignment) {
        Match operator = findFirst(assignment, "=");
        if (operator != null) {
            String left = assignment.substring(0, operator.index).trim();
            String right = assignment.substring(operator.index + operator.text.length());

            if (operator.text.equals("=")) {
                variables.put(left.trim(), instruction(right));
                return right;
            }
        }
        return null;
    }

    private String nextVar(String instruction) {
        instruction = instruction.trim();

//        if (EQUATION.matcher(instruction).matches()) {
//            //System.out.println("EQUATION");
//            return handleEquation(instruction);
//        }

        if (TEMP_VARIABLE.matcher(instruction).matches()) {
            //System.out.println("TEMP_VARIABLE" + tempVariables);
            //System.out.println(tempVariables.get(instruction));
            return tempVariables.get(instruction);
        }
        if (VARIABLE.matcher(instruction).matches()) {
            //System.out.println("VARIABLE" + variables);
            //System.out.println(variables.get(instruction));
            return variables.get(instruction);
        }
        if (STRING.matcher(instruction).matches()) {
            //System.out.println("STRING");
            //System.out.println(instruction);
            return instruction;
        }
        if (NUMBER.matcher(instruction).matches()) {
            //System.out.println("NUMBER");
            //System.out.println(instruction);
            return instruction;
        }
        if (BOOLEAN.matcher(instruction).matches()) {
            //System.out.println("BOOLEAN");
            //System.out.println(instruction.toLowerCase());
            return instruction.toLowerCase();
        }
        return null;
    }

    public String interpretMath(String operator, double left, double right) {
        double result = Double.NaN;
        switch (operator) {
            case "+" -> result = (left + right);
            case "-" -> result = (left - right);
            case "/" -> result = (left / right);
            case "*" -> result = (left * right);
            case ">" -> {
                return Boolean.toString(left > right);
            }
            case "<" -> {
                return Boolean.toString(left < right);
            }
            case ">=" -> {
                return Boolean.toString(left >= right);
            }
            case "<=" -> {
                return Boolean.toString(left <= right);
            }
        }
        if (Double.isNaN(result)) {
            return null;
        }
        if (INT.matcher(Double.toString(result)).matches()) {
            return Long.toString((long) result);
        }
        return Double.toString(result);
    }

    private String randomNameExcept(Collection<String> exceptions) {
        Random random = new Random();
        int rnd;
        do {
            rnd = random.nextInt(999999);
        } while (exceptions.contains("§" + rnd));
        return "§" + rnd;
    }

    private String rndTempName() {
        return randomNameExcept(tempVariables.keySet());
    }
}
