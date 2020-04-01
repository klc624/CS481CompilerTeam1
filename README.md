# CS481CompilerTeam1

# Updates to nap as of 3/31/20

# nap > Added type folder
# nap > Added util folder for Pair()

# nap > nap.g4
# Char, String - removed \ from range for TODO 
  # TO DO: Needs to be double checked

# nap > ast
  # copied new ast functions from assignment 5 keys
  
  # Updates:
  # removed ExpLength and ExpConversionCall
  # added ExpPredefinedCall
  # removed TypBasic and TypArray, updated Type ( see nap > type )
  # changed order of some classes constructor parameters

# compiler > Main.java
  # updated to assignment 5 key

# compiler > BuildAst.java
  # Q: where is getText()? 
  # Q: where is getChildCount()?
  # Q: Does it need to use linked list()?

  # Added:
  # private opBinaryOfAssign()
  # private isPredefined()
  # private Map<String, OpPredefined>final predefined
  # private buildPredefined()
  # private expList()
  # private Ast assignmentOperator()

  # Updated Functions:

  # visitProgram(): fixed for loop according to assignment 5 key

  # visitFunction_definition(): fixed arguments.add for type according to assignment 5 key; added conditional for return type

  # visitSDecl() & visitSIns: changed visit( ctx.x() ) to ctx.x.accept(this)
  # * not sure what the difference is or if changing this matters

  # visitDeclaration(): changed visit() to .accept(this), added variables for readability

  # visitIFor(), visitIWhile(), visitIDoWhile(), visitIInput(), visitIPrint(), visitIIf(): added variables for readability

  # visitAssign(): updated according to homework 5 key for Ast class changes in StmAssign and updates to nap.g4

  # visitENew(): added variables for readability, changed type from (TypBasic) to (Type)

  # visitEArrayEnumeration(): changed according to assignment 5 key to get expression list using private function expList()

  # visitECall(): updated to handle predefined functions for added Ast class ExpPredefinedCall; changed according to assignment 5 key to get expression list using private function expList()

  # visitEPrefix() & visitEPostfix(): updated to use new function assignmentOperator()

  # visitECmp(), visitEOr(), visitEAnd(), visitEAdds(): updated to use new function binary()

  # visitEString(), visitEChar(): updated to use new function escaping()

  # visitEBool(): corrected according to assignment 5 key

  # visitTArray(): changed type from visit() to .accept(this); corrected return statement

  # visitTInt(), visitTByte(), visitTFloat(), visitTChar(), visitTBool(): corrected return statement

# compiler > PrettyPrinter.java
  # Q: what is StringBuilder()?

  # Added functions:
  # private deEscaping()
  # private parenthesis()
  # private buildCall()

  # removed ExpCallConversion, ExpLength to match ast class changes

  # added ExpPredefinedCall
  
  # Updated visitor functions:

  # ExpBinop, ExpUnop: added use of parenthesis()

  # ExpChar: added use of deEscaping(); added single quotes

  # ExpString: added use of deEscaping()

  # Block: corrected according to assignment 5 key

  # StmDecl: corrected return statement

  # FunctionDefinition: corrected according to assignment 5 key

  # Program: corrected according to assignment 5 key

  # StmFor: corrected spaces() order

  # StmWhile: added use of parenthesis(), added missing end block curly brace

  # ExpFuncCall: updated to use buildCall()
