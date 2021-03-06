package fr.rthd.jlc.compiler.llvm;

import fr.rthd.jlc.TypeCode;
import fr.rthd.jlc.Visitor;
import fr.rthd.jlc.compiler.OperationItem;
import fr.rthd.jlc.compiler.Variable;
import fr.rthd.jlc.env.ClassType;
import fr.rthd.jlc.env.Env;
import fr.rthd.jlc.env.FunType;
import javalette.Absyn.Prog;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Compiler
 * @author RomainTHD
 */
@NonNls
public class LLVMCompiler implements Visitor {
    /**
     * Output file path
     */
    @Nullable
    private final String _outputFilePath;

    /**
     * Constructor
     * @param outputFilePath Output file path
     */
    public LLVMCompiler(@Nullable String outputFilePath) {
        _outputFilePath = outputFilePath;
    }

    /**
     * Cast a variable to a specific type
     * @param dstType Destination type
     * @param value Value
     * @param env Environment
     * @return Cast variable
     */
    public static OperationItem castTo(
        TypeCode dstType,
        OperationItem value,
        EnvCompiler env
    ) {
        // Cast needed, for object types only, like `Object a = new Animal;`
        Variable tmp = env.createTempVar(
            dstType,
            "cast",
            value.getPointerLevel()
        );
        env.emit(env.instructionBuilder.cast(
            tmp,
            value,
            tmp.getType()
        ));

        return tmp;
    }

    /**
     * Entry point
     * @param p Program
     * @param parent Parent environment
     * @return Compiled program as a string
     */
    @NotNull
    @Override
    public Prog accept(
        @NotNull Prog p,
        @NotNull Env<?, FunType, ClassType<?>> parent
    ) {
        EnvCompiler env = new EnvCompiler(parent, new InstructionBuilder());
        p.accept(new ProgVisitor(), env);
        String asm = env.toAssembly();

        if (_outputFilePath == null) {
            // Default, print assembly to stdout
            System.out.println(asm);
        } else {
            // Flag `-o` was set, write to file
            try {
                FileWriter fw = new FileWriter(_outputFilePath);
                fw.write(asm);
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
                // TODO: Log error
            }
        }

        return p;
    }
}
