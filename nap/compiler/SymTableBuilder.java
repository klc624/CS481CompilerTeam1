// IMPORTS
package compiler;

import ast.*;
import java.util.*;
import util.*;

// builds the symbol tables for a program
  // pretty sure this implements Visitor?
  // traverses an AST to build the symbol tables
  // should also check if undeclared names are used
public class SymTableBuilder implements ast.Visitor</*IDK RETURN TYPE */>
{
  GlobalSymbolTable globalSymTable;
  HashMap< Block, LocalSymbolTable > localSymTables;
  HierarchyMap blockHierarchy;
  Block currentBlock;

  public SymTableBuilder( )
  {
    globalSymTable = new GlobalSymbolTable();
    localSymTables = new HashMap<>();
    blockHierarchy = new HierarchyMap();
    currentBlock = null;
  }

  @Override
  public String visit( ExpBinop exp )
  {

  }

  @Override
  public String visit( ExpUnop unaryExpr )
  {

  }

  @Override
  public String visit( ExpInt num )
  {

  }

  @Override
  public String visit( ExpVar var )
  {

  }

@Override
  public String visit( ExpBool bool )
  {

  }

  @Override
  public String visit( ExpChar c )
  {

  }

  @Override
  public String visit( ExpString string )
  {

  }

  @Override
  public String visit( StmRead stm )
  {

  }

  @Override
  public String visit( StmIf stm )
  {

  }

  @Override
  public String visit( StmAssign stm )
  {

  }

  @Override
  public String visit( StmReturn stm )
  {

  }

  @Override
  public String visit( Type type )
  {

  }

  @Override
  public String visit( Block block )
  {

  }

  @Override
  public String visit( StmDecl decl )
  {

  }

  @Override
  public String visit( FunctionDefinition fun )
  {
    String funcName = fun.name;
    Signature sig;

    if( fun.returnType.isPresent() )
    {
      sig = new Signature( fun.arguments, fun.returnType );
    }
    else
    {
      sig = new Signature( fun.arguments );
    }

    globalSymTable.addBinding( funcName, sig );
    localSymTables.put( fun.body, new LocalSymbolTable() );

    // IDK

  }

  @Override
  public String visit( Program program )
  {

    for ( FunctionDefinition f : program.functions )
    {
      f.accept( this );
    }


    // IDK
  }

  @Override
  public String visit( StmFor forLoop )
  {

  }

  @Override
  public String visit( StmWhile stm )
  {

  }

  @Override
  public String visit( StmPrint stm )
  {

  }

  @Override
  public String visit( StmExp stm )
  {

  }

  @Override
  public String visit( ExpArrEnum exp )
  {

  }

  @Override
  public String visit( ExpArrAccess exp )
  {

  }

  @Override
  public String visit( ExpFuncCall exp )
  {

  }

  @Override
  public String visit( ExpPredefinedCall exp )
  {

  }

  @Override
  public String visit( ExpNew exp )
  {

  }

  @Override
  public String visit( ExpAssignop exp )
  {

  }
}
