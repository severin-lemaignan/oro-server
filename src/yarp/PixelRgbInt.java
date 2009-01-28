/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.35
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package yarp;

public class PixelRgbInt {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected PixelRgbInt(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(PixelRgbInt obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      yarpJNI.delete_PixelRgbInt(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setR(SWIGTYPE_p_yarp__os__NetInt32 value) {
    yarpJNI.PixelRgbInt_r_set(swigCPtr, this, SWIGTYPE_p_yarp__os__NetInt32.getCPtr(value));
  }

  public SWIGTYPE_p_yarp__os__NetInt32 getR() {
    return new SWIGTYPE_p_yarp__os__NetInt32(yarpJNI.PixelRgbInt_r_get(swigCPtr, this), true);
  }

  public void setG(SWIGTYPE_p_yarp__os__NetInt32 value) {
    yarpJNI.PixelRgbInt_g_set(swigCPtr, this, SWIGTYPE_p_yarp__os__NetInt32.getCPtr(value));
  }

  public SWIGTYPE_p_yarp__os__NetInt32 getG() {
    return new SWIGTYPE_p_yarp__os__NetInt32(yarpJNI.PixelRgbInt_g_get(swigCPtr, this), true);
  }

  public void setB(SWIGTYPE_p_yarp__os__NetInt32 value) {
    yarpJNI.PixelRgbInt_b_set(swigCPtr, this, SWIGTYPE_p_yarp__os__NetInt32.getCPtr(value));
  }

  public SWIGTYPE_p_yarp__os__NetInt32 getB() {
    return new SWIGTYPE_p_yarp__os__NetInt32(yarpJNI.PixelRgbInt_b_get(swigCPtr, this), true);
  }

  public PixelRgbInt() {
    this(yarpJNI.new_PixelRgbInt__SWIG_0(), true);
  }

  public PixelRgbInt(int n_r, int n_g, int n_b) {
    this(yarpJNI.new_PixelRgbInt__SWIG_1(n_r, n_g, n_b), true);
  }

}
