package com.vidyo.vidyoconnector.event;

import androidx.annotation.NonNull;

public class BusBase<T, Call extends CallBase> {

    private final T[] value;
    private final Call call;

    public BusBase(Call call) {
        this(call, null);
    }

    public BusBase(Call call, T[] value) {
        this.call = call;
        this.value = value;
    }

    public T getValue() {
        return value == null || value.length == 0 ? null : value[0];
    }

    public T[] getValues() {
        return value;
    }

    public Call getCall() {
        return call;
    }

    public boolean hasValues() {
        return getValue() != null;
    }

    @NonNull
    @Override
    public String toString() {
        return "BusBase{" + "call=" + call + '}';
    }
}