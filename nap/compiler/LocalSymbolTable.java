// IMPORTS

// Class for LocalSymbolTable
// one for each block
// contains variable mappings
public class LocalSymbolTable
{
  public HashMap< String, Type > symbolTable;

  // constructor to create new local symbol table - initialize no values
    // Map< String, Type >
  public LocalSymbolTable( )
  {
    this.symbolTable = new HashMap<>();
  }

  // constructor to create local symbol table with first key value pair
  public LocalSymbolTable( String var, Type type )
  {
    HashMap< String, Type > symTable = new HashMap<>();
    symTable.put( var, type );
    this.symbolTable = symTable;
  }

  // method for updating symbol table with new binding
  public void addBinding( String var, Type type )
  {
    this.symbolTable.put( var, type );
  }

  // method for getting the type of a variable in a symbol table (lookup)
  public Type getType( String var )
  {
    return this.symbolTable.get( var );
  }
}
