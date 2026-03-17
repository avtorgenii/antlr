import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class FunctionManager {
    private final Map<String, ExprParser.DefContext> functions = new HashMap<>();
    private final Scopes<Object> scopes;

    public FunctionManager(Scopes<Object> scopes) {
        this.scopes = scopes;
    }

    public void register(String name, ExprParser.DefContext ctx) {
        functions.put(name, ctx);
    }

    public ExprParser.DefContext get(String name) {
        return functions.get(name);
    }

    /**
     * Подготовка области видимости для функции
     * @param def - контекст определения функции
     * @param actualArgs - вычисленные значения, которые мы передаем
     */
    public void prepareScope(ExprParser.DefContext def, List<Object> actualArgs) {
        // Проверяем количество аргументов
        if (def.par.size() != actualArgs.size()) {
            throw new RuntimeException("Функция " + def.name.getText() + 
                " ожидает " + def.par.size() + " аргументов, но получено " + actualArgs.size());
        }

        // Создаем новый "Lower Scope" для функции
        scopes.enterScope();

        // Связываем параметры с аргументами
        for (int i = 0; i < def.par.size(); i++) {
            String paramName = def.par.get(i).getText();
            Object value = actualArgs.get(i);
            
            // Объявляем аргументы как локальные переменные внутри нового скоупа
            scopes.declareSymbol(paramName, value);
        }
    }
}