package ir.translation;

import ast.*;
import compiler.Signature;
import compiler.SymbolTable;
import compiler.TypeChecker;
import compiler.VisitedBlocks;
import ir.Frame;
import ir.Register;
import ir.com.*;
import ir.expr.Binary;
import ir.expr.ReadMem;
import ir.expr.ReadReg;
import ir.expr.Unary;
import type.Array;
import type.Basic;
import util.ErrorReporter;
import util.Pair;

import java.util.*;

public class Translate
{

  private static ErrorReporter<ir.translation.Status> errorReporter =
          new ErrorReporter<>("IR translation error");

  private static final TypeConverter typeConverter = new TypeConverter();

  /**
    * Convert an type.Type object to an ir.Type.
    *
    * @param type a type.Type object
    * @return an IR type i.e. BYTE, INT or ADDRESS.
  */
  private static ir.Type ofType( type.Type type )
  {
    return type.accept( typeConverter );
  }

  public static List<Pair<Frame, List<Command>>> run( SymbolTable symbolTable,
                                                      Program program )
  {
    TranslationVisitor translator = new TranslationVisitor( symbolTable );
    program.accept( translator );

    return translator.fragments;
  }

  /**
   * A visitor of type.Type, to convert such a type to an ir.Type.
   */
  private static class TypeConverter implements type.Visitor<ir.Type>
  {
    @Override
    public ir.Type visit(Array type)
    {
      return ir.Type.ADDRESS;
    }

    @Override
    public ir.Type visit(Basic.Int type)
    {
      return ir.Type.INT;
    }

    @Override
    public ir.Type visit(Basic.Bool type)
    {
      return ir.Type.BYTE;
    }

    @Override
    public ir.Type visit(Basic.Char type)
    {
      return ir.Type.BYTE;
    }

    @Override
    public ir.Type visit(Basic.Byte type)
    {
      return ir.Type.BYTE;
    }

    @Override
    public ir.Type visit(Basic.Float type)
    {
      errorReporter.report(Status.UNSUPPORTED, "float");
      return null;
    }
  }

  private static class TranslationVisitor implements ast.Visitor<ir.translation.Result>
  {
    private SymbolTable symbolTable;
    private TypeChecker typeChecker;

    private Map<String, Frame> frames;
    private Map<Pair<Block, String>, Register> varToReg;
    private List<Pair<Frame, List<ir.com.Command>>> fragments;
    private Frame currentFrame;

    public TranslationVisitor( SymbolTable symbolTable )
    {
      this.symbolTable = symbolTable;
      this.varToReg = new HashMap<>();
      this.typeChecker = new TypeChecker(symbolTable);
      this.fragments = new LinkedList<>();
      this.frames = new HashMap<>();
      this.currentFrame = null;
    }

    private static ir.expr.Unary.Operation ofOpUnary( OpUnary op )
    {
      if (op == OpUnary.NOT)
          return Unary.Operation.NOT;
      return Unary.Operation.SUB;
    }

    private static ir.expr.Unary.Operation ofPredefined( OpPredefined op )
    {
      switch (op)
      {
        case BYTE_OF_CHAR:
          return Unary.Operation.BYTE_OF_CHAR;
        case CHAR_OF_BYTE:
          return Unary.Operation.CHAR_OF_BYTE;
        case INT_OF_BYTE:
          return Unary.Operation.INT_OF_BYTE;
        case BYTE_OF_INT:
          return Unary.Operation.BYTE_OF_INT;
        case LENGTH:
          return Unary.Operation.LENGTH;
      }

      return Unary.Operation.LENGTH;
    }

    private Pair<Block, String> inCurrentBlock( String variable )
    {
      return new Pair<>(typeChecker.getVisitedBlocks().current(), variable);
    }

    @Override
    public Result visit(ExpBool exp)
    {
      return new Result(new ir.expr.Byte((byte) (exp.value ? 1 : 0)));
    }

    @Override
    public Result visit(ExpChar exp)
    {
      return new Result(new ir.expr.Byte((byte) exp.value));
    }

    @Override
    public Result visit(ExpInt exp)
    {
      return new Result(new ir.expr.Int(exp.value));
    }

    @Override
    public Result visit(ExpVar exp)
    {
      Register reg = null;
      for(Block block : typeChecker.getVisitedBlocks().getStack())
      {
        reg = varToReg.get(new Pair<>(block, exp.name));
        if (reg!= null)
        {
          return new Result(new ReadReg(reg));
        }
      }

      assert reg != null :
          "Internal Error: no register associated with " + exp.name;
      return null;
    }

    @Override
    public Result visit(ExpBinop exp)
    {
      Result left = exp.left.accept(this);
      Result right = exp.right.accept(this);
      ir.expr.Expression binary = new Binary(left.getExp(), right.getExp(), exp.op);
      List<ir.com.Command> code = new LinkedList<>();

      code.addAll(left.getCode());
      code.addAll(right.getCode());
      return new Result(binary, code);
    }

    @Override
    public Result visit(ExpUnop exp)
    {
      Result result = exp.accept(this);
      ir.expr.Unary.Operation op = ofOpUnary(exp.op);
      ir.expr.Expression newExp = new Unary(result.getExp(), op);

      return new Result(newExp, result.getCode());
    }

    @Override
    public Result visit(ExpAssignop exp)
    {
      errorReporter.report(Status.UNSUPPORTED, "++ and --");
      return null;
    }

    @Override
    public Result visit(ExpFuncCall exp)
    {
      List<ir.com.Command> code = new LinkedList<>();
      List<ir.expr.Expression> arguments = new LinkedList<>();

      for (Expression expression : exp.arguments)
      {
        Result result = expression.accept(this);
        arguments.add(result.getExp());
        code.addAll(result.getCode());
      }

      Frame frame = frames.get(exp.funcName);
      Optional<Signature> signature = symbolTable.funcLookup(exp.funcName);

      assert signature.isPresent() :
        "Internal Error: function name not in symbol table: " + exp.funcName;

      Optional<type.Type> returnType = signature.get().returnType;

      if (returnType.isPresent())
      {
        Register reg = new Register(ofType(returnType.get()));
        currentFrame.addLocal(reg);

        ir.com.Command call = new FunCall(reg, frame, arguments);
        code.add(call);

        return new Result(new ReadReg(reg), code);
      }

      code.add(new ProcCall(frame, arguments));

      return new Result(code);
    }

    @Override
    public Result visit(ExpArrAccess exp)
    {
      Optional<type.Type> typingResult = exp.accept(typeChecker);

      assert typingResult.isPresent() :
        "Internal Error: typing failed in " + exp;

      type.Type cellType = typingResult.get();
      Result arrayResult = exp.array.accept(this);
      Result indexResult = exp.index.accept(this);
      Register arrayRegister = new Register(ir.Type.ADDRESS);

      currentFrame.addLocal(arrayRegister);

      List<ir.com.Command> code = new LinkedList<>(indexResult.getCode());
      code.addAll(arrayResult.getCode());

      ir.expr.Expression index = indexResult.getExp();
      ir.com.Command arrayCom = new WriteReg( arrayRegister,
                                              arrayResult.getExp());
      code.add( arrayCom );

      ir.expr.Expression newExp = new ReadMem( arrayRegister,
                                               index,
                                               ofType(cellType));

      return new Result(newExp, code);
    }

    @Override
    public Result visit(StmAssign stm)
    {
      // StmAssign has passed the type checking, therefore the lValue field
      // contains either an ExpVar, or and ExpArrAccess expression
      assert ( stm.lValue instanceof ast.ExpVar
              || stm.lValue instanceof ast.ExpArrAccess ) :
              "Internal Error: lValue not ExpVar or ExpArrAccess in " + stm;

      Result resultExp = stm.exp.accept(this);
      List<ir.com.Command> code = new LinkedList<>(resultExp.getCode());

      if (stm.lValue instanceof ast.ExpVar)
      {
        ast.ExpVar expVar = (ast.ExpVar) stm.lValue;
        Register reg = varToReg.get(inCurrentBlock(expVar.name));
        code.add(new ir.com.WriteReg(reg, resultExp.getExp()));
      }

      if (stm.lValue instanceof ast.ExpArrAccess)
      {
        Result result = stm.lValue.accept(this);

        if (result.getExp() instanceof ReadMem)
        {
          code.addAll(result.getCode());
          ReadMem read = (ReadMem) result.getExp();
          Optional<type.Type> typingResult = stm.exp.accept(typeChecker);

          assert typingResult.isPresent() :
              "Internal Error: typing failed in " + stm.exp;

          ir.Type type = ofType(typingResult.get());
          ir.expr.Expression address = new Binary(
                                        new ReadReg( read.getRegister()),
                                        read.getOffset(),
                                        OpBinary.ADD );

          code.add(new ir.com.WriteMem(address, resultExp.getExp(), type));
        }
        else
        {
          assert false :
            "Internal error: ExpArrAccess not translated to ReadMem in "
            + stm.lValue;
        }

      }
      return new Result(code);
    }

    @Override
    public Result visit(StmExp stm)
    {
      return stm.exp.accept(this);
    }

    @Override
    public Result visit(StmReturn stm)
    {
      Result result = stm.exp.accept(this);

      assert currentFrame.getResult().isPresent() :
        "Internal Error: return statement in a function without a return type in "
        + stm;

      Register returnReg = currentFrame.getResult().get();
      Command writeReturnReg = new WriteReg(returnReg, result.getExp());
      Command gotoExitPoint = new Jump(currentFrame.getExitPoint());
      List<Command> code = new LinkedList<>(result.getCode());

      code.add(writeReturnReg);
      code.add(gotoExitPoint);

      return new Result(code);
    }

    @Override
    public Result visit(ExpPredefinedCall exp)
    {
      // should create frames for all predefined operations
      List<ir.com.Command> code = new LinkedList<>();
      List<ir.expr.Expression> arguments = new LinkedList<>();

      ir.expr.Unary.Operation funcName = ofPredefined( exp.funcName );

      Frame frame;
      Label entryLabel;
      Label exitLabel;
      type.Type returnType;

      Register reg;
      List<Register> params;
      List<Boolean> passByRef;
      int size; // IDK WHAT TO DO FOR SIZE

      for ( Expression e : exp.arguments )
      {
        Result result = e.accept( this );
        arguments.add( result.getExp() );
        code.addAll( result.getCode );
      }

      switch( funcName )
      {
        case Unary.Operation.LENGTH:
          returnType = BasicType.INT;
          entryLabel = new Label( "lengthEntry" );
          exitLabel = new Label( "lengthExit" );
          params.add( new Register( ir.Type.ADDRESS ) );
          break;

        case Unary.Operation.BYTE_OF_INT:
          returnTyper = BasicType.BYTE;
          entryLabel = new Label( "byteOfIntEntry" );
          exitLabel = new Label( "byteOfIntExit" );
          params.add( new Register( ir.Type.INT ) );
          break;

        case Unary.Operation.BYTE_OF_CHAR:
          returnType = BasicType.BYTE;
          entryLabel = new Label( "byteOfCharEntry" );
          exitLabel = new Label( "byteOfCharExit" );
          params.add( new Register( ir.Type.ADDRESS ) );
          break;

        case Unary.Operation.INT_OF_BYTE:
          returnType = BasicType.INT;
          entryLabel = new Label( "intOfByteEntry" );
          exitLabel = new Label( "intOfByteExit" );
          params.add( new Register( ir.Type.BYTE ) );
          break;

        case Unary.Operation.CHAR_OF_BYTE:
          returnType = BasicType.CHAR;
          entryLabel = new Label( "charOfByteEntry" );
          exitLabel = new Label( "charOfByteExit" );
          params.add( new Register( ir.Type.BYTE ) );
          break;
      }

      passByRef.add( false );
      reg = new Register( returnType );

      frame = new Frame( entryLabel, exitLabel , params, passByRef, reg );

      frames.put( funcName, frame );

      ir.com.Command call = new FuncCall( reg, frame, arguments );
      code.add( call );

      return new Result( new ReadReg( reg ), code );
    }

    @Override
    public Result visit(StmRead stm)
    {
      // TODO: read
      return null;
    }

    @Override
    public Result visit(StmPrint stm)
    {
      // TODO: print

      return null;
    }

    @Override
    public Result visit(ExpNew exp)
    {
      List<ir.com.Command> code = new LinkedList<>();
      List<ir.expr.Expression> arguments = new LinkedList<>();

      Frame frame;
      Label entryLabel = new Label( "newArrayEntry" );
      Label exitLabel = new Label( "newArrayExit" );
      type.Type returnType = ir.Type.ADDRESS;

      Register reg = new Register( returnType );

      List<Register> params;
      params.add( new Register( ir.Type.INT ) );
      params.add( new Register( ir.Type.INT ) );

      List<Boolean> passByRef;
      passByRef.add( false );
      passByRef.add( false );

      int size; // IDK WHAT TO DO FOR SIZE???  MAYBE...

      if( exp.type instanceof type.Type.INT
          || exp.Type instanceof type.Type.CHAR
          || exp.Type instanceof type.Type.BYTE )
      {
        // if type checked exp.exp should be a number...?
        size = 4*( (int) exp.exp.accept( this ) );
      }
      /*
      else
      {
        ???
      }
      */


      frame = new Frame( entryLabel, exitLabel, params, passByRef, reg, size );

      currentFrame.addLocal( reg );
      ir.com.Command call = new FuncCall( reg, frame, arguments );
      code.add( call );

      // IDK if this is right
      return new Result( new WriteMem( reg, ), code );
    }

    @Override
    public Result visit(ExpArrEnum array)
    {
      Frame frame =
      return null;
    }

    @Override
    public Result visit(ExpString exp)
    {
      // TODO: String literals
      return null;
    }

    @Override
    public Result visit(StmIf stm)
    {
      // TODO: Conditional statement
      return null;
    }


    @Override
    public Result visit(StmWhile stm)
    {
      // TODO: while loops
      return null;
    }


    @Override
    public Result visit(StmFor stm)
    {
      errorReporter.report(Status.UNSUPPORTED, "for loop");
      return null;
    }

    @Override
    public Result visit(StmDecl stm)
    {
      // Declaration of a new variable = creation of a fresh register
      Register reg = new Register(ofType(stm.binding.getSnd().type));

      varToReg.put(inCurrentBlock(stm.binding.getFst()), reg);
      currentFrame.addLocal(reg);

      // If there is an initialization, a write to the new register is needed
      List<ir.com.Command> code = new LinkedList<>();

      if (stm.initialization.isPresent())
      {
        Result result = stm.initialization.get().accept(this);
        code.addAll(result.getCode());
        code.add(new ir.com.WriteReg(reg, result.getExp()));
      }
      return new Result(code);
    }

    @Override
    public Result visit(Type type)
    {
      assert false : "Internal Error: Type node should not be visited";
      return null;
    }

    @Override
    public Result visit(Block block)
    {
      typeChecker.getVisitedBlocks().enter(block);
      List<Command> code = new LinkedList<>();

      for (Statement stm : block.statements)
      {
        Result result = stm.accept(this);
        code.addAll(result.getCode());
      }

      typeChecker.getVisitedBlocks().exit();
      return new Result(code);
    }

    @Override
    public Result visit(FunctionDefinition fun)
    {
      Frame frame = frames.get(fun.name);

      assert frame != null :
          "Internal Error: no frame for function " + fun.name;

      currentFrame = frame;
      Result result = fun.body.accept(this);

      // If result contains an expression part, it is just discarded
      fragments.add(new Pair<>(frame, result.getCode()));
      return null;
    }

    @Override
    public Result visit(Program program)
    {
      program.accept(typeChecker);
      FramesBuilder framesBuilder = new FramesBuilder();
      program.accept(framesBuilder);

      for (FunctionDefinition fun : program.functions)
      {
        fun.accept(this);
      }

      return null;
    }

    private class FramesBuilder extends ast.VisitorBase<Void>
    {
      @Override
      public Void visit(FunctionDefinition fun)
      {
        List<Register> parameters = new LinkedList<>();
        List<Boolean> passedByRef = new LinkedList<>();

        for (Pair<Pair<String, ast.Type>, Boolean> argument : fun.arguments)
        {
          ast.Type argType = argument.getFst().getSnd();
          String argName = argument.getFst().getFst();
          boolean byRef = argument.getSnd();
          Register register = new Register(ofType(argType.type));
          varToReg.put(new Pair<>(fun.body, argName), register);
          parameters.add(register);
          passedByRef.add(byRef);
        }

        Frame frame;
        if (fun.returnType.isPresent())
        {
          ir.Type type = ofType(fun.returnType.get().type);
          frame = new Frame( new Label(), new Label(),
                             parameters, passedByRef, new Register(type) );
        }

        else
        {
          frame = new Frame( new Label(), new Label(),
                             parameters, passedByRef );
        }

        frames.put(fun.name, frame);

        return null;
      }

      @Override
      public Void visit(Program program)
      {
        for (FunctionDefinition fun : program.functions)
        {
          fun.accept(this);
        }

        return null;
      }
    }
  }
}
