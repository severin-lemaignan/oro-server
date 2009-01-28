/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.35
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package yarp;

public class ImageRgb extends Image {
  private long swigCPtr;

  protected ImageRgb(long cPtr, boolean cMemoryOwn) {
    super(yarpJNI.SWIGImageRgbUpcast(cPtr), cMemoryOwn);
    swigCPtr = cPtr;
  }

  protected static long getCPtr(ImageRgb obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      yarpJNI.delete_ImageRgb(swigCPtr);
    }
    swigCPtr = 0;
    super.delete();
  }

  public int getPixelSize() {
    return yarpJNI.ImageRgb_getPixelSize(swigCPtr, this);
  }

  public int getPixelCode() {
    return yarpJNI.ImageRgb_getPixelCode(swigCPtr, this);
  }

  public PixelRgb pixel(int x, int y) {
    return new PixelRgb(yarpJNI.ImageRgb_pixel(swigCPtr, this, x, y), false);
  }

  public PixelRgb access(int x, int y) {
    return new PixelRgb(yarpJNI.ImageRgb_access(swigCPtr, this, x, y), false);
  }

  public PixelRgb safePixel(int x, int y) {
    return new PixelRgb(yarpJNI.ImageRgb_safePixel(swigCPtr, this, x, y), false);
  }

  public ImageRgb() {
    this(yarpJNI.new_ImageRgb(), true);
  }

}
