        PUSH #2
        DUP
        POP [a]
        Terminal node:;
        PUSH [a]
        PUSH #3
        ADD
        Terminal node:;
        PUSH #L_TRUE_0  // 1. Кладем адрес. Стек: [L_TRUE]
        PUSH [a]
        PUSH #5
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