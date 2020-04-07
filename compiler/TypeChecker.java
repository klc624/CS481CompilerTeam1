package compiler;

import ast.*;
import util.Pair;

import java.util.*;

public class TypeChecker extends ErrorList implements Visitor<Optional<type.Type>>
{
  private SymbolTable symbolTable;
  private VisitedBlocks visitedBlocks;

  public TypeChecker ( SymbolTable symbolTable ) {
    this.symbolTable = symbolTable;
    this.visitedBlocks = new VisitedBlocks();
  }


  @Override
  public Optional<type.Type> visit(ExpBool exp) {
    return Optional.of(type.Basic.BOOL);
  }

  @Override
  public Optional<type.Type> visit(ExpChar exp) {
    return Optional.of(type.Basic.CHAR);
  }

  @Override
  public Optional<type.Type> visit(ExpInt exp) {
    return Optional.of(type.Basic.INT);
  }

  @Override
  public Optional<type.Type> visit(ExpString exp) {
    return Optional.of(new type.Array( type.Basic.CHAR ));
  }

  @Override
  public Optional<type.Type> visit(ExpVar exp) {
    return symbolTable.varLookup( exp.name , visitedBlocks );
  }

  @Override
  public Optional<type.Type> visit(ExpBinop exp) {
    Optional<type.Type> type1 = exp.right.accept(this);
    Optional<type.Type> type2 = exp.left.accept(this);

    if( type1 != type2 ) {
      errors.add("Mismatched Types: " + type1
              + " and " + type2 + " at position " + exp.pos);
    }

    switch( exp.op ) {
      case LT:
      case GT:
      case LE:
      case GE:
      case EQ:
      case NEQ:
      case AND:
      case OR:
        return Optional.of( type.Basic.BOOL );
        break;
      default:
        return Optional.of( type.Basic.INT );
        break;
    }
  }

  @Override
  public Optional<type.Type> visit(ExpUnop exp) {
    Optional<type.Type> type = exp.exp.accept(this);

      switch( exp.op ) {
        case NOT:
          if( type != type.Basic.BOOL ) {
            errors.add( "Unary operation ! applied to " + type
                            + ", expected Boolean at position " + exp.pos );
          }
          break;
        case SUB:
          if( type != type.Basic.INT ) {
            errors.add( "Unary operation - applied to " + type
                            + ", expected Int at position " + exp.pos );
          }
          break;
      }

      return type;
  }

  @Override
  public Optional<type.Type> visit(ExpAssignop exp) {
    //TODO: Needs to be assignable

    Optional<type.Type> expType = exp.exp.accept(this);

    if( expType != type.Basic.INT ) {
      errors.add( exp.op + " applied to type " + expType +
                    " instead of type Int at position " + exp.pos );
    }

    return Optional.of( type.Basic.INT );
  }

  @Override
  public Optional<type.Type> visit(ExpFuncCall exp) {
    //TODO: Check argument is pass by reference than that expression must be assignable
    Signature mySig = symbolTable.funcLookup( exp.funcName );

    List<Optional<type.Type>> argTypes = new ArrayList<Optional<type.Type>>();

    for( Expression exp : exp.arguments ) {
      argTypes.add( exp.accept(this) );
    }

    if( !mySig.check( argTypes ) ) {
      errors.add( "Incorrect arg types for function "
                            + exp.funcName + " at position " + exp.pos );
    }

    return Optional.of( mySig.returnType );
  }

  @Override
  public Optional<type.Type> visit(ExpPredefinedCall exp) {
    Signature mySig = symbolTable.funcLookup( exp.funcName );

    List<Optional<type.Type>> argTypes = new ArrayList<Optional<type.Type>>();

    for( Expression exp : exp.arguments ) {
      argTypes.add( exp.accept(this) );
    }

    if( !mySig.check( argTypes ) ) {
      errors.add( "Incorrect arg types for function "
                            + exp.funcName + " at position " + exp.pos );
    }

    return Optional.of( mySig.returnType );
  }

  @Override
  public Optional<type.Type> visit(ExpNew exp) {
    if( exp.exp.accept(this) != type.BasicType.INT ) {
      errors.add( "Expected arg type Int, got " +
              exp.exp.accept(this) + " at position " + exp.pos );
    }

    return Optional.of( exp.type.type );
  }

  @Override
  public Optional<type.Type> visit(ExpArrAccess array) {
    Optional<type.Type> arrayType = array.array.accept( this );
    Optional<type.Type> indexType = array.index.accept( this );

    if( indexType != type.Basic.INT ) {
      errors.add( "Array index requires type Int, got type "
                          + indexType + " at position " + array.pos );
    }

    if( !(arrayType instanceof type.Array) ) {
      errors.add( "Cannot access variable of type " + arrayType
                + " by index, expected type Array at position " + array.pos );

      return arrayType;
    }

    return Optional.of( arrayType.type );
  }

  @Override
  public Optional<type.Type> visit(ExpArrEnum array) {
    assert(0 < exps.size()) :
                "Array enumerations of length 0 are not supported yet "
                + array.pos;

    Optional<type.Type> firstType = array.exps.get(0).accept(this);

    for( int index = 1; index < array.exps.size(); index++ ) {
      if( exps.get( index ).accept(this) != firstType ) {
        errors.add( "Expected type " + firstType + ", got type "
            + exps.get( index ).accept(this) + " at position "
             + exps.get( index ).pos );
      }
    }

    return Optional.of( firstType );
  }

  @Override
  public Optional<type.Type> visit(StmIf stm) {
    if( stm.condition.accept( this ) != type.BasicType.BOOL ) {
      errors.add( "Expected expression of type Boolean, got type "
            + stm.condition.accept(this) + " at position " + stm.condition.pos );
    }

    stm.then_block.accept( this );

    if( stm.else_block.isPresent() ) {
      stm.else_block.accept( this );
    }

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmAssign stm) {
    Optional<type.Type> varType = stm.lvalue.accept( this );
    Optional<type.Type> expType = stm.exp.accept( this );

    if( varType != expType ) {
      errors.add( "Assigning type " + expType +
                  " to type " + varType + "at position " + stm.exp.pos );
    }

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmExp stm) {
    return stm.exp.accept( this );
  }

  @Override
  public Optional<type.Type> visit(StmRead stm) {
    //TODO: Check if variable is assignable

    if( stm.type != stm.exp.accept( this ) ) {
      errors.add( "Input type " + stm.type + " not compatible with "
                + stm.exp.accept( this ) + " at position " + stm.exp.pos );
    }

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmPrint stm) {
    if( stm.type != stm.exp.accept( this ) ) {
      errors.add( "Print type " + stm.type + " not compatible with "
                + stm.exp.accept( this ) + " at position " + stm.exp.pos );
    }

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmReturn stm) {
    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmWhile stm) {
    if( stm.condition.accept( this ) != type.BasicType.BOOL ) {
      errors.add( "Expected type Boolean, got type " +
          stm.condition.accept( this ) + " at position " + stm.condition.pos );
    }

    stm.body.accept( this );

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmFor stm) {
    Optional<type.Type> arrayType = stm.collection.accept( this );

    if( !( arrayType instanceof type.Array) ) {
      errors.add( "For loop expected type Array, got "
                  + arrayType + " at position " + stm.collection.pos );
    } else if ( arrayType.type.accept( this ) != stm.type.accept( this ) ) {
      errors.add( "Iterating over type " + arrayType.type.accept( this )
        + " with index type " + stm.type.accept( this )
          + " at position " + stm.type.pos );
    }

    stm.body.accept( this );

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmDecl stm) {
    stm.initialization.ifPresent( exp -> {
      Optional<type.Type> expType = exp.accept( this );

      if( expType != stm.binding.getSnd().accept( this ) ) {
        errors.add( "Assinging type " + expType + " to type "
            + stm.binding.getSnd().accept( this ) + " at position " + stm.pos );
      }
    });

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(Type type) {
    return Optional.of( type.type );
  }

  @Override
  public Optional<type.Type> visit(Block block) {
    visitedBlocks.enter(block);
    for(Statement stm: block.statements)
        stm.accept(this);
    visitedBlocks.exit();

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(FunctionDefinition fun) {
    fun.body.accept(this);

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(Program program) {
    for( FunctionDefinition fun: program.functions  ) {
      fun.accept(this);
    }

    return Optional.empty();
  }
}
