import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Visitor extends ExprParserBaseVisitor<Object> {
    // Хранилище переменных: имя -> true/false
    private final Map<String, Object> variables = new HashMap<>();

    private <T> void print(T value) {
        System.out.println(value);
    }

    // visit() - заставляет программу спуститься глубже в дерево разбора и выполнить там соответствующую логику. Вызывает нужную функцию из определенных судя по типу ctx
    @Override
    public Object visitProgram(ExprParser.ProgramContext ctx) {
        // 1. Обрабатываем все утверждения (stat)
        // Метод ctx.stat() возвращает List, поэтому используем цикл for-each
        for (ExprParser.StatContext sContext : ctx.stat()) {
            Object result = visit(sContext);
            print("Read statement, result: " + result);
        }

        // 2. Обрабатываем все определения функций (def)
        // Если в грамматике def*, то это тоже будет список
        for (ExprParser.DefContext dContext : ctx.def()) {
            print("Read function");
            visit(dContext);
        }

        return true; // Программа прочитана полностью
    }

    // ##### STAT #####

    // 1. Декларация переменной
    @Override
    public Object visitDeclare(ExprParser.DeclareContext ctx) {
        String type = ctx.type().getText();
        String name = ctx.ID().getText();
        Object value = visit(ctx.expr());

        // Тут можно добавить проверку: если type == "int", а value == "строка" -> ОШИБКА
        variables.put(name, value);
        return value;
    }

    // 2. Просто выражение: x + 5;
    @Override
    public Object visitStatement(ExprParser.StatementContext ctx) {
        // Просто вычисляем и возвращаем результат
        return visit(ctx.expr());
    }

    @Override
    public Object visitIfStat(ExprParser.IfStatContext ctx) {
        // 1. Вычисляем условие в скобках
        Object condition = visit(ctx.cond);

        // 2. Проверяем, истинно ли оно (нужно привести к Boolean)
        if (isTrue(condition)) {
            // Выполняем ветку 'then'
            return visit(ctx.then);
        } else if (ctx.elseBlock != null) { // Проверяем, есть ли else
            // Выполняем ветку 'else'
            return visit(ctx.elseBlock);
        }

        return null;
    }

    // Вспомогательный метод для проверки "истинности"
    private boolean isTrue(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Integer) return (Integer) value != 0;
        return value != null;
    }

    // ##### DEF #####
    @Override
    public Object visitDef(ExprParser.DefContext ctx) {
        final Token name = ctx.name;
        final List<Token> par = ctx.par;
//        final ExprParser.BlockContext body = ctx.body;
        return super.visitDef(ctx);
    }

    // ##### EXPR #####
    // Изменение существующей переменной (y = ...) внутри expr
    @Override
    public Object visitAssign(ExprParser.AssignContext ctx) {
        String name = ctx.ID().getText();
        Object value = visit(ctx.expr());

        if (!variables.containsKey(name)) {
            throw new RuntimeException("Ошибка: переменная " + name + " не объявлена!");
        }

        variables.put(name, value);
        return value; // ОБЯЗАТЕЛЬНО возвращаем значение, чтобы цепочка x = y = 5 работала
    }

    @Override
    public Object visitId(ExprParser.IdContext ctx) {
        String name = ctx.ID().getText();
        if (!variables.containsKey(name)) {
            throw new RuntimeException("Undefined variable: " + name);
        }
        return variables.get(name);
    }

    @Override
    public Object visitIntLit(ExprParser.IntLitContext ctx) {
        return Integer.parseInt(ctx.INT().getText());
    }


    @Override
    public Object visitBinOp(ExprParser.BinOpContext ctx) {
        Object left = visit(ctx.l);
        Object right = visit(ctx.r);
        int tokenType = ctx.op.getType();

        // 1. ЛОГИКА ДЛЯ ЧИСЕЛ (Integer и Float)
        if (left instanceof Number && right instanceof Number) {
            // Проверяем, нужно ли нам работать в режиме Float
            // Если хотя бы одно число дробное — результат будет дробным
            boolean isFloat = (left instanceof Float || right instanceof Float);

            if (isFloat) {
                float l = ((Number) left).floatValue();
                float r = ((Number) right).floatValue();
                return switch (tokenType) {
                    case ExprLexer.ADD -> l + r;
                    case ExprLexer.SUB -> l - r;
                    case ExprLexer.MUL -> l * r;
                    case ExprLexer.DIV -> {
                        if (r == 0) throw new RuntimeException("Division by zero!");
                        yield l / r;
                    }
                    case ExprLexer.EQUAL -> l == r;
                    case ExprLexer.NOTEQUAL -> l != r;
                    case ExprLexer.GT -> l > r;
                    case ExprLexer.LT -> l < r;
                    default -> throw new RuntimeException("Unknown numeric operator");
                };
            } else {
                // Оба числа целые
                int l = (Integer) left;
                int r = (Integer) right;
                return switch (tokenType) {
                    case ExprLexer.ADD -> l + r;
                    case ExprLexer.SUB -> l - r;
                    case ExprLexer.MUL -> l * r;
                    case ExprLexer.DIV -> {
                        if (r == 0) throw new RuntimeException("Division by zero!");
                        yield l / r; // Целочисленное деление (как в Java)
                    }
                    case ExprLexer.EQUAL -> l == r;
                    case ExprLexer.NOTEQUAL -> l != r;
                    case ExprLexer.GT -> l > r;
                    case ExprLexer.LT -> l < r;
                    default -> throw new RuntimeException("Unknown integer operator");
                };
            }
        }

        // 2. ЛОГИКА ДЛЯ СТРОК (Конкатенация)
        if (tokenType == ExprLexer.ADD && (left instanceof String || right instanceof String)) {
            return String.valueOf(left) + String.valueOf(right);
        }

        // 3. ЛОГИКА ДЛЯ BOOLEAN (and / or)
        if (left instanceof Boolean && right instanceof Boolean) {
            boolean l = (Boolean) left;
            boolean r = (Boolean) right;
            return switch (tokenType) {
                case ExprLexer.AND -> l && r;
                case ExprLexer.OR -> l || r;
                default -> throw new RuntimeException("Logic operator mismatch");
            };
        }

        throw new RuntimeException("Type mismatch: " + left.getClass().getSimpleName() +
                " " + ctx.op.getText() + " " + right.getClass().getSimpleName());
    }
}