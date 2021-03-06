package fr.rthd.jlc;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Type code
 * @author RomainTHD
 */
@NonNls
public class TypeCode {
    /**
     * Integer type code
     */
    @NotNull
    public static final TypeCode CInt = TypeCode.fromPrimitive(
        "int",
        "i32",
        0,
        4
    );

    /**
     * Double type code
     */
    @NotNull
    public static final TypeCode CDouble = TypeCode.fromPrimitive(
        "double",
        "double",
        0.0,
        8
    );

    /**
     * Boolean type code
     */
    @NotNull
    public static final TypeCode CBool = TypeCode.fromPrimitive(
        "boolean",
        "i1",
        false,
        1
    );

    /**
     * Void type code
     */
    @NotNull
    public static final TypeCode CVoid = TypeCode.fromPrimitive(
        "void",
        "void"
    );

    /**
     * String type code
     */
    @NotNull
    public static final TypeCode CString = TypeCode.fromPrimitive(
        "string",
        "i8*" // TODO: Use pointer level
    );

    /**
     * Raw pointer type code
     */
    @NotNull
    public static final TypeCode CRawPointer = TypeCode.fromPrimitive(
        "raw_pointer",
        "i8*" // TODO: Use pointer level
    );

    /**
     * Pointer size
     */
    public final static int POINTER_SIZE = 8;

    /**
     * Type code pool, to avoid problems with class and array type comparison,
     * like `Dog == Dog` or `Dog[] == Dog[]`, because they might be different
     * instances.
     */
    @NotNull
    private static final Map<String, TypeCode> _pool = new HashMap<>();

    /**
     * Real name in source code
     */
    @NotNull
    private final String _realName;

    /**
     * Readable name in assembly, since some types can use different structures
     * under the hood
     */
    @NotNull
    private final String _readableAssemblyName;

    /**
     * Assembly name
     */
    @NotNull
    private final String _assemblyName;

    /**
     * Default value
     */
    @Nullable
    private final Object _defaultValue;

    /**
     * Will contain the size in bytes of the type for all types except for
     * arrays, where it will contain its dimension. This is why the method
     * `getSize()` needs to be used, even internally.
     * @see #getSize()
     */
    private final int _size;

    /**
     * Primitive type or not
     */
    private final boolean _isPrimitive;

    /**
     * Base type for arrays
     */
    @Nullable
    private final TypeCode _baseType;

    /**
     * Constructor
     * @param realName Real name in source code
     * @param readableAssemblyName Readable name in assembly
     * @param assemblyName Assembly name
     * @param defaultValue Default value
     * @param size Size
     * @param isPrimitive Primitive type or not
     * @param baseType Base type for arrays
     */
    private TypeCode(
        @NotNull String realName,
        @NotNull String readableAssemblyName,
        @NotNull String assemblyName,
        @Nullable Object defaultValue,
        int size,
        boolean isPrimitive,
        @Nullable TypeCode baseType
    ) {
        _realName = realName;
        _readableAssemblyName = readableAssemblyName;
        _assemblyName = assemblyName;
        _defaultValue = defaultValue;
        _size = size;
        _isPrimitive = isPrimitive;
        _baseType = baseType;
    }

    /**
     * Create a type code from a primitive type
     * @param realName Real name in source code
     * @param assemblyName Assembly name
     * @param defaultValue Default value
     * @param size Size
     * @return Type code
     */
    @NotNull
    private static TypeCode fromPrimitive(
        @NotNull String realName,
        @NotNull String assemblyName,
        @Nullable Object defaultValue,
        int size
    ) {
        return new TypeCode(
            realName,
            realName,
            assemblyName,
            defaultValue,
            size,
            true,
            null
        );
    }

    /**
     * Create a type code from a primitive type
     * @param realName Real name in source code
     * @param assemblyName Assembly name
     * @return Type code
     */
    @NotNull
    private static TypeCode fromPrimitive(
        @NotNull String realName,
        @NotNull String assemblyName
    ) {
        return fromPrimitive(
            realName,
            assemblyName,
            null,
            0
        );
    }

    /**
     * Create a type code for a class
     * @param realName Real class name in source code
     * @return Type code
     */
    @NotNull
    public static TypeCode forClass(@NotNull String realName) {
        TypeCode typeCode = _pool.get(realName);
        if (typeCode == null) {
            typeCode = new TypeCode(
                realName,
                realName,
                "%" + realName,
                null,
                0,
                false,
                null
            );
            _pool.put(realName, typeCode);
        }

        return typeCode;
    }

    /**
     * Create a type code for an array
     * @param baseType Array base type
     * @param dimension Array dimension (1D, 2D, ...)
     * @return Type code
     */
    @NotNull
    public static TypeCode forArray(@NotNull TypeCode baseType, int dimension) {
        if (dimension <= 0) {
            return baseType;
        }

        String realName = baseType._realName + "[]".repeat(dimension);
        TypeCode typeCode = _pool.get(realName);
        if (typeCode == null) {
            typeCode = new TypeCode(
                realName,
                baseType.getReadableAssemblyName() + "_" + dimension + "D",
                "%Array_" + baseType.getRealName() + "_" + dimension + "D",
                null,
                dimension,
                false,
                baseType
            );
            _pool.put(realName, typeCode);
        }

        return typeCode;
    }

    /**
     * Get all non-primitive types
     * @return All non-primitive types
     */
    @NotNull
    public static Collection<TypeCode> getAllComplexTypes() {
        return _pool.values();
    }

    @Contract(pure = true)
    @NotNull
    @Override
    public String toString() {
        return _assemblyName;
    }

    @Contract(pure = true)
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj instanceof TypeCode) {
            TypeCode other = (TypeCode) obj;
            return _realName.equals(other._realName);
        } else {
            return false;
        }
    }

    /**
     * @return Type size
     */
    @Contract(pure = true)
    public int getSize() {
        if (isArray()) {
            return CInt.getSize() + POINTER_SIZE; // Length + content fields
        } else {
            return _size;
        }
    }

    /**
     * @return Array dimension
     */
    @Contract(pure = true)
    public int getDimension() {
        if (isArray()) {
            return _size;
        } else {
            return 0;
        }
    }

    /**
     * @return Is a primitive type or not
     */
    @Contract(pure = true)
    public boolean isPrimitive() {
        return _isPrimitive;
    }

    /**
     * @return Array base type
     */
    @Contract(pure = true)
    @NotNull
    public TypeCode getBaseType() {
        if (isArray()) {
            assert _baseType != null;
            return _baseType;
        } else {
            return this;
        }
    }

    /**
     * @return Real name in source code
     */
    @Contract(pure = true)
    @NotNull
    public String getRealName() {
        return _realName;
    }

    /**
     * @return Real name in source code
     */
    @Contract(pure = true)
    @NotNull
    public String getReadableAssemblyName() {
        return _readableAssemblyName;
    }

    /**
     * @return Assembly name
     */
    @Contract(pure = true)
    @NotNull
    public String getAssemblyName() {
        return _assemblyName;
    }

    /**
     * @return Is an array type or not
     */
    @Contract(pure = true)
    public boolean isArray() {
        return _baseType != null;
    }

    /**
     * @return Is an object type or not
     */
    @Contract(pure = true)
    public boolean isObject() {
        return _baseType == null && !_isPrimitive;
    }

    /**
     * @return Default value
     */
    @Contract(pure = true)
    @Nullable
    public Object getDefaultValue() {
        return _defaultValue;
    }
}
