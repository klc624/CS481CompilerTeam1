package compiler;

import ast.*;
import util.Pair;

import java.util.*;

public class TypeChecker extends ErrorList implements Visitor<Optional<type.Type>>
{
  private SymbolTable symbolTable;
  private VisitedBlocks visitedBlocks;
  private String currentFuncName;

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
    type.Type opType;

    switch( exp.op ) {
      case EQ:
      case NEQ:
        if( !type1.equals( type2 ) ) {
          errors.add("Operator " + exp.op + " expects matched types, got " + type1.get()
                  + " and " + type2.get() + " at position " + exp.pos);
        }
        break;
      case AND:
      case OR:
        if( !type1.equals( type2 ) || !type1.get().equals( type.Basic.BOOL ) ) {
          errors.add("Operator " + exp.op + " expects ( bool, bool ), got ( " + type1.get()
                + ", " + type2.get() + " ) at position " + exp.pos);
        }
        break;
      default:
        if( !type1.equals( type2 ) || !type1.get().equals( type.Basic.INT ) ) {
          errors.add("Operator " + exp.op + " expects ( int, int ), got ( " + type1.get()
                + ", " + type2.get() + " ) at position " + exp.pos);
        }
        break;
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
      default:
        return Optional.of( type.Basic.INT );
    }
  }

  @Override
  public Optional<type.Type> visit(ExpUnop exp) {
    Optional<type.Type> myType = exp.exp.accept(this);

      switch( exp.op ) {
        case NOT:
          if( !myType.get().equals( type.Basic.BOOL ) ) {
            errors.add( "Unary operation ! applied to " + myType.get()
                            + ", expected bool at position " + exp.pos );
          }
          break;
        case SUB:
          if( !myType.get().equals( type.Basic.INT ) ) {
            errors.add( "Unary operation - applied to " + myType.get()
                            + ", expected int at position " + exp.pos );
          }
          break;
      }

      return myType;
  }

  @Override
  public Optional<type.Type> visit(ExpAssignop exp) {
    if( !(exp.exp instanceof ExpVar) && !(exp.exp instanceof ExpArrAccess) ) {
      errors.add( "Expression not assignable at position " + exp.exp.pos );
    }

    Optional<type.Type> expType = exp.exp.accept(this);

    if( !expType.get().equals( type.Basic.INT ) ) {
      errors.add( exp.op + " applied to type " + expType.get() +
                    " instead of type int at position " + exp.pos );
    }

    return Optional.of( type.Basic.INT );
  }

  @Override
  public Optional<type.Type> visit(ExpFuncCall exp) {
    Optional<Signature> optional = symbolTable.funcLookup( exp.funcName );

    List<type.Type> argTypes = new ArrayList<type.Type>();

    for( Expression currentExp : exp.arguments ) {
      if( currentExp.accept(this).isPresent() ) {
        argTypes.add( currentExp.accept(this).get() );
      }
    }

    optional.ifPresent( mySig -> {
      if( !mySig.check( argTypes ) ) {
        errors.add( "Incorrect arg types for function "
                            + exp.funcName + " at position " + exp.pos );
      }
    });

    if( optional.isPresent() ) {
      Optional<type.Type> retType = optional.get().returnType;

      if( retType.isPresent() ) {
        return Optional.of( retType.get() );
      }
    }

    if( optional.isPresent() ) {
      for( int index = 0; index < optional.get().argTypes.size(); index ++ ) {
        if ( optional.get().argTypes.get( index ).getSnd() ) {
          if( !(exp.arguments.get( index ) instanceof ExpVar) &&
                  !(exp.arguments.get( index ) instanceof ExpArrAccess) ) {
            errors.add( "Pass by reference parameter must be assignable at position " + exp.pos );
          }
        }
      }
    }

    return Optional.empty();

  }

  @Override
  public Optional<type.Type> visit(ExpPredefinedCall exp) {
    Signature predefined = Signatures.predefined.get( exp.funcName );

    List<type.Type> argTypes = new ArrayList<type.Type>();

    for( Expression currentExp : exp.arguments ) {
      Optional<type.Type> currentType = currentExp.accept( this );
      if( currentType.isPresent() ) {
        argTypes.add( currentType.get() );
      }
    }


    if( predefined != null && !predefined.check( argTypes ) ) {
      errors.add( "Incorrect arg types for function "
                            + exp.funcName + " at position " + exp.pos );
    }

    if( predefined != null ) {
      return predefined.returnType;
    }

    return Optional.of( type.Basic.INT );

  }

  @Override
  public Optional<type.Type> visit(ExpNew exp) {
    if( !exp.exp.accept(this).get().equals( type.Basic.INT ) ) {
      errors.add( "Expected arg type int, got " +
              exp.exp.accept(this).get() + " at position " + exp.pos );
    }

    return Optional.of( new type.Array( exp.type.type ));
  }

  @Override
  public Optional<type.Type> visit(ExpArrAccess array) {
    Optional<type.Type> arrayType = array.array.accept( this );
    Optional<type.Type> indexType = array.index.accept( this );

    if( !indexType.get().equals( type.Basic.INT ) ) {
      errors.add( "Array index requires type int, got type "
                          + indexType.get() + " at position " + array.pos );
    }

    if( !(arrayType.get() instanceof type.Array) ) {
      errors.add( "Cannot access variable of type " + arrayType.get()
                + " by index, expected type array at position " + array.pos );

      return arrayType;
    }

    return Optional.of( ((type.Array)arrayType.get()).type );
  }

  @Override
  public Optional<type.Type> visit(ExpArrEnum array) {
    assert(0 < array.exps.size()) :
                "Array enumerations of length 0 are not supported yet "
                + array.pos;

    Optional<type.Type> firstType = array.exps.get(0).accept(this);

    for( int index = 1; index < array.exps.size(); index++ ) {
      if( !array.exps.get( index ).accept(this).equals( firstType ) ) {
        errors.add( "Expected type " + firstType.get() + ", got type "
            + array.exps.get( index ).accept(this).get() + " at position "
             + array.exps.get( index ).pos );
      }
    }

    return Optional.of( new type.Array( firstType.get() ) );
  }

  @Override
  public Optional<type.Type> visit(StmIf stm) {
    if( !stm.condition.accept( this ).get().equals( type.Basic.BOOL ) ) {
      errors.add( "Expected expression of type bool, got type "
            + stm.condition.accept(this).get() + " at position " + stm.condition.pos );
    }

    stm.then_branch.accept( this );

    if( stm.else_branch.isPresent() ) {
      stm.else_branch.get().accept( this );
    }

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmAssign stm) {
    Optional<type.Type> varType = stm.lValue.accept( this );
    Optional<type.Type> expType = stm.exp.accept( this );

    if( !(stm.lValue instanceof ExpVar) && !(stm.lValue instanceof ExpArrAccess) ) {
      errors.add( "Expression not assignable at position " + stm.exp.pos );
    }

    if( varType.isPresent() && expType.isPresent() ) {
      if( !varType.equals( expType ) ) {
        errors.add( "Assigning type " + expType.get() +
                    " to type " + varType.get() + " at position " + stm.exp.pos );
      }

      if( stm.op.isPresent() &&
        ( !varType.get().equals( type.Basic.INT ) || !expType.get().equals( type.Basic.INT ) ) ) {
        errors.add( "Assignment operator expected expressions of type ( int, int ), got ( " + expType.get() +
                    ", " + varType.get() + " ) at position " + stm.exp.pos );
      }
    } else {
      errors.add( "Undefined types at position " + stm.exp.pos );
    }

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmExp stm) {
    return stm.exp.accept( this );
  }

  @Override
  public Optional<type.Type> visit(StmRead stm) {
    if( !(stm.exp instanceof ExpVar) && !(stm.exp instanceof ExpArrAccess) ) {
      errors.add( "Expression not assignable at position " + stm.exp.pos );
    }

    if( !stm.type.type.equals( stm.exp.accept( this ).get() ) ) {
      errors.add( "Input type " + stm.type.type + " not compatible with "
                + stm.exp.accept( this ).get() + " at position " + stm.exp.pos );
    }

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmPrint stm) {
    if( !stm.type.type.equals( stm.exp.accept( this ).get() ) ) {
      errors.add( "Print type " + stm.type.type + " not compatible with "
                + stm.exp.accept( this ).get() + " at position " + stm.exp.pos );
    }

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmReturn stm) {
    Optional<Signature> funcSig = symbolTable.funcLookup( currentFuncName );
    Optional<type.Type> retType = stm.exp.accept( this );

    funcSig.ifPresent( sig -> {
        if( !retType.equals(sig.returnType) ) {
          errors.add( "Incorrect return type, expected " + sig.returnType.get()
                + " got " + retType.get() + " at position " + stm.exp.pos );
        }});
    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmWhile stm) {
    if( !stm.condition.accept( this ).get().equals( type.Basic.BOOL ) ) {
      errors.add( "Expected type bool, got type " +
          stm.condition.accept( this ).get() + " at position " + stm.condition.pos );
    }

    stm.body.accept( this );

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmFor stm) {
    Optional<type.Type> arrayType = stm.collection.accept( this );

    if( !( arrayType.get() instanceof type.Array) ) {
      errors.add( "For loop expected type array, got "
                  + arrayType.get() + " at position " + stm.collection.pos );
    } else if ( !((type.Array)arrayType.get()).type.equals(stm.type.accept( this ).get() ) ) {
      errors.add( "Iterating over type " + ((type.Array)arrayType.get()).type
        + " with index type " + stm.type.accept( this ).get()
          + " at position " + stm.type.pos );
    }

    stm.body.accept( this );

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(StmDecl stm) {
    stm.initialization.ifPresent( exp -> {
      Optional<type.Type> expType = exp.accept( this );

      if( !expType.equals(stm.binding.getSnd().accept( this ) ) ) {
        errors.add( "Assinging type " + expType.get() + " to type "
            + stm.binding.getSnd().accept( this ).get() + " at position " + stm.pos );
      }
    });

    return Optional.empty();
  }

  @Override
  public Optional<type.Type> visit(ast.Type type) {
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
    currentFuncName = fun.name;

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
