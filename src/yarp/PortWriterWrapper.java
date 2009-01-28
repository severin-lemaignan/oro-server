/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.35
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package yarp;

public class PortWriterWrapper extends PortWriter {
  private long swigCPtr;

  protected PortWriterWrapper(long cPtr, boolean cMemoryOwn) {
    super(yarpJNI.SWIGPortWriterWrapperUpcast(cPtr), cMemoryOwn);
    swigCPtr = cPtr;
  }

  protected static long getCPtr(PortWriterWrapper obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      yarpJNI.delete_PortWriterWrapper(swigCPtr);
    }
    swigCPtr = 0;
    super.delete();
  }

  public PortWriter getInternal() {
    long cPtr = yarpJNI.PortWriterWrapper_getInternal(swigCPtr, this);
    return (cPtr == 0) ? null : new PortWriter(cPtr, false);
  }

}
