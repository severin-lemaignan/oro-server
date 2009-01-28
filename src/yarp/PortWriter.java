/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.35
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package yarp;

public class PortWriter {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected PortWriter(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(PortWriter obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      yarpJNI.delete_PortWriter(swigCPtr);
    }
    swigCPtr = 0;
  }

  public boolean write(ConnectionWriter connection) {
    return yarpJNI.PortWriter_write(swigCPtr, this, ConnectionWriter.getCPtr(connection), connection);
  }

  public void onCompletion() {
    yarpJNI.PortWriter_onCompletion(swigCPtr, this);
  }

  public void onCommencement() {
    yarpJNI.PortWriter_onCommencement(swigCPtr, this);
  }

}
