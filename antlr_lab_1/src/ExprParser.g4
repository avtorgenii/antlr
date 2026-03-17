parser grammar ExprParser;
options { tokenVocab=ExprLexer; }

program   // starting point
    : (stat | def)* EOF; // program can be composed of multiple statements and function definitions

type : INT_TYPE | FLOAT_TYPE | BOOL_TYPE | STRING_TYPE ;

// в StatContext создастся метод ID() для получения названия переменной если оно есть
stat: type ID ASSIGN expr SEMI                                                  #Declare         // statement could be variable declaration, ID - variable name, expr - maths or logical expression
    | expr SEMI                                                                 #Statement       // statement can also be simply calculation and not assignment
    | PRINT_kw expr SEMI                                                        #PrintStat
    | IF_kw '(' cond=expr ')' then=block (ELSE_kw elseBlock=block)?             #IfStat
    | RETURN_kw expr? SEMI                                                      #ReturnStat
    | FOR_kw '(' init=stat cond=expr SEMI step=expr ')' body=block              #ForStat
    ;

block: stat             #BlockSingle
    | '{' stat* '}'     #BlockReal
    ;

def : name=ID '(' par+=ID (',' par+=ID)* ')' '{' body+=stat* '}' ;  // function deinition and arguments, += means add, = would mean only the last stat

// в ExprContext будет пусто, так как тут есть лейблы (#BinOp и тд.), зато появяться новые контексты (ExprParser.BinOpContext и др.)
expr: ID '(' expr (',' expr)* ')'                       #FunCall
    | l=expr op=(MUL|DIV) r=expr                        #BinOp            // l, op и r появяться как полня контекста BinOpContext
    | l=expr op=(ADD|SUB) r=expr                        #BinOp
    | l=expr op=(EQUAL|NOTEQUAL|GT|LT|GE|LE) r=expr     #BinOp
    | INT                                               #IntLit
    | FLOAT                                             #FloatLit
    | STRING                                            #StringLit
    | BOOL                                              #BoolLit
    | ID                                                #Id
    | '(' expr ')'                                      #Parens
    | <assoc=right> ID ASSIGN expr                      #Assign           // Магия здесь: y = 5 теперь валидный expr
    ;
