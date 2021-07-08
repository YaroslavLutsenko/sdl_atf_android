package com.sdl.atfandroid.transport.enums;


/**
 * Defines available types of the transports.
 */
public enum TransportType {
    BLUETOOTH,
    TCP,
    USB;

    public static TransportType valueForString(String value) {
        try {
            return valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
}
