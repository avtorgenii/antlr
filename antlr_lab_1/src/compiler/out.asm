    DD aa
    PUSH #2
    DUP
    POP [a]
        PUSH #L_TRUE_0  // 1. Кладем номер строки. Стек: [L_TRUE]
        PUSH [a]
        PUSH #2
        SUB        // 2. Вычисляем. Команды sub положат результат СВЕРХУ. Стек: [L_TRUE, результат]
        JG                   // 3. JG забирает результат (верхний), потом L_TRUE (под ним)

        PUSH #0
        PUSH #L_END_0
        JMP

        L_TRUE_0:
        PUSH #1
        L_END_0:
        Terminal node:;
    Terminal node:<EOF>