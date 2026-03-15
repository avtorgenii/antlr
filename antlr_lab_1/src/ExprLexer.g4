lexer grammar ExprLexer;

// Ключевые слова (Типы)
INT_TYPE    : 'int' ;
FLOAT_TYPE  : 'float' ;
BOOL_TYPE   : 'bool' ;
STRING_TYPE : 'string' ;

// --- Логические операторы ---
AND : 'and' ;
OR  : 'or' ;
NOT : 'not' ;

// --- Операторы сравнения ---
EQUAL    : '==' ;
NOTEQUAL : '!=' ;
GT       : '>' ;
LT       : '<' ;

// --- Арифметика ---
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;

// --- Служебные символы ---
EQ : '=' ;
COMMA : ',' ;
SEMI : ';' ;
LPAREN : '(' ;
RPAREN : ')' ;
LCURLY : '{' ;
RCURLY : '}' ;
IF_kw  : 'if';
ELSE_kw : 'else' ;

// Литералы (Значения)
FLOAT  : [0-9]+ '.' [0-9]* | '.' [0-9]+ ;
STRING : '"' .*? '"' ;
// Для простоты пока используем обычные true/false
BOOL   : 'true' | 'false' ;
INT    : [0-9]+ ; // ingerers
ID     : [a-zA-Z_][a-zA-Z_0-9]* ; // variable or function names
WS     : [ \t\n\r\f]+ -> skip ;  // spaces, tabs and переносы строк - skip = Выбросить токен в корзину. Парсер его никогда не увидит, он исчезнет навсегда.
Other  : . ;  // catches all other symbols
COMMENT : '/*' .*? '*/' -> channel(HIDDEN) ; // Отправить токен в «скрытый канал».
LINE_COMMENT : '//' ~'\n'* '\n' -> channel(HIDDEN) ; // Отправить токен в «скрытый канал».