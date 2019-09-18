package org.expand.dict;

/**
 * 词的类型枚举定义。
 */
public enum WordType {

    MainWord(1), 
    StopWord(2), 
    SurnameWord(3), 
    QuantifierWord(4), 
    SuffixWord(5), 
    PrepositionWord(6);

    WordType(int code) {
        this.code = code;
    }

    private int code;

    public int getCode() {
        return this.code;
    }

    public static WordType fromCode(int code) {
        for (WordType wordType : WordType.values()) {
            if (code == wordType.code) {
                return wordType;
            }
        }

        return null;
    }
}
