// IRemoteService.aidl
package com.group057;

// Declare any non-default types here with import statements
import com.group057.IRemoteServiceCallback;
import android.graphics.Bitmap;

/** Example service interface */
interface IRemoteService {
    /** Request the process ID of this service, to do evil things with it. */
    int getPid();

    /** Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);

    void imageRequestQueue(in byte[] image, int time, byte priority, String packageName); 
    void bitmapRequestQueue(in Bitmap image, int time, byte priority, String packageName);        
    void imageRequestCancelCurrent(String packageName);        
    void imageRequestCancelAll(String packageName);
    void deviceChosenConnect(String address);
    void registerCallback(IRemoteServiceCallback client);
    void unregisterCallback(IRemoteServiceCallback client);
   	void valueSetClient(int value);
}