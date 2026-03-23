package interpreter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class Scopes<T> {

	// Стек таблиц символов. Peek() всегда возвращает текущий (самый внутренний) уровень.
	private final Deque<Map<String, T>> memory = new ArrayDeque<>();

	public Scopes() {
		// Создаем глобальный уровень при инициализации
		memory.push(new HashMap<>());
	}

	/**
	 * Создает новый уровень видимости (например, при входе в { ... })
	 */
	public void enterScope() {
		memory.push(new HashMap<>());
	}

	/**
	 * Удаляет текущий уровень видимости при выходе из блока.
	 * Переменные этого уровня становятся недоступны для внешних уровней.
	 */
	public void leaveScope() {
		if (memory.size() <= 1)
			throw new RuntimeException("Cannot leave the global scope!");
		memory.pop();
	}

	/**
	 * Поиск таблицы, в которой объявлена переменная.
	 * Идет от текущего уровня к глобальному.
	 * Возвращает ПЕРВУЮ найденную таблицу (самую "близкую" к коду).
	 */
	private Map<String, T> findTableWithSymbol(String name) {
		for (Map<String, T> scope : memory) {
			if (scope.containsKey(name))
				return scope;
		}
		return null;
	}

	/**
	 * Создание НОВОЙ переменной в текущем (самом внутреннем) уровне.
	 * Это позволяет "затенять" переменные из внешних уровней.
	 */
	public void declareSymbol(String name, T value) {
		if (memory.peek().containsKey(name)) {
			throw new RuntimeException("Variable '" + name + "' already declared in this scope!");
		}
		memory.peek().put(name, value);
	}

	/**
	 * Обновление значения СУЩЕСТВУЮЩЕЙ переменной.
	 * Ищет переменную по всей иерархии (снизу вверх).
	 */
	public void setSymbol(String name, T value) {
		Map<String, T> table = findTableWithSymbol(name);
		if (table != null) {
			table.put(name, value);
		} else {
			throw new RuntimeException("Variable '" + name + "' not found! Declare it first.");
		}
	}

	/**
	 * Получение значения. Ищет от текущего уровня до глобального.
	 */
	public T getSymbol(String name) {
		Map<String, T> table = findTableWithSymbol(name);
		if (table != null) {
			return table.get(name);
		}
		throw new RuntimeException("Variable '" + name + "' is not defined!");
	}
}