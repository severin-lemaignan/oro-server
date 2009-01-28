/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.35
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package yarp;

public class TypedReaderCallbackProperty {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected TypedReaderCallbackProperty(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(TypedReaderCallbackProperty obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      yarpJNI.delete_TypedReaderCallbackProperty(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void onRead(Property datum) {
    yarpJNI.TypedReaderCallbackProperty_onRead__SWIG_0(swigCPtr, this, Property.getCPtr(datum), datum);
  }

  public void onRead(Property datum, TypedReaderProperty reader) {
    yarpJNI.TypedReaderCallbackProperty_onRead__SWIG_1(swigCPtr, this, Property.getCPtr(datum), datum, TypedReaderProperty.getCPtr(reader), reader);
  }

  public TypedReaderCallbackProperty() {
    this(yarpJNI.new_TypedReaderCallbackProperty(), true);
  }

}
