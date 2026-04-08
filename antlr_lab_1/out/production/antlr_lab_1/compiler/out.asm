alham:
    DD _ret_alham
    DD _bp_alham
    POP A
    MOV [_ret_alham], A
    DD a
    DD b
    POP A
    MOV [b], A
    POP A
    MOV [a], A
    DD c
    MOV A, [b]
    PUSH A
    MOV A, [a]
    POP B
    ADD A, B
    MOV [c], A
    MOV A, [_ret_alham]
    PUSH A
    RET

start:
MOV A, #3
PUSH A
MOV A, #5
PUSH A
CALL alham