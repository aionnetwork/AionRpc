package org.aion.api.prototype;

// == SomeStruct.java ==
public class SomeStruct {
    private byte[] MyData;
    private java.math.BigInteger MyQuantity;

    public SomeStruct(
        byte[] MyData,
        java.math.BigInteger MyQuantity
    ) {
        this.MyData = MyData;
        this.MyQuantity = MyQuantity;
    }

    public byte[] getMyData() {
        return this.MyData;
    }

    public java.math.BigInteger getMyQuantity() {
        return this.MyQuantity;
    }

}
