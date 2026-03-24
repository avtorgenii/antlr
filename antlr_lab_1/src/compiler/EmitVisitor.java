package compiler;

import grammar.ExprParserBaseVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import grammar.ExprLexer;
import grammar.ExprParser;

public class EmitVisitor extends ExprParserBaseVisitor<ST> {
    private final STGroup stGroup;
    private int labelCounter = 0;

    public EmitVisitor(STGroup group) {
        super();
        this.stGroup = group;
    }

    private int getNextLabelId() {
        return labelCounter++;
    }

    @Override
    protected ST defaultResult() {
        return stGroup.getInstanceOf("deflt");
    }

    // Когда визитор обходит список инструкций в программе, он получает результат для каждой строки (nextResult) и кладет его в ведро
    @Override
    protected ST aggregateResult(ST aggregate, ST nextResult) {
        if (nextResult != null) aggregate.add("elem", nextResult);
        return aggregate;
    }

    // Этот метод срабатывает на КАЖДЫЙ символ, который не является правилом
    @Override
    public ST visitTerminal(TerminalNode node) {
        return new ST("Terminal node:<n>").add("n", node.getText());
    }

    // ##### STATS #####
    @Override
    public ST visitDeclare(ExprParser.DeclareContext ctx) {
        String varName = ctx.ID().getText();

        // 1. Создаем декларацию: DD a
        ST decl = stGroup.getInstanceOf("decl");
        decl.add("n", varName);

        // 2. Если в строке есть присваивание (например, = 2)
        if (ctx.expr() != null) {
            ST init = stGroup.getInstanceOf("assign");
            init.add("v", visit(ctx.expr())); // Тут будет PUSH #2
            init.add("n", varName);

            // Склеиваем: сначала объявили, потом положили значение
            decl.add("n", varName + "\n" + init.render());
        }

        return decl;
    }


    // ##### EXPRESSIONS #####
    @Override
    public ST visitIntLit(ExprParser.IntLitContext ctx) {
        ST st = stGroup.getInstanceOf("int");
        st.add("i", ctx.INT().getText());
        return st;
    }

    @Override
    public ST visitId(ExprParser.IdContext ctx) {
        // Используем шаблон "load", который превратится в PUSH [имя]
        ST st = stGroup.getInstanceOf("load");
        st.add("n", ctx.ID().getText());
        return st;
    }

    @Override
    public ST visitAssign(ExprParser.AssignContext ctx) {
        ST st = stGroup.getInstanceOf("assign");

        // Имя переменной (например, "x")
        String varName = ctx.ID().getText();
        // Генерируем код для правой части
        ST valueCode = visit(ctx.expr());

        st.add("n", varName);
        st.add("v", valueCode);

        return st;
    }

    @Override
    public ST visitBinOp(ExprParser.BinOpContext ctx) {
        // Получаем тип токена
        int type = ctx.op.getType();
        String templateName;

        // Выбираем имя шаблона в зависимости от типа операции
        templateName = switch (type) {
            case ExprLexer.ADD -> "sum";
            case ExprLexer.SUB -> "sub";
            case ExprLexer.MUL -> "mul";
            case ExprLexer.DIV -> "div";

            case ExprLexer.EQUAL -> "eq";
            case ExprLexer.GT -> "gt";
            case ExprLexer.LT -> "lt";
            case ExprLexer.NOTEQUAL -> "ne";
            default -> throw new RuntimeException("Unknown operator: " + ctx.op.getText());
        };

        ST st = stGroup.getInstanceOf(templateName);
        st.add("p1", visit(ctx.l));
        st.add("p2", visit(ctx.r));

        // Если это сравнение, добавляем уникальный номер для меток
        if (type == ExprLexer.GT || type == ExprLexer.LT || type == ExprLexer.EQUAL || type == ExprLexer.NOTEQUAL) {
            st.add("addr", getNextLabelId());
        }

        return st;
    }
}
