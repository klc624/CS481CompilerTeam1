grammar nap;

program : function_definition* EOF;

function_definition: FUNC Identifier LPAR parameters RPAR (ARROW returnType=type)? block;

parameter: REF? type Identifier ;

parameters: (parameter (COMMA parameter)*)? ;

block: LBLOCK statement* RBLOCK;

type: INT                      #TInt  
    | BOOL                     #TBool
    | CHAR                     #TChar
    | FLOAT                    #TFloat
    | BYTE                     #TByte
    | ARRAY LT type GT         #TArray
    ;

statement: declaration        #SDecl
         | instruction         #SIns
         ;

declaration: VAR type Identifier (ASSIGN expr)? ;

instruction:
      expr op=(ASSIGN|AEQ|MEQ|SEQ|DEQ) expr        #IAssign
    | FOR LPAR type Identifier IN expr RPAR block #IFor
    | WHILE LPAR expr RPAR block                  #IWhile
    | DO block WHILE LPAR expr RPAR               #IDoWhile
    | INPUT LPAR type COMMA expr RPAR             #IInput
    | PRINT LPAR type COMMA expr RPAR             #IPrint
    | IF LPAR expr RPAR block (ELSE block)?       #IIf
    | ARROW expr                                  #IReturn
    | expr                                        #IExpr
    ;

expr:
      expr LBRACKET expr RBRACKET                 #EArrayAccess
    | expr op=(MUL|DIV|MOD) expr                  #EMuls
    | expr op=(ADD|SUB) expr                      #EAdds
    | SUB expr                                    #EOpp
    | expr op=(EQ|NEQ|LT|LE|GT|GE) expr           #ECmp
    | expr AND expr                               #EAnd
    | expr OR  expr                               #EOr
    | NOT expr                                    #ENot
    | expr AssignOp                               #EPostfix
    | AssignOp expr                               #EPrefix
    | Identifier LPAR expressions RPAR            #ECall    
    | Identifier                                  #EIdentifier        
    | Int                                         #EInt
    | Bool                                        #EBool
    | Char                                        #EChar
    | String                                      #EString
    | NEW LPAR type COMMA expr RPAR               #ENew
    | LBLOCK expressions RBLOCK                   #EArrayEnumeration
    | LPAR expr RPAR                              #EPar    
    ;

expressions: (expr (COMMA expr)*)? ;

Bool: TRUE | FALSE;

Int: PInt | (SUB)PInt;

PInt: [0-9]+;

//ToDo: remove \ from the range
Char: QUOTE([\u0020-\u0026\u0028-\u007E]|Escape)QUOTE; 

//ToDo: remove \ from the range
String: SQUOTE([ !\u0023-\u007E]|Escape)*SQUOTE;
        
Escape: BACKSLASH('n'|'t'|QUOTE|SQUOTE|BACKSLASH|'0');
        
AssignOp: INCR | DECR;

// =====================
// Remove White Spaces
// =====================
WS: [ \r\n\t]+ -> skip;

// =====================
// Keyworkds
// =====================
TRUE: 'T';
FALSE: 'F';
FOR: 'for';
WHILE: 'while';
DO: 'do';
IF: 'if';
ELSE: 'else';
ARROW: '->';
IN: 'in';
AEQ: '+=';
SEQ: '-=';
MEQ: '*=';
DEQ: '/=';          
QUOTE: '\'';
SQUOTE: '"';
BACKSLASH: '\\';
LBLOCK : '{';
RBLOCK : '}';
LBRACKET: '[';
RBRACKET: ']';
INCR: '++';
DECR: '--';
ADD: '+';
AND: '&&';
OR: '||';
MUL: '*';
SUB: '-';
DIV: '/';
MOD: 'mod';
EQ: '==';
NEQ: '!=';
LT: '<';
GT: '>';
LE: '<=';
GE: '>=';
ASSIGN: '=';
NOT: '!';
ARRAY: 'array';
BOOL: 'bool';
BYTE: 'byte';
INT: 'int';
FLOAT: 'float';
CHAR: 'char';
FUNC: 'func';
LPAR: '(';
RPAR: ')';
COMMA: ',';
REF: 'ref';
VAR: 'var';
INPUT: 'read';
PRINT: 'print';
NEW: 'new';

// =====================
// Identifiers
// =====================
Identifier: [a-zA-Z_][a-zA-Z_0-9]* ;

// =========================
// Comments
// =========================
Comments: '#'(~'\n')* -> skip;
