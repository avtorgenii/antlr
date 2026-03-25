    DD a
    PUSH #2
    PUSH #3
    ADD
    DUP
    POP [a]
    DD b
    PUSH [a]
    PUSH #5
    DIV
    DUP
    POP [b]
    DD c
    PUSH [a]
    PUSH [b]
    MUL
    DUP
    POP [c]
    DD d
    PUSH [a]
    PUSH [c]
    SUB
    DUP
    POP [d]
    Terminal node:<EOF>