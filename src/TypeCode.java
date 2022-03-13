public enum TypeCode {
    CInt("int", 'I', 1),
    CDouble("double", 'D', 2),
    CBool("bool", 'I', 1),
    CVoid("void", 'V', 0);

    private final String _typename;
    private final char _shortType;
    private final int _size;

    TypeCode(String typename, char shortType, int size) {
        this._typename = typename;
        this._shortType = shortType;
        this._size = size;
    }

    public String toString() {
        return this._typename;
    }

    public char getShortType() {
        return this._shortType;
    }

    public int getSize() {
        return _size;
    }
}
