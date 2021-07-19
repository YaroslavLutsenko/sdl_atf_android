package com.sdl.atfandroid.transport.utl;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.sdl.atfandroid.transport.enums.TransportType;

public class TransportRecord implements Parcelable {
    private TransportType type;
    private String address;
    private final int sessionId;

    public TransportRecord(TransportType type, String address, int sessionId) {
        this.type = type;
        this.address = address;
        this.sessionId = sessionId;
    }

    public TransportType getType() {
        return type;
    }

    public String getAddress() {
        return address;
    }

    public int getSessionId() { return sessionId; }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof TransportRecord) {
            TransportRecord record = (TransportRecord) obj;
            return record.type != null && record.type.equals(type)  //Transport type is the same
                    && ((record.address == null && address == null) //Both addresses are null
                    || (record.address != null && record.address.equals(address)) //Or they match
                    && record.sessionId == sessionId); // session is the same
        }

        return super.equals(obj);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Transport Type: ");
        builder.append(type.name());
        builder.append(" Address: ");
        builder.append(address);
        builder.append(" SessionId: ");
        builder.append(sessionId);
        return builder.toString();
    }

    public TransportRecord(Parcel p) {
        if (p.readInt() == 1) { //We should have a transport type attached
            String transportName = p.readString();
            if (transportName != null) {
                this.type = TransportType.valueOf(transportName);
            }
        }

        if (p.readInt() == 1) { //We should have a transport address attached
            address = p.readString();
        }

        sessionId = p.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type != null ? 1 : 0);
        if (type != null) {
            dest.writeString(type.name());
        }

        dest.writeInt(address != null ? 1 : 0);
        if (address != null) {
            dest.writeString(address);
        }

        dest.writeInt(sessionId);
    }



    public static final Parcelable.Creator<TransportRecord> CREATOR = new Parcelable.Creator<TransportRecord>() {
        public TransportRecord createFromParcel(Parcel in) {
            return new TransportRecord(in);
        }

        @Override
        public TransportRecord[] newArray(int size) {
            return new TransportRecord[size];
        }

    };
}
