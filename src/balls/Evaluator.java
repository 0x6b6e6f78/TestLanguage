package balls;

public class Evaluator {

    public String evaluateBitOperations(String operator, int left, int right) {
        switch (operator) {
            case "|" -> {
                return "" + (left | right);
            }
            case "&" -> {
                return "" + (left & right);
            }
        }
        return null;
    }

    public String evaluateMath(String operator, double left, double right) {
        double result = Double.NaN;
        switch (operator) {
            case "+" -> result = (left + right);
            case "-" -> result = (left - right);
            case "/" -> result = (left / right);
            case "*" -> result = (left * right);
            case "%" -> result = (left % right);
            case "^" -> result = Math.pow(left, right);
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
        if (Executer.INT.matcher(Double.toString(result)).matches()) {
            return Long.toString((long) result);
        }
        return Double.toString(result);
    }

    public String evaluateString(String operator, String left, String right) {
        left = left.substring(1, left.length() - 1);
        if (Executer.INT.matcher(right).matches() && operator.equals("-")) {
            return "\"" + left.substring(0, Math.max(0, left.length() - Integer.parseInt(right))) + "\"";
        }
        if (Executer.INT.matcher(right).matches() && operator.equals("*")) {
            return "\"" + left.repeat(Math.max(0, Integer.parseInt(right))) + "\"";
        }
        if (operator.equals("+")) {
            if (Executer.STRING.matcher(right).matches()) {
                right = right.substring(1, right.length() - 1);
            }
            return "\"" + left + right + "\"";
        }
        return null;
    }

    public String evaluateBoolean(String operator, boolean left, boolean right) {
        switch (operator) {
            case "|" -> {
                return "" + (left || right);
            }
            case "&" -> {
                return "" + (left && right);
            }
            case "^" -> {
                return "" + (left ^ right);
            }
        }
        return null;
    }
}
