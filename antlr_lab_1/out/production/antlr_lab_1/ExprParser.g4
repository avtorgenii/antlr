parser grammar ExprParser;
options { tokenVocab=ExprLexer; }

program   // starting point
    : (stat | def)* EOF; // program can be composed of multiple statements and function definitions

type : INT_TYPE | FLOAT_TYPE | BOOL_TYPE | STRING_TYPE ;

// в StatContext создастся метод ID() для получения названия переменной если оно есть
stat: type ID EQ expr SEMI  #Declare            // statement could be variable declaration, ID - variable name, expr - maths or logical expression
    | expr SEMI  #Statement                     // statement can also be simply calculation and not assignment
    | IF_kw '(' cond=expr ')' then=block (ELSE_kw elseBlock=block)? #IfStat
    ;

block: stat #BlockSingle
    | '{' stat* '}' #BlockReal
    ;

def : ID '(' ID (',' ID)* ')' '{' stat* '}' ;  // function deinition and arguments

// в ExprContext будет пусто, так как тут есть лейблы (#BinOp и тд.), зато появяться новые контексты (ExprParser.BinOpContext и др.)
expr: <assoc=right> ID EQ expr                   #Assign           // Магия здесь: y = 5 теперь валидный expr
    | l=expr op=(MUL|DIV) r=expr                 #BinOp            // l, op и r появяться как полня контекста BinOpContext
    | l=expr op=(ADD|SUB) r=expr                 #BinOp
    | l=expr op=(EQUAL|NOTEQUAL|GT|LT) r=expr    #BinOp
    | INT                                        #IntLit
    | FLOAT                                      #FloatLit
    | STRING                                     #StringLit
    | BOOL                                       #BoolLit
    | ID                                         #Id
    | LPAREN expr RPAREN                         #Parens
    ;

func : ID '(' expr (',' expr)* ')' ;  // function call