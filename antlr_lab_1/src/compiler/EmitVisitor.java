package compiler;

import grammar.ExprParserBaseVisitor;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import grammar.ExprLexer;
import grammar.ExprParser;

import java.util.List;

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

//    @Override
//    protected ST defaultResult() {
//        return stGroup.getInstanceOf("deflt");
//    }
//
//    // Когда визитор обходит список инструкций в программе, он получает результат для каждой строки (nextResult) и кладет его в ведро
//    @Override
//    protected ST aggregateResult(ST aggregate, ST nextResult) {
//        if (nextResult != null) aggregate.add("elem", nextResult);
//        return aggregate;
//    }

    // Этот метод срабатывает на КАЖДЫЙ символ, который не является правилом
    @Override
    public ST visitTerminal(TerminalNode node) {
        return null;
    }

    @Override
    public ST visitProgram(ExprParser.ProgramContext ctx) {
        ST program = stGroup.getInstanceOf("program");

        // First pass: emit all function definitions
        for (var child : ctx.children) {
            if (child instanceof ExprParser.DefContext) {
                program.add("defs", visit(child));
            }
        }

        // Second pass: emit all top-level statements
        for (var child : ctx.children) {
            if (child instanceof ExprParser.StatContext) {
                program.add("stats", visit(child));
            }
        }

        return program;
    }

    // ##### STATS #####
    @Override
    public ST visitDeclare(ExprParser.DeclareContext ctx) {
        String varName = ctx.ID().getText();

        // 1. Создаем декларацию: DD a
        ST decl = stGroup.getInstanceOf("decl");

        // 2. Если в строке есть присваивание (например, = 2)
        if (ctx.expr() != null) {
            ST init = stGroup.getInstanceOf("assign");
            init.add("v", visit(ctx.expr())); // Тут будет PUSH #2
            init.add("n", varName);

            // Склеиваем: сначала объявили, потом положили значение
            decl.add("n", varName + "\n" + init.render());
        } else {
            decl.add("n", varName);
        }

        return decl;
    }


    @Override
    public ST visitStatement(ExprParser.StatementContext ctx) {
        return visit(ctx.expr());
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


    // ##### FUNCTION STUFF #####
    @Override
    public ST visitDef(ExprParser.DefContext ctx) {
        ST func = stGroup.getInstanceOf("func_def");
        func.add("name", ctx.name.getText());

        if (ctx.par != null && !ctx.par.isEmpty()) {
            List<Token> parameters = ctx.par;

            // First pass: declare all parameters with DD
            for (Token param : parameters) {
                ST decl = stGroup.getInstanceOf("decl");
                decl.add("n", param.getText());
                func.add("params_decl", decl);
            }

            // Second pass: POP values from stack (reverse order, last arg on top)
            for (int i = parameters.size() - 1; i >= 0; i--) {
                ST pop = stGroup.getInstanceOf("pop_param");
                pop.add("n", parameters.get(i).getText());
                func.add("params_init", pop);
            }
        }

        for (ExprParser.StatContext stat : ctx.body) {
            func.add("body", visit(stat));
        }

        return func;
    }

    @Override
    public ST visitFunCall(ExprParser.FunCallContext ctx) {
        ST call = stGroup.getInstanceOf("call");
        call.add("name", ctx.ID().getText());

        for (ExprParser.ExprContext exprCtx : ctx.expr()) {
            ST pushArg = stGroup.getInstanceOf("push_arg");
            pushArg.add("v", visit(exprCtx));
            call.add("args", pushArg);
        }

        return call;
    }
}


