// IMPORTS


// Class for global symbol table
// one single one for an entire program
// maps function names to signatures
public class GlobalSymbolTable
{
  public HashMap< String, Signature > symbolTable;

  // constructor for creating a new global symbol table - initialize no values
    // Map< String, Signature >
  public GlobalSymbolTable()
  {
    this.symbolTable = new HashMap<>();
  }

  // constructor for creating a global symbol table with first key value pair
  public GlobalSymbolTable( String funcName, Signature sig )
  {
    HashMap< String, Signature > symTable = new HashMap<>();
    symTable.put( funcName, sig );
    this.symbolTable = symTable;
  }

  // method for updating the global table with a new function binding
  public void addBinding( String funcName, Signature sig )
  {
    this.symbolTable.put( funcName, sig );
  }

  // method for getting a signature given a function name
  public Signature getSignature( String funcName )
  {
    return this.symbolTable.get( funcName );
  }
}
