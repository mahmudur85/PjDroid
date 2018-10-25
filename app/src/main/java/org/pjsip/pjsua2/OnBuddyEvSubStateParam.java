/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.8
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.pjsip.pjsua2;

public class OnBuddyEvSubStateParam {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected OnBuddyEvSubStateParam(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(OnBuddyEvSubStateParam obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        pjsua2JNI.delete_OnBuddyEvSubStateParam(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setE(SipEvent value) {
    pjsua2JNI.OnBuddyEvSubStateParam_e_set(swigCPtr, this, SipEvent.getCPtr(value), value);
  }

  public SipEvent getE() {
    long cPtr = pjsua2JNI.OnBuddyEvSubStateParam_e_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SipEvent(cPtr, false);
  }

  public OnBuddyEvSubStateParam() {
    this(pjsua2JNI.new_OnBuddyEvSubStateParam(), true);
  }

}
