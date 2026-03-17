import java.util.ArrayList;
import java.util.List;


public class Visitor extends ExprParserBaseVisitor<Object> {
    private final Scopes<Object> memory = new Scopes<>();
    private final FunctionManager functionManager = new FunctionManager(memory);

    private <T> void print(T value) {
        System.out.println(value);
    }

    // visit() - заставляет программу спуститься глубже в дерево разбора и выполнить там соответствующую логику. Вызывает нужную функцию из определенных судя по типу ctx
    @Override
    public Object visitProgram(ExprParser.ProgramContext ctx) {
        // 1. Сначала собираем все определения функций
        for (ExprParser.DefContext dContext : ctx.def()) {
            // Вызов visit(dContext) должен сохранять функцию в Map
            visit(dContext);
        }

        // 2. Затем по порядку выполняем все инструкции (stat)
        for (ExprParser.StatContext sContext : ctx.stat()) {
            Object result = visit(sContext);
            print("Read statement, result: " + result);
        }

        return "Omae wa mou shindeiru";
    }


    // ##### STAT #####
    // 1. Декларация переменной
    @Override
    public Object visitDeclare(ExprParser.DeclareContext ctx) {
        String type = ctx.type().getText();
        String name = ctx.ID().getText();
        Object value = visit(ctx.expr());

        memory.declareSymbol(name, value);

        return value;
    }

    // 2. Просто выражение: x + 5;
    @Override
    public Object visitStatement(ExprParser.StatementContext ctx) {
        // Просто вычисляем и возвращаем результат
        return visit(ctx.expr());
    }

    @Override
    public Object visitBlockSingle(ExprParser.BlockSingleContext ctx) {
        // Если в блоке одна строка без скобок - просто выполняем её
        return visit(ctx.stat());
    }

    @Override
    public Object visitBlockReal(ExprParser.BlockRealContext ctx) {
        memory.enterScope();
        Object res = null;
        try {
            for (ExprParser.StatContext s : ctx.stat()) {
                res = visit(s);
            }
        } finally {
            memory.leaveScope();
        }
        return res;
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
        if (value instanceof Float) return (Float) value != 0.0f; // 0.0 - это ложь
        return value != null;
    }

    // Print
    @Override
    public Object visitPrintStat(ExprParser.PrintStatContext ctx) {
        // 1. Вычисляем то, что стоит после слова print
        Object value = visit(ctx.expr());

        // 2. Выводим в консоль
        // Можно использовать твой внутренний метод print() или сразу System.out
        System.out.println("> " + value);

        // Возвращаем само значение (хороший тон для интерпретаторов)
        return value;
    }

    // FOR LOOP
    @Override
    public Object visitForStat(ExprParser.ForStatContext ctx) {
        memory.enterScope();
        try {
            visit(ctx.init);

            while (isTrue(visit(ctx.cond))) {
                visit(ctx.body);
                visit(ctx.step);
            }
        } finally {
            memory.leaveScope();
        }
        return null;
    }


    // ##### EXPR #####
    // Изменение существующей переменной (y = ...) внутри expr
    @Override
    public Object visitAssign(ExprParser.AssignContext ctx) {
        String name = ctx.ID().getText();
        Object newValue = visit(ctx.expr());

        memory.setSymbol(name, newValue);
        return newValue;
    }

    @Override
    public Object visitId(ExprParser.IdContext ctx) {
        String name = ctx.ID().getText();

        return memory.getSymbol(name);
    }

    @Override
    public Object visitIntLit(ExprParser.IntLitContext ctx) {
        return Integer.parseInt(ctx.INT().getText());
    }

    @Override
    public Object visitFloatLit(ExprParser.FloatLitContext ctx) {
        return Float.parseFloat(ctx.getText());
    }

    @Override
    public Object visitStringLit(ExprParser.StringLitContext ctx) {
        String str = ctx.getText();
        return str.substring(1, str.length() - 1);
    }

    @Override
    public Object visitBinOp(ExprParser.BinOpContext ctx) {
        Object left = visit(ctx.l);
        Object right = visit(ctx.r);

        // Защита от null (если какой-то visit вернул null)
        if (left == null || right == null) {
            throw new RuntimeException("Runtime Error: One of the operands is null at " + ctx.getText());
        }

        int tokenType = ctx.op.getType();

        // 1. СРАВНЕНИЕ И РАВЕНСТВО
        if (tokenType == ExprLexer.EQUAL || tokenType == ExprLexer.NOTEQUAL ||
                tokenType == ExprLexer.GT || tokenType == ExprLexer.LT ||
                tokenType == ExprLexer.GE || tokenType == ExprLexer.LE) {

            if (left instanceof Number && right instanceof Number) {
                float l = ((Number) left).floatValue();
                float r = ((Number) right).floatValue();
                return switch (tokenType) {
                    case ExprLexer.EQUAL -> l == r;
                    case ExprLexer.NOTEQUAL -> l != r;
                    case ExprLexer.GT -> l > r;
                    case ExprLexer.LT -> l < r;
                    case ExprLexer.GE -> l >= r;
                    case ExprLexer.LE -> l <= r;
                    default -> false;
                };
            }

            // Для строк и булеанов только == и !=
            if (tokenType == ExprLexer.EQUAL) return left.equals(right);
            if (tokenType == ExprLexer.NOTEQUAL) return !left.equals(right);
        }

        // 2. АРИФМЕТИКА
        if (left instanceof Number && right instanceof Number) {
            boolean isFloat = (left instanceof Float || right instanceof Float);
            float l = ((Number) left).floatValue();
            float r = ((Number) right).floatValue();

            Object res = switch (tokenType) {
                case ExprLexer.ADD -> isFloat ? l + r : (int) l + (int) r;
                case ExprLexer.SUB -> isFloat ? l - r : (int) l - (int) r;
                case ExprLexer.MUL -> isFloat ? l * r : (int) l * (int) r;
                case ExprLexer.DIV -> {
                    if (r == 0) throw new RuntimeException("Division by zero");
                    yield isFloat ? l / r : (int) l / (int) r;
                }
                default -> null;
            };
            if (res != null) return res;
        }

        // 3. КОНКАТЕНАЦИЯ
        if (tokenType == ExprLexer.ADD && (left instanceof String || right instanceof String)) {
            return String.valueOf(left) + String.valueOf(right);
        }

        // 4. ЛОГИКА
        if (left instanceof Boolean && right instanceof Boolean) {
            boolean l = (Boolean) left;
            boolean r = (Boolean) right;
            return switch (tokenType) {
                case ExprLexer.AND -> l && r;
                case ExprLexer.OR -> l || r;
                default -> throw new RuntimeException("Unknown logic operator");
            };
        }

        throw new RuntimeException("Type mismatch: " + left.getClass().getSimpleName() + " " + ctx.op.getText() + " " + right.getClass().getSimpleName());
    }

    // ##### FUNCTIONS STUFF #####

    @Override
    public Object visitReturnStat(ExprParser.ReturnStatContext ctx) {
        Object value = null;
        if (ctx.expr() != null) {
            value = visit(ctx.expr());
        }
        // Бросаем исключение, чтобы мгновенно прервать выполнение стейтментов
        throw new ReturnException(value);
    }

    @Override
    public Object visitDef(ExprParser.DefContext ctx) {
        String name = ctx.name.getText();
        functionManager.register(name, ctx);
        System.out.println("Read func definition: " + name);
        return null;
    }

    @Override
    public Object visitFunCall(ExprParser.FunCallContext ctx) {
        String name = ctx.ID().getText();
        ExprParser.DefContext def = functionManager.get(name);

        if (def == null) {
            throw new RuntimeException("Функция " + name + " не определена!");
        }

        // 1. Вычисляем аргументы в ТЕКУЩЕМ скоупе (до входа в функцию)
        List<Object> actualArgs = new ArrayList<>();
        if (ctx.expr() != null) {
            for (ExprParser.ExprContext expr : ctx.expr()) {
                actualArgs.add(visit(expr));
            }
        }

        // 2. С помощью менеджера создаем новый скоуп и биндим параметры
        functionManager.prepareScope(def, actualArgs);

        // 3. Выполняем тело функции
        Object result = null;
        try {
            for (ExprParser.StatContext s : def.body) {
                result = visit(s);
            }
        } catch (ReturnException e) {
            // Если внутри сработал return — возвращаем его значение
            return e.getValue();
        } finally {
            // 4. Обязательно покидаем скоуп, даже если произошла ошибка
            memory.leaveScope();
        }

        return result;
    }
}