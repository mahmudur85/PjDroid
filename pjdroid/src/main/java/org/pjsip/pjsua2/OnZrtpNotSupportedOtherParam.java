/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.1
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.pjsip.pjsua2;

public class OnZrtpNotSupportedOtherParam {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected OnZrtpNotSupportedOtherParam(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(OnZrtpNotSupportedOtherParam obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        pjsua2JNI.delete_OnZrtpNotSupportedOtherParam(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setCallId(int value) {
    pjsua2JNI.OnZrtpNotSupportedOtherParam_callId_set(swigCPtr, this, value);
  }

  public int getCallId() {
    return pjsua2JNI.OnZrtpNotSupportedOtherParam_callId_get(swigCPtr, this);
  }

  public OnZrtpNotSupportedOtherParam() {
    this(pjsua2JNI.new_OnZrtpNotSupportedOtherParam(), true);
  }

}
