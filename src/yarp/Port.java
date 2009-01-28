/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.35
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package yarp;

public class Port extends Contactable {
  private long swigCPtr;

  protected Port(long cPtr, boolean cMemoryOwn) {
    super(yarpJNI.SWIGPortUpcast(cPtr), cMemoryOwn);
    swigCPtr = cPtr;
  }

  protected static long getCPtr(Port obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      yarpJNI.delete_Port(swigCPtr);
    }
    swigCPtr = 0;
    super.delete();
  }

  public Port() {
    this(yarpJNI.new_Port(), true);
  }

  public boolean addOutput(String name) {
    return yarpJNI.Port_addOutput__SWIG_0(swigCPtr, this, name);
  }

  public boolean addOutput(String name, String carrier) {
    return yarpJNI.Port_addOutput__SWIG_1(swigCPtr, this, name, carrier);
  }

  public boolean addOutput(Contact contact) {
    return yarpJNI.Port_addOutput__SWIG_2(swigCPtr, this, Contact.getCPtr(contact), contact);
  }

  public void close() {
    yarpJNI.Port_close(swigCPtr, this);
  }

  public void interrupt() {
    yarpJNI.Port_interrupt(swigCPtr, this);
  }

  public Contact where() {
    return new Contact(yarpJNI.Port_where(swigCPtr, this), true);
  }

  public boolean write(PortWriter writer, PortWriter callback) {
    return yarpJNI.Port_write__SWIG_0(swigCPtr, this, PortWriter.getCPtr(writer), writer, PortWriter.getCPtr(callback), callback);
  }

  public boolean write(PortWriter writer) {
    return yarpJNI.Port_write__SWIG_1(swigCPtr, this, PortWriter.getCPtr(writer), writer);
  }

  public boolean write(PortWriter writer, PortReader reader, PortWriter callback) {
    return yarpJNI.Port_write__SWIG_2(swigCPtr, this, PortWriter.getCPtr(writer), writer, PortReader.getCPtr(reader), reader, PortWriter.getCPtr(callback), callback);
  }

  public boolean write(PortWriter writer, PortReader reader) {
    return yarpJNI.Port_write__SWIG_3(swigCPtr, this, PortWriter.getCPtr(writer), writer, PortReader.getCPtr(reader), reader);
  }

  public boolean read(PortReader reader, boolean willReply) {
    return yarpJNI.Port_read__SWIG_0(swigCPtr, this, PortReader.getCPtr(reader), reader, willReply);
  }

  public boolean read(PortReader reader) {
    return yarpJNI.Port_read__SWIG_1(swigCPtr, this, PortReader.getCPtr(reader), reader);
  }

  public boolean reply(PortWriter writer) {
    return yarpJNI.Port_reply(swigCPtr, this, PortWriter.getCPtr(writer), writer);
  }

  public void setReader(PortReader reader) {
    yarpJNI.Port_setReader(swigCPtr, this, PortReader.getCPtr(reader), reader);
  }

  public void setReaderCreator(PortReaderCreator creator) {
    yarpJNI.Port_setReaderCreator(swigCPtr, this, PortReaderCreator.getCPtr(creator), creator);
  }

  public void enableBackgroundWrite(boolean backgroundFlag) {
    yarpJNI.Port_enableBackgroundWrite(swigCPtr, this, backgroundFlag);
  }

  public boolean isWriting() {
    return yarpJNI.Port_isWriting(swigCPtr, this);
  }

  public boolean setEnvelope(PortWriter envelope) {
    return yarpJNI.Port_setEnvelope(swigCPtr, this, PortWriter.getCPtr(envelope), envelope);
  }

  public boolean getEnvelope(PortReader envelope) {
    return yarpJNI.Port_getEnvelope(swigCPtr, this, PortReader.getCPtr(envelope), envelope);
  }

  public int getInputCount() {
    return yarpJNI.Port_getInputCount(swigCPtr, this);
  }

  public int getOutputCount() {
    return yarpJNI.Port_getOutputCount(swigCPtr, this);
  }

  public void getReport(PortReport reporter) {
    yarpJNI.Port_getReport(swigCPtr, this, PortReport.getCPtr(reporter), reporter);
  }

  public void setReporter(PortReport reporter) {
    yarpJNI.Port_setReporter(swigCPtr, this, PortReport.getCPtr(reporter), reporter);
  }

  public void setAdminMode(boolean adminMode) {
    yarpJNI.Port_setAdminMode__SWIG_0(swigCPtr, this, adminMode);
  }

  public void setAdminMode() {
    yarpJNI.Port_setAdminMode__SWIG_1(swigCPtr, this);
  }

  public boolean write(Bottle data) {
    return yarpJNI.Port_write__SWIG_4(swigCPtr, this, Bottle.getCPtr(data), data);
  }

  public boolean write(Property data) {
    return yarpJNI.Port_write__SWIG_5(swigCPtr, this, Property.getCPtr(data), data);
  }

  public boolean write(ImageRgb data) {
    return yarpJNI.Port_write__SWIG_6(swigCPtr, this, ImageRgb.getCPtr(data), data);
  }

  public boolean write(ImageFloat data) {
    return yarpJNI.Port_write__SWIG_7(swigCPtr, this, ImageFloat.getCPtr(data), data);
  }

}
