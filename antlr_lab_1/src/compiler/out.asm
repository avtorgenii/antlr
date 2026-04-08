    alham:
        DD a
        DD b
        POP [b]
        POP [a]
        DD c
        PUSH [a]
        PUSH [b]
        ADD
        DUP
        POP [c]
        RET
        PUSH #3
        PUSH #5
        CALL alham
        Terminal node:;
    Terminal node:<EOF>