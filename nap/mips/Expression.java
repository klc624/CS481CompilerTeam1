package mips;

import ir.Register;
import ir.RegisterOffset;
import ir.Type;
import ir.expr.Byte;
import ir.expr.*;
import util.MakeList;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static mips.Asm.sizeOf;

public class Expression implements ir.expr.Visitor<List<String>>
{

  private Map<Register, Integer> regAddress;

  public Expression( Map<Register, Integer> regAddress )
  {
    this.regAddress = regAddress;
  }

  @Override
  public List<String> visit( ReadReg exp )
  {
    List<String> asmCode = new LinkedList<>();

    // get the offset
    int registerOffset = regAddress.get( exp.getRegister() );

    // load address in $t0
    asmCode.add( Asm.command( Asm.load( Program.DEFAULT_SIZE ) + " $t0, "
                                                               + registerOffset
                                                               + "($fp)"));
    // push the result onto the stack
    asmCode.addAll( Asm.push( "$t0" ) );

    return asmCode;
  }

  @Override
  public List<String> visit( ReadMem exp )
  {
    List<String> asmCode = new LinkedList<>();

    int registerOffset = regAddress.get( exp.getRegister() );

    // The address of the array is in $t0
    asmCode.add( Asm.command( Asm.load( Program.DEFAULT_SIZE ) + " $t0, "
                                                               + registerOffset
                                                               + "($fp)" ) );

    // offset is an integer value for the index in $t1
    asmCode.addAll( exp.getOffset().accept( this ) );

    // pop the index (NAP offset)
    asmCode.addAll( Asm.pop( "$t1" ) );

    // get the size for the type of memory read
    int size = sizeOf( exp.getType() );

    // load the size into $t3
    asmCode.add( Asm.command( "li $t3, " + size ) );

    // perform sz x n and store in $t2
    asmCode.add( Asm.command( "mul $t2, $t1, $t3" ) );

    // load the size of int into $t3
    asmCode.add( Asm.command( "li $t3, " + sizeOf( Type.INT ) ) );
    // perform 4 + (sz x n) and store the result in $t2
    asmCode.add( Asm.command( "add $t2, $t2, $t3" ) );

    // add $t0 to $t2
    asmCode.add( Asm.command( "add $t2, $t2, $t0" ) );

    // load based on the size
    asmCode.add( Asm.command( Asm.load( size ) + " $t3, ($t2)" ) );

    // push $t3 onto the stack
    asmCode.addAll( Asm.push( "$t3" ) );

    return asmCode;
  }

  @Override
  public List<String> visit( Byte exp )
  {
    List<String> asmCode = MakeList.one( Asm.command( "li $t0, "
                                                      + exp.getValue() ) );
    asmCode.addAll( Asm.push( "$t0" ) );
    return asmCode;
  }

  @Override
  public List<String> visit( Int exp )
  {
    List<String> asmCode = MakeList.one( Asm.command( "li $t0, "
                                                      + exp.getValue() ) );
    asmCode.addAll( Asm.push( "$t0" ) );
    return asmCode;
  }

  public List<String> memoryRW( RegisterOffset irObject, int size )
  {
    Register arrayRegister = irObject.getRegister();
    int registerOffset = regAddress.get( arrayRegister );

    // The offset is an expression: the value is pushed
    List<String> asmCode = irObject.getOffset().accept( this );

    // The offset is popped in $t2
    asmCode.addAll( Asm.pop( "$t2" ) );

    // The address of the array is in $t1
    asmCode.add( Asm.command( Asm.load( Program.DEFAULT_SIZE )
                              + " $t1, "
                              + registerOffset
                              + "($fp)" ) );

    asmCode.add( Asm.command( "li $t3, " + size ) );
    asmCode.add( Asm.command( "mul $t2, $t2, $t3" ) );
    asmCode.add( Asm.command( "add $t1, $t1, $t2" ) );
    asmCode.add( Asm.command( "li $t3, " + sizeOf( Type.INT ) ) );

    // The address of the cell is in $t1
    asmCode.add( Asm.command( "add $t1, $t1, $t3" ) );

    return asmCode;
  }


  @Override
  public List<String> visit( Unary exp )
  {
    List<String> code = exp.getExp().accept( this );
    code.addAll( Asm.pop( "$t0" ) );
    switch ( exp.getOp() )
    {
      case SUB:
          code.add( Asm.command( "subu $t0, $zero, $t0" ) );
          break;
      case NOT:
          code.add( Asm.command( "li $t1, 4294967294" ) );
          code.add( Asm.command( "nor $t0, $t1, $t0" ) );
    }

    code.addAll( Asm.push( "$t0" ) );
    return code;
  }

  @Override
  public List<String> visit( Binary exp )
  {
    List<String> left = exp.getLeft().accept( this );
    List<String> right = exp.getRight().accept( this );
    String op = null;

    switch ( exp.getOp() )
    {
      case ADD:
        op = "addu";
        break;

      case SUB:
        op = "subu";
        break;

      case DIV:
        op = "divu";
        break;

      case MUL:
        op = "mul";
        break;

      case MOD:
        op = "rem";
        break;

      case LT:
        op = "slt";
        break;

      case LE:
        op = "sle";
        break;

      case GT:
        op = "sgt";
        break;

      case GE:
        op = "sge";
        break;

      case AND:
        op = "and";
        break;

      case OR:
        op = "or";
        break;

      case EQ:
        op = "seq";
        break;

      case NEQ:
        op = "sne";
        break;
    }

    List<String> asmCode = left;
    asmCode.addAll( right );

    asmCode.addAll( Asm.pop( "$t2" ) );
    asmCode.addAll( Asm.pop( "$t1" ) );

    asmCode.add( Asm.command( op + " $t0, $t1, $t2" ) );
    asmCode.addAll( Asm.push( "$t0" ) );

    return asmCode;
  }

  @Override
  public List<String> visit( Symbol exp )
  {
    int size;

    if ( exp == Symbol.BYTE_SIZE )
    {
      size = 1;
    }

    else
    {
      size = 4;
    }

    List<String> asmCode = MakeList.one( Asm.command( "li $t0, " + size ) );
    asmCode.addAll( Asm.push( "$t0" ) );

    return asmCode;
  }
}
