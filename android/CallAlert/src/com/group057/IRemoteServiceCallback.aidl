// IRemoteService.aidl
package com.group057;

// Declare any non-default types here with import statements

/** Example service interface */
interface IRemoteServiceCallback {
    /** Request the process ID of this service, to do evil things with it. */
    int getPid();

    void valueChanged(int value);
}