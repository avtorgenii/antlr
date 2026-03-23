MOV A, #2
MOV [a], A
Terminal node:;
MOV A, #3
PUSH A
MOV A, [a]
POP B
ADD A, B
Terminal node:;
MOV A, #5
PUSH A
MOV A, [a]
POP B
CMP A, B            // Сравниваем A и B
JG L_TRUE_0    // Если A > B, прыгаем на метку

MOV A, #0           // Иначе в A ложится 0 (Ложь)
JMP L_END_0    // Уходим в конец

L_TRUE_0:
MOV A, #1           // В A ложится 1 (Истина)
L_END_0:
Terminal node:;
Terminal node:<EOF>