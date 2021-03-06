package fr.rthd.jlc.env;

import fr.rthd.jlc.TypeCode;
import fr.rthd.jlc.env.exception.EnvException;
import fr.rthd.jlc.env.exception.SymbolAlreadyDefinedException;
import fr.rthd.jlc.env.exception.SymbolNotFoundException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Environment
 * @param <Value> Variable type
 * @param <Func> Function type
 * @author RomainTHD
 */
@NonNls
public class Env<Value, Func extends FunType, Class extends ClassType<?>> {
    /**
     * Global functions map
     */
    @NotNull
    private final Map<String, Func> _funcSignatures;

    /**
     * Class map
     */
    @NotNull
    private final Map<String, Class> _classSignatures;

    /**
     * Variable contexts
     */
    @NotNull
    private final LinkedList<Map<String, Value>> _contexts;

    /**
     * Class-wide function map
     */
    @NotNull
    private Map<String, Func> _classFuncSignatures;

    /**
     * Class we're currently in
     */
    @Nullable
    private Class _currentClass = null;

    /**
     * Empty constructor
     */
    public Env() {
        _funcSignatures = new HashMap<>();
        _classFuncSignatures = new HashMap<>();
        _classSignatures = new HashMap<>();
        _contexts = new LinkedList<>();
    }

    /**
     * Copy constructor, will copy the function signatures
     * @param baseEnv Parent environment
     */
    public Env(@NotNull Env<?, Func, Class> baseEnv) {
        _funcSignatures = baseEnv._funcSignatures;
        _classFuncSignatures = baseEnv._classFuncSignatures;
        _classSignatures = baseEnv._classSignatures;
        _contexts = new LinkedList<>();
        _contexts.push(new HashMap<>());
        _currentClass = null;
    }

    @Contract(pure = true)
    @NotNull
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("\n");
        for (String funcName : _funcSignatures.keySet()) {
            s.append(funcName).append(" ");
            s.append(lookupFun(funcName));
            s.append("\n");
        }

        for (String className : _classSignatures.keySet()) {
            s.append(className).append(" ");
            s.append(lookupClass(className));
            s.append("\n");
        }

        for (String funcName : _classFuncSignatures.keySet()) {
            s.append(funcName).append(" ");
            s.append(lookupFun(funcName));
            s.append("\n");
        }

        Iterator<Map<String, Value>> iter = _contexts.descendingIterator();
        while (iter.hasNext()) {
            Map<String, Value> ctx = iter.next();
            s.append("\n");
            for (String varName : ctx.keySet()) {
                s.append(varName).append(" ");
                s.append(lookupVar(varName));
                s.append("\n");
            }
        }

        return s.toString();
    }

    /**
     * @return Current class
     */
    @Contract(pure = true)
    @Nullable
    public Class getCurrentClass() {
        return _currentClass;
    }

    /**
     * Set the new current class
     * @param c Class to set
     */
    public void setCurrentClass(@Nullable Class c) {
        _currentClass = c;
    }

    /**
     * Lookup a variable
     * @param id Variable name
     * @return Variable or null if not found
     */
    @Contract(pure = true)
    @Nullable
    public Value lookupVar(String id) {
        for (Map<String, Value> env : _contexts) {
            Value v = env.get(id);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /**
     * Lookup a function
     * @param id Function name
     * @return Function or null if not found
     */
    @Contract(pure = true)
    @Nullable
    public Func lookupFun(String id) {
        Func f = _classFuncSignatures.get(id);
        if (f == null) {
            f = _funcSignatures.get(id);
        }
        return f;
    }

    /**
     * Lookup a class
     * @param id Class name
     * @return Class or null if not found
     */
    @Contract(pure = true)
    @Nullable
    public Class lookupClass(String id) {
        return _classSignatures.get(id);
    }

    /**
     * Lookup a class
     * @param t Class type
     * @return Class or null if not found
     */
    @Contract(pure = true)
    @Nullable
    public Class lookupClass(TypeCode t) {
        return lookupClass(t.getRealName());
    }

    /**
     * @return All functions
     */
    @Contract(pure = true)
    @NotNull
    public List<Func> getAllFun() {
        return new LinkedList<>(_funcSignatures.values());
    }

    /**
     * @return All class
     */
    @Contract(pure = true)
    @NotNull
    public List<Class> getAllClass() {
        return new LinkedList<>(_classSignatures.values());
    }

    /**
     * Insert a variable
     * @param id Variable name
     * @param value Variable
     * @throws EnvException If the variable is already set in the top-level
     *     context
     */
    public void insertVar(
        @NotNull String id,
        @NotNull Value value
    ) throws EnvException {
        insertVar(id, value, false);
    }

    /**
     * Insert a variable
     * @param id Variable name
     * @param value Variable
     * @param force Force insert or not
     * @throws EnvException If the variable is already set in the top-level
     *     context and `force` is false
     */
    public void insertVar(
        @NotNull String id,
        @NotNull Value value,
        boolean force
    ) throws EnvException {
        Map<String, Value> env = _contexts.peek();
        assert env != null;
        if (force || env.get(id) == null) {
            env.put(id, value);
        } else {
            throw new SymbolAlreadyDefinedException(id);
        }
    }

    /**
     * @param value Variable
     * @return Whether a variable is in the top-level context or not
     */
    @Contract(pure = true)
    public boolean isTopLevel(Value value) {
        Map<String, Value> env = _contexts.peek();
        assert env != null;
        return env.containsValue(value);
    }

    /**
     * Update a variable
     * @param id Variable name
     * @param value Variable
     */
    public void updateVar(@NotNull String id, @NotNull Value value) {
        for (Map<String, Value> env : _contexts) {
            Value v = env.get(id);
            if (v != null) {
                env.put(id, value);
                break;
            }
        }
    }

    /**
     * @return Class methods
     */
    @Contract(pure = true)
    @NotNull
    public Map<String, Func> getClassFunctions() {
        return _classFuncSignatures;
    }

    /**
     * Set the class methods
     * @param fns Class methods
     */
    public void setClassFunctions(@Nullable Map<String, Func> fns) {
        _classFuncSignatures = Objects.requireNonNullElseGet(
            fns,
            HashMap::new
        );
    }

    /**
     * Insert a function
     * @param func Function
     * @throws EnvException If the function is already defined
     */
    public void insertFun(@NotNull Func func) throws EnvException {
        if (lookupFun(func.getName()) == null) {
            _funcSignatures.put(func.getName(), func);
        } else {
            throw new SymbolAlreadyDefinedException(func.getName());
        }
    }

    /**
     * Insert a class
     * @param cls Class
     * @throws EnvException If the class is already defined
     */
    public void insertClass(@NotNull Class cls) throws EnvException {
        if (lookupClass(cls.getName()) == null) {
            _classSignatures.put(cls.getName(), cls);
        } else {
            throw new SymbolAlreadyDefinedException(cls.getName());
        }
    }

    /**
     * Enter a new scope
     */
    public void enterScope() {
        _contexts.push(new HashMap<>());
    }

    /**
     * Leave the scope
     */
    public void leaveScope() {
        _contexts.pop();
    }

    /**
     * Reset scope
     */
    public void resetScope() {
        _contexts.clear();
        _contexts.push(new HashMap<>());
    }

    /**
     * @return Scope depth
     */
    @Contract(pure = true)
    public int getScopeDepth() {
        return _contexts.size() - 1;
    }

    /**
     * Remove a function
     * @param name Function name
     * @throws EnvException If the function doesn't exist
     */
    public void removeFun(@NotNull String name) throws EnvException {
        if (lookupFun(name) == null) {
            throw new SymbolNotFoundException(name);
        } else {
            _funcSignatures.remove(name);
        }
    }

    /**
     * Remove a class
     * @param name Class name
     * @throws EnvException If the class doesn't exist
     */
    public void removeClass(@NotNull String name) throws EnvException {
        if (lookupClass(name) == null) {
            throw new SymbolNotFoundException(name);
        } else {
            _classSignatures.remove(name);
        }
    }
}
