/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.35
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package yarp;

public class SearchMonitor {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected SearchMonitor(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(SearchMonitor obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      yarpJNI.delete_SearchMonitor(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void report(SearchReport report, String context) {
    yarpJNI.SearchMonitor_report(swigCPtr, this, SearchReport.getCPtr(report), report, context);
  }

}
