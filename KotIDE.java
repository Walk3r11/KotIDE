import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KotIDE {

    public static void main(String[] args) {

        JFrame frame = new JFrame(".kot IDE");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JTextArea codeArea = new JTextArea();
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JTextArea outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputArea.setEditable(false);

        JTextArea terminalArea = new JTextArea();
        terminalArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        terminalArea.setEditable(true);

        JScrollPane codeScrollPane = new JScrollPane(codeArea);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        JScrollPane terminalScrollPane = new JScrollPane(terminalArea);

        JButton runButton = new JButton("Run and Debug");

        frame.setLayout(new BorderLayout());
        frame.add(codeScrollPane, BorderLayout.CENTER);
        frame.add(outputScrollPane, BorderLayout.SOUTH);
        frame.add(runButton, BorderLayout.NORTH);
        frame.add(terminalScrollPane, BorderLayout.EAST);

        outputScrollPane.setPreferredSize(new Dimension(800, 100));
        terminalScrollPane.setPreferredSize(new Dimension(200, 600));

        KotInterpreter interpreter = new KotInterpreter(outputArea, terminalArea);

        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String code = codeArea.getText();
                interpreter.interpret(code);
            }
        });

        codeArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_COMMA && e.isShiftDown()) {
                    int caretPos = codeArea.getCaretPosition();
                    String textBeforeCaret = codeArea.getText().substring(0, caretPos);

                    if (textBeforeCaret.endsWith("")) {

                        codeArea.insert(">", caretPos);
                        codeArea.setCaretPosition(caretPos);
                        codeArea.replaceRange("", caretPos, caretPos);
                    }
                }
            }
        });

        terminalArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    String terminalInput = terminalArea.getText().trim();
                    terminalArea.setText("");

                    if (terminalInput.equals("clear")) {
                        outputArea.setText("");
                    } else {
                        outputArea.append("Unknown command: " + terminalInput + "\n");
                    }
                }
            }
        });

        frame.setVisible(true);
    }
}

class KotInterpreter {
    private final JTextArea outputArea;
    @SuppressWarnings("unused")
    private final JTextArea terminalArea;
    private final Map<String, Object> variables;
    private final Set<String> keywords;

    public KotInterpreter(JTextArea outputArea, JTextArea terminalArea) {
        this.outputArea = outputArea;
        this.terminalArea = terminalArea;
        this.variables = new HashMap<>();
        this.keywords = new HashSet<>(Arrays.asList("int", "double", "string", "bool", "type", "list", "in", "to"));
    }

    public void interpret(String code) {
        outputArea.setText("");
        variables.clear();

        String[] lines = code.split("\\n");
        boolean skipBlock = false;

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (skipBlock) {
                if (line.equals("}")) {
                    skipBlock = false;
                }
                continue;
            }

            if (line.startsWith("int<") && line.contains(">")) {
                handleIntDeclaration(line);
            } else if (line.startsWith("double<") && line.contains(">")) {
                handleDoubleDeclaration(line);
            } else if (line.startsWith("string<") && line.contains(">")) {
                handleStringDeclaration(line);
            } else if (line.startsWith("bool<") && line.contains(">")) {
                handleBoolDeclaration(line);
            } else if (line.startsWith("<in>(") && line.endsWith(")")) {
                handleInput(line);
            } else if (line.startsWith("<in>(") && line.contains(").to<") && line.endsWith(">")) {
                handleTypedInput(line);
            } else if (line.startsWith("type<") && line.contains(">")) {
                handleTypeCommand(line);
            } else if (line.startsWith("list<") && line.contains(")")) {
                handleListDeclaration(line);
            } else if (line.startsWith("if (") && line.contains(") {")) {
                skipBlock = !handleIfStatement(line);
            } else if (line.startsWith("(") && line.endsWith(")")) {
                handlePrintCommand(line);
            } else if (line.startsWith("f(") && line.endsWith(")")) {
                handleInlinePrint(line);
            } else if (line.contains("=")) {
                handleAssignment(line);
            } else {
                outputArea.append("Unknown command: " + line + "\n");
            }
        }
    }

    private void handleInlinePrint(String line) {
        try {

            String content = line.substring(2, line.length() - 1).trim();

            Pattern pattern = Pattern.compile("\\{([^}]+)\\}");

            Matcher matcher = pattern.matcher(content);

            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                String expression = matcher.group(1);
                String evaluatedExpression = evaluateExpression(expression);

                matcher.appendReplacement(result, Matcher.quoteReplacement(evaluatedExpression));
            }

            matcher.appendTail(result);

            outputArea.append(result.toString() + "\n");

        } catch (Exception e) {
            outputArea.append("Error in inline print: " + line + "\n");
        }
    }

    private String evaluateExpression(String expression) {
        try {

            if (expression.contains("pow(")) {
                return evaluatePowExpression(expression);
            }

            if (expression.contains("sqrt(")) {
                return evaluateSqrtExpression(expression);
            }

            if (expression.contains("log[")) {
                return evaluateLogExpression(expression);
            }

            Pattern varPattern = Pattern.compile("([a-zA-Z_][a-zA-Z_0-9]*)");
            Matcher varMatcher = varPattern.matcher(expression);

            StringBuffer exprWithValues = new StringBuffer();
            while (varMatcher.find()) {
                String varName = varMatcher.group(1);
                Object value = variables.get(varName);

                String replacement = (value != null) ? value.toString() : varMatcher.group(0);
                varMatcher.appendReplacement(exprWithValues, Matcher.quoteReplacement(replacement));
            }
            varMatcher.appendTail(exprWithValues);

            String evaluatedExpression = exprWithValues.toString();
            return String.valueOf(evaluateArithmeticExpression(evaluatedExpression));

        } catch (Exception e) {
            return "Error evaluating expression";
        }
    }

    private String evaluateLogExpression(String expression) {
        try {

            Pattern logPattern = Pattern.compile("log\\[(.+?)\\]\\((.+?)\\)");
            Matcher matcher = logPattern.matcher(expression);

            if (matcher.find()) {
                String baseExpression = matcher.group(1).trim();
                String valueExpression = matcher.group(2).trim();

                double base = Double.parseDouble(evaluateExpression(baseExpression));
                double value = Double.parseDouble(evaluateExpression(valueExpression));

                double result = Math.log(value) / Math.log(base);
                return String.valueOf(result);
            }
            return "Invalid log expression";

        } catch (Exception e) {
            return "Error in log expression";
        }
    }

    private String evaluatePowExpression(String expression) {
        try {

            Pattern powPattern = Pattern.compile("pow\\(([^,]+),\\s*([^\\)]+)\\)");
            Matcher matcher = powPattern.matcher(expression);

            if (matcher.find()) {
                String baseExpression = matcher.group(1).trim();
                String exponentExpression = matcher.group(2).trim();

                double base = Double.parseDouble(evaluateExpression(baseExpression));
                double exponent = Double.parseDouble(evaluateExpression(exponentExpression));

                double result = Math.pow(base, exponent);
                return String.valueOf(result);
            }
            return "Invalid pow expression";

        } catch (Exception e) {
            return "Error in pow expression";
        }
    }

    private String evaluateSqrtExpression(String expression) {
        try {

            Pattern sqrtPattern = Pattern.compile("sqrt\\(([^\\)]+)\\)");
            Matcher matcher = sqrtPattern.matcher(expression);

            if (matcher.find()) {
                String argumentExpression = matcher.group(1).trim();

                double argument = Double.parseDouble(evaluateExpression(argumentExpression));

                double result = Math.sqrt(argument);
                return String.valueOf(result);
            }
            return "Invalid sqrt expression";

        } catch (Exception e) {
            return "Error in sqrt expression";
        }
    }

    private double evaluateArithmeticExpression(String expression) {
        // Replace variables with their values
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String varName = entry.getKey();
            Object value = entry.getValue();
            expression = expression.replace(varName, value.toString());
        }

        // Handle arithmetic operations (+, -, *, /)
        if (expression.contains("+")) {
            String[] parts = expression.split("\\+");
            return evaluateArithmeticExpression(parts[0].trim()) + evaluateArithmeticExpression(parts[1].trim());
        } else if (expression.contains("-")) {
            String[] parts = expression.split("-");
            return evaluateArithmeticExpression(parts[0].trim()) - evaluateArithmeticExpression(parts[1].trim());
        } else if (expression.contains("*")) {
            String[] parts = expression.split("\\*");
            return evaluateArithmeticExpression(parts[0].trim()) * evaluateArithmeticExpression(parts[1].trim());
        } else if (expression.contains("/")) {
            String[] parts = expression.split("/");
            return evaluateArithmeticExpression(parts[0].trim()) / evaluateArithmeticExpression(parts[1].trim());
        } else {
            // If it's just a number, return it
            return Double.parseDouble(expression.trim());
        }
    }

    private void handleAssignment(String line) {
        try {
            String[] parts = line.split("=");
            if (parts.length != 2) {
                outputArea.append("Invalid assignment: " + line + "\n");
                return;
            }

            String varName = parts[0].trim();
            String value = parts[1].trim();

            if (!variables.containsKey(varName)) {
                outputArea.append("Undefined variable: " + varName + "\n");
                return;
            }

            Object resolvedValue = resolveValue(value);
            if (resolvedValue == null) {
                outputArea.append("Error resolving value for assignment: " + value + "\n");
                return;
            }

            variables.put(varName, resolvedValue);

        } catch (Exception e) {
            outputArea.append("Error in assignment: " + line + "\n");
        }
    }

    private boolean handleIfStatement(String line) {
        try {

            line = line.replace("=<", "<=");

            int conditionStart = line.indexOf("(") + 1;
            int conditionEnd = line.indexOf(")");
            String condition = line.substring(conditionStart, conditionEnd).trim();

            String operator = "";
            if (condition.contains(">="))
                operator = ">=";
            else if (condition.contains("<="))
                operator = "<=";
            else if (condition.contains("=="))
                operator = "==";
            else if (condition.contains(">"))
                operator = ">";
            else if (condition.contains("<"))
                operator = "<";

            if (operator.isEmpty()) {
                outputArea.append("Invalid condition: " + condition + "\n");
                return false;
            }

            String[] parts = condition.split("\\Q" + operator + "\\E");
            if (parts.length != 2) {
                outputArea.append("Error parsing condition: " + condition + "\n");
                return false;
            }

            String left = parts[0].trim();
            String right = parts[1].trim();

            Object leftValue = resolveValue(left);
            Object rightValue = resolveValue(right);

            if (leftValue == null || rightValue == null) {
                outputArea.append("Error: Undefined variable in condition.\n");
                return false;
            }

            return compareValues(leftValue, rightValue, operator);
        } catch (Exception e) {
            outputArea.append("Error parsing if statement: " + line + "\n");
            return false;
        }
    }

    private boolean compareValues(Object leftValue, Object rightValue, String operator) {
        try {

            double leftNum = Double.parseDouble(leftValue.toString());
            double rightNum = Double.parseDouble(rightValue.toString());

            switch (operator) {
                case ">":
                    return leftNum > rightNum;
                case "<":
                    return leftNum < rightNum;
                case "==":
                    return leftNum == rightNum;
                case ">=":
                    return leftNum >= rightNum;
                case "<=":
                    return leftNum <= rightNum;
                default:
                    outputArea.append("Invalid operator: " + operator + "\n");
                    return false;
            }
        } catch (NumberFormatException e) {
            outputArea.append("Error comparing values: " + leftValue + " and " + rightValue + "\n");
            return false;
        }
    }

    private Object resolveValue(String value) {

        if (variables.containsKey(value)) {
            return variables.get(value);
        }
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private void handleIntDeclaration(String line) {
        try {
            int startIndex = line.indexOf("<") + 1;
            int endIndex = line.indexOf(">");
            String varName = line.substring(startIndex, endIndex);

            if (keywords.contains(varName)) {
                outputArea.append("Error: " + varName + " is a reserved keyword.\n");
                return;
            }

            int valueIndex = endIndex + 1;
            int value = Integer.parseInt(line.substring(valueIndex).trim());

            variables.put(varName, value);
        } catch (Exception e) {
            outputArea.append("Error parsing line: " + line + "\n");
        }
    }

    private void handleDoubleDeclaration(String line) {
        try {
            int startIndex = line.indexOf("<") + 1;
            int endIndex = line.indexOf(">");
            String varName = line.substring(startIndex, endIndex);

            if (keywords.contains(varName)) {
                outputArea.append("Error: " + varName + " is a reserved keyword.\n");
                return;
            }

            int valueIndex = endIndex + 1;
            double value = Double.parseDouble(line.substring(valueIndex).trim());

            variables.put(varName, value);
        } catch (Exception e) {
            outputArea.append("Error parsing line: " + line + "\n");
        }
    }

    private void handleStringDeclaration(String line) {
        try {
            int startIndex = line.indexOf("<") + 1;
            int endIndex = line.indexOf(">");
            String varName = line.substring(startIndex, endIndex);

            if (keywords.contains(varName)) {
                outputArea.append("Error: " + varName + " is a reserved keyword.\n");
                return;
            }

            int valueIndex = endIndex + 1;
            String value = line.substring(valueIndex).trim();

            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }

            variables.put(varName, value);
        } catch (Exception e) {
            outputArea.append("Error parsing line: " + line + "\n");
        }
    }

    private void handleBoolDeclaration(String line) {
        try {
            int startIndex = line.indexOf("<") + 1;
            int endIndex = line.indexOf(">");
            String varName = line.substring(startIndex, endIndex);

            if (keywords.contains(varName)) {
                outputArea.append("Error: " + varName + " is a reserved keyword.\n");
                return;
            }

            int valueIndex = endIndex + 1;
            String valueStr = line.substring(valueIndex).trim();

            if (valueStr.equals("true") || valueStr.equals("false")) {
                boolean value = Boolean.parseBoolean(valueStr);
                variables.put(varName, value);
            } else {
                outputArea.append("Error: Invalid boolean value: " + valueStr + "\n");
            }
        } catch (Exception e) {
            outputArea.append("Error parsing line: " + line + "\n");
        }
    }

    private void handleInput(String line) {
        try {
            String varName = line.substring(5, line.length() - 1).trim();
            String inputValue = JOptionPane.showInputDialog("Enter value for " + varName + ":");

            if (inputValue.contains(".")) {
                variables.put(varName, Double.parseDouble(inputValue));
            } else {
                variables.put(varName, Integer.parseInt(inputValue));
            }
        } catch (Exception e) {
            outputArea.append("Error parsing input command: " + line + "\n");
        }
    }

    private void handleTypedInput(String line) {
        try {
            int startIndex = line.indexOf("<in>(") + 5;
            int endIndex = line.indexOf(").to<");
            String varName = line.substring(startIndex, endIndex).trim();

            String targetType = line.substring(line.indexOf(").to<") + 5, line.length() - 1).trim();

            if (keywords.contains(varName)) {
                outputArea.append("Error: " + varName + " is a reserved keyword.\n");
                return;
            }

            String inputValue = JOptionPane
                    .showInputDialog("Enter value for " + varName + " (type: " + targetType + "):");
            if (inputValue != null) {
                Object value = null;

                switch (targetType) {
                    case "int":
                        value = Integer.parseInt(inputValue);
                        break;
                    case "double":
                        value = Double.parseDouble(inputValue);
                        break;
                    case "string":
                        value = inputValue;
                        break;
                    case "bool":
                        value = Boolean.parseBoolean(inputValue);
                        break;
                    default:
                        outputArea.append("Error: Unsupported target type " + targetType + ".\n");
                        return;
                }

                variables.put(varName, value);
            }
        } catch (Exception e) {
            outputArea.append("Error parsing type casting input command: " + line + "\n");
        }
    }

    private void handleTypeCommand(String line) {
        try {
            int startIndex = line.indexOf("<") + 1;
            int endIndex = line.indexOf(">");
            String varName = line.substring(startIndex, endIndex);

            if (variables.containsKey(varName)) {
                Object value = variables.get(varName);
                outputArea.append(varName + " is of type: " + value.getClass().getSimpleName() + "\n");
            } else {
                outputArea.append("Undefined variable: " + varName + "\n");
            }
        } catch (Exception e) {
            outputArea.append("Error parsing type command: " + line + "\n");
        }
    }

    private void handleListDeclaration(String line) {
        try {
            int startIndex = line.indexOf("<") + 1;
            int endIndex = line.indexOf(">");
            String varName = line.substring(startIndex, endIndex);

            if (keywords.contains(varName)) {
                outputArea.append("Error: " + varName + " is a reserved keyword.\n");
                return;
            }

            int maxSizeStart = endIndex + 1;
            int maxSizeEnd = line.indexOf("(", maxSizeStart);
            int maxSize = Integer.parseInt(line.substring(maxSizeStart, maxSizeEnd).trim());

            String elementsPart = line.substring(maxSizeEnd + 1, line.length() - 1);
            String[] elements = elementsPart.split(",");

            List<Object> list = new ArrayList<>();
            for (String element : elements) {
                if (list.size() < maxSize) {
                    list.add(element.trim());
                } else {
                    outputArea.append("Warning: List exceeded max size. Remaining elements ignored.\n");
                    break;
                }
            }

            variables.put(varName, list);
        } catch (Exception e) {
            outputArea.append("Error parsing list command: " + line + "\n");
        }
    }

    private void handlePrintCommand(String line) {
        try {
            String content = line.substring(1, line.length() - 1).trim();

            if (content.startsWith("\"") && content.endsWith("\"")) {

                outputArea.append(content.substring(1, content.length() - 1) + "\n");
            } else if (variables.containsKey(content)) {

                outputArea.append(variables.get(content) + "\n");
            } else {
                outputArea.append("Undefined variable: " + content + "\n");
            }
        } catch (Exception e) {
            outputArea.append("Error parsing print command: " + line + "\n");
        }
    }

    @SuppressWarnings("unused")
    private void handleMultilinePrint(String line) {
        try {
            String content = line.substring(3, line.length() - 2).trim();
            outputArea.append(content.replaceAll("\\\\n", "\n") + "\n");
        } catch (Exception e) {
            outputArea.append("Error parsing multiline print command: " + line + "\n");
        }
    }
}
