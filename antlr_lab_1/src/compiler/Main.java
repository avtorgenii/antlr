package compiler;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import grammar.ExprLexer;
import grammar.ExprParser;

import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        CharStream inp = null;

        try {
            inp = CharStreams.fromFileName("src/compiler/compiler.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        CharStream inp = CharStreams.fromString("1\n2+3+4","wejście");
        ExprLexer lex = new ExprLexer(inp);

        // Лексер разбивает текст на токены (ключевые слова, числа, операторы) и складывает их в "очередь" (CommonTokenStream)
        CommonTokenStream tokens = new CommonTokenStream(lex);
        ExprParser par = new ExprParser(tokens);

        // Запускаем парсинг с самого верхнего правила (program).
        // Результат — дерево разбора (ParseTree), отражающее структуру кода.
        ParseTree tree = par.program();

        //st group
//        STGroup.trackCreationEvents = true;

        // Здесь мы выбираем "стратегию": генерировать стековый код или регистровый.
        STGroup group = new STGroupFile("src/compiler/stack.stg");

        EmitVisitor em = new EmitVisitor(group);

        // Запускаем обход дерева. Визитор идет по узлам дерева (например, BinOp), берет нужный шаблон из STGroup и заполняет его данными из дерева.
        ST res = em.visit(tree);

        // Заполнить шаблоны и высветить выход
        System.out.println(res.render());

        // Записать выход в файл
        try {
            var wr = new FileWriter("src/compiler/out.asm");
            wr.write(res.render());
            wr.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        res.inspect();
    }
}
