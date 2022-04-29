package fr.rthd.jlc.compiler.llvm;

import fr.rthd.jlc.AnnotatedExpr;
import fr.rthd.jlc.TypeCode;
import fr.rthd.jlc.compiler.Literal;
import fr.rthd.jlc.compiler.OperationItem;
import fr.rthd.jlc.compiler.Variable;
import fr.rthd.jlc.env.ClassType;
import fr.rthd.jlc.env.FunType;
import fr.rthd.jlc.internal.NotImplementedException;
import javalette.Absyn.EAdd;
import javalette.Absyn.EAnd;
import javalette.Absyn.EApp;
import javalette.Absyn.EDot;
import javalette.Absyn.ELitDoub;
import javalette.Absyn.ELitFalse;
import javalette.Absyn.ELitInt;
import javalette.Absyn.ELitTrue;
import javalette.Absyn.EMul;
import javalette.Absyn.ENew;
import javalette.Absyn.ENull;
import javalette.Absyn.EOr;
import javalette.Absyn.ERel;
import javalette.Absyn.ESelf;
import javalette.Absyn.EString;
import javalette.Absyn.EVar;
import javalette.Absyn.Expr;
import javalette.Absyn.ListExpr;
import javalette.Absyn.Neg;
import javalette.Absyn.Not;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static fr.rthd.jlc.TypeCode.CBool;
import static fr.rthd.jlc.TypeCode.CDouble;
import static fr.rthd.jlc.TypeCode.CInt;
import static fr.rthd.jlc.TypeCode.CString;
import static fr.rthd.jlc.TypeCode.CVoid;

class ExprVisitor implements Expr.Visitor<OperationItem, EnvCompiler> {
    /**
     * Used for ENew calls
     */
    @Nullable
    private final String _refVar;

    public ExprVisitor() {
        this(null);
    }

    public ExprVisitor(@Nullable String refVar) {
        _refVar = refVar;
    }

    public OperationItem visit(ENull p, EnvCompiler env) {
        throw new NotImplementedException();
    }

    public OperationItem visit(EVar p, EnvCompiler env) {
        Variable var = env.lookupVar(p.ident_);
        assert var != null;
        if (var.isPointer() && var.getType().isPrimitive()) {
            Variable tmp = env.createTempVar(var.getType(), String.format(
                "var_%s",
                var.getName().replace(EnvCompiler.SEP, '-')
            ));
            env.emit(env.instructionBuilder.load(tmp, var));
            return tmp;
        } else {
            return var;
        }
    }

    public OperationItem visit(ELitInt p, EnvCompiler env) {
        return new Literal(CInt, p.integer_);
    }

    public OperationItem visit(ELitDoub p, EnvCompiler env) {
        return new Literal(CDouble, p.double_);
    }

    public OperationItem visit(ELitTrue p, EnvCompiler env) {
        return new Literal(CBool, true);
    }

    public OperationItem visit(ELitFalse p, EnvCompiler env) {
        return new Literal(CBool, false);
    }

    public OperationItem visit(ESelf p, EnvCompiler env) {
        throw new NotImplementedException();
    }

    public OperationItem visit(EApp p, EnvCompiler env) {
        List<OperationItem> args = new ArrayList<>();

        for (Expr expr : p.listexpr_) {
            args.add(expr.accept(new ExprVisitor(), env));
        }

        FunType func = env.lookupFun(p.ident_);
        assert func != null;
        // `getName` should not be used here, because it contains the logical
        //  name of the function, not the name of the function in the LLVM IR

        if (func.getRetType() == CVoid) {
            env.emit(env.instructionBuilder.call(p.ident_, args));
            return null;
        } else {
            Variable out = env.createTempVar(
                func.getRetType(),
                "function_call"
            );
            env.emit(env.instructionBuilder.call(out, p.ident_, args));
            return out;
        }
    }

    public OperationItem visit(EString p, EnvCompiler env) {
        String content = p.string_;
        Variable global = env.createGlobalStringLiteral(content);

        if (env.lookupVar(global.getName()) == null) {
            // Avoid loading the same string literal multiple times
            env.insertVar(global.getName(), global);
            env.emitAtBeginning(env.instructionBuilder.globalStringLiteral(
                global,
                content
            ));
        }

        Variable tmp = env.createTempVar(
            CString,
            "string_literal"
        );
        env.emit(env.instructionBuilder.loadStringLiteral(tmp, global));
        return tmp;
    }

    public OperationItem visit(EDot p, EnvCompiler env) {
        TypeCode left;
        Expr e = ((AnnotatedExpr<?>) p.expr_).getParentExp();
        if (e instanceof EVar) {
            Variable v = env.lookupVar(((EVar) e).ident_);
            assert v != null;
            left = v.getType();
        } else {
            // FIXME: Avoid a double visit here, but not really portable
            throw new NotImplementedException(
                "Dot operator not implemented yet for non-variable expressions"
            );
        }

        ClassType c = env.lookupClass(left);
        assert c != null;

        ListExpr args = new ListExpr();
        args.add(p.expr_);

        // TODO: Add `this` and change the name of the function
        return new EApp(
            c.getAssemblyMethodName(p.ident_),
            args
        ).accept(new ExprVisitor(), env);
    }

    public OperationItem visit(ENew p, EnvCompiler env) {
        if (_refVar == null) {
            // FIXME: Create a new variable instead?
            throw new IllegalStateException(
                "`new` expressions can only be used with a underlying variable"
            );
        }

        Variable ref = env.lookupVar(_refVar);
        if (ref == null) {
            throw new IllegalStateException(
                String.format("Variable `%s` not found", _refVar)
            );
        }

        if (ref.getType().isPrimitive()) {
            throw new IllegalStateException(
                "Cannot create a new object for a primitive type"
            );
        }

        ClassType c = env.lookupClass(ref.getType());
        assert c != null;

        return new EDot(
            new AnnotatedExpr<>(ref.getType(), new EVar(_refVar)),
            c.getConstructorName(),
            new ListExpr()
        ).accept(new ExprVisitor(), env);
    }

    public OperationItem visit(Neg p, EnvCompiler env) {
        OperationItem expr = p.expr_.accept(new ExprVisitor(), env);
        if (expr instanceof Literal) {
            Literal lit = (Literal) expr;
            assert lit.getValue() != null;
            if (lit.getType() == CInt) {
                return new Literal(CInt, -(int) lit.getValue());
            } else if (lit.getType() == CDouble) {
                return new Literal(CDouble, -(double) lit.getValue());
            } else {
                throw new RuntimeException("Unsupported type for negation");
            }
        } else {
            Variable var = env.createTempVar(expr.getType(), "neg");
            env.emit(env.instructionBuilder.neg(var, (Variable) expr));
            return var;
        }
    }

    public OperationItem visit(Not p, EnvCompiler env) {
        OperationItem expr = p.expr_.accept(new ExprVisitor(), env);
        Variable var = env.createTempVar(expr.getType(), "not");
        env.emit(env.instructionBuilder.not(var, expr));
        return var;
    }

    public OperationItem visit(EMul p, EnvCompiler env) {
        return p.mulop_.accept(new MulOpVisitor(
            p.expr_1.accept(new ExprVisitor(), env),
            p.expr_2.accept(new ExprVisitor(), env)
        ), env);
    }

    public OperationItem visit(EAdd p, EnvCompiler env) {
        return p.addop_.accept(new AddOpVisitor(
            p.expr_1.accept(new ExprVisitor(), env),
            p.expr_2.accept(new ExprVisitor(), env)
        ), env);
    }

    public OperationItem visit(ERel p, EnvCompiler env) {
        return p.relop_.accept(new RelOpVisitor(
            p.expr_1.accept(new ExprVisitor(), env),
            p.expr_2.accept(new ExprVisitor(), env)
        ), env);
    }

    public OperationItem visit(EAnd p, EnvCompiler env) {
        Variable var = env.createTempVar(CBool, "and_ptr", true);
        env.emit(env.instructionBuilder.declare(var));

        String trueLabel = env.getNewLabel("and_true");
        String falseLabel = env.getNewLabel("and_false");
        String endLabel = env.getNewLabel("and_end");

        env.emit(env.instructionBuilder.comment("and"));
        env.indent();
        env.emit(env.instructionBuilder.comment("and left"));

        OperationItem left = p.expr_1.accept(new ExprVisitor(), env);
        env.emit(env.instructionBuilder.conditionalJump(
            left,
            trueLabel,
            falseLabel
        ));

        env.emit(env.instructionBuilder.label(trueLabel));
        env.enterScope();
        env.emit(env.instructionBuilder.comment("and true"));
        env.emit(env.instructionBuilder.store(
            var,
            p.expr_2.accept(new ExprVisitor(), env)
        ));
        env.emit(env.instructionBuilder.jump(endLabel));
        env.leaveScope();

        env.emit(env.instructionBuilder.label(falseLabel));
        env.emit(env.instructionBuilder.comment("and false"));
        env.emit(env.instructionBuilder.store(
            var,
            new Literal(CBool, false)
        ));
        env.emit(env.instructionBuilder.jump(endLabel));

        env.unindent();
        env.emit(env.instructionBuilder.label(endLabel));
        env.emit(env.instructionBuilder.comment("endand"));
        env.emit(env.instructionBuilder.newLine());

        Variable tmp = env.createTempVar(var.getType(), "and");
        env.emit(env.instructionBuilder.load(tmp, var));
        return tmp;
    }

    public OperationItem visit(EOr p, EnvCompiler env) {
        Variable var = env.createTempVar(CBool, "or_ptr", true);
        env.emit(env.instructionBuilder.declare(var));

        String trueLabel = env.getNewLabel("or_true");
        String falseLabel = env.getNewLabel("or_false");
        String endLabel = env.getNewLabel("or_end");

        env.emit(env.instructionBuilder.comment("or"));
        env.indent();
        env.emit(env.instructionBuilder.comment("or left"));

        OperationItem left = p.expr_1.accept(new ExprVisitor(), env);
        env.emit(env.instructionBuilder.conditionalJump(
            left,
            trueLabel,
            falseLabel
        ));

        env.emit(env.instructionBuilder.label(trueLabel));
        env.emit(env.instructionBuilder.comment("or true"));
        env.emit(env.instructionBuilder.store(
            var,
            new Literal(CBool, true)
        ));
        env.emit(env.instructionBuilder.jump(endLabel));

        env.emit(env.instructionBuilder.label(falseLabel));
        env.enterScope();
        env.emit(env.instructionBuilder.comment("or false"));
        env.emit(env.instructionBuilder.store(
            var,
            p.expr_2.accept(new ExprVisitor(), env)
        ));
        env.emit(env.instructionBuilder.jump(endLabel));
        env.leaveScope();

        env.unindent();
        env.emit(env.instructionBuilder.label(endLabel));
        env.emit(env.instructionBuilder.comment("endor"));
        env.emit(env.instructionBuilder.newLine());

        Variable tmp = env.createTempVar(var.getType(), "or");
        env.emit(env.instructionBuilder.load(tmp, var));
        return tmp;
    }
}
