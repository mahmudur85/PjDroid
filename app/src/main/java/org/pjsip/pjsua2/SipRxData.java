/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.8
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.pjsip.pjsua2;

public class SipRxData {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected SipRxData(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(SipRxData obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        pjsua2JNI.delete_SipRxData(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setInfo(String value) {
    pjsua2JNI.SipRxData_info_set(swigCPtr, this, value);
  }

  public String getInfo() {
    return pjsua2JNI.SipRxData_info_get(swigCPtr, this);
  }

  public void setWholeMsg(String value) {
    pjsua2JNI.SipRxData_wholeMsg_set(swigCPtr, this, value);
  }

  public String getWholeMsg() {
    return pjsua2JNI.SipRxData_wholeMsg_get(swigCPtr, this);
  }

  public void setSrcAddress(String value) {
    pjsua2JNI.SipRxData_srcAddress_set(swigCPtr, this, value);
  }

  public String getSrcAddress() {
    return pjsua2JNI.SipRxData_srcAddress_get(swigCPtr, this);
  }

  public void setPjRxData(SWIGTYPE_p_void value) {
    pjsua2JNI.SipRxData_pjRxData_set(swigCPtr, this, SWIGTYPE_p_void.getCPtr(value));
  }

  public SWIGTYPE_p_void getPjRxData() {
    long cPtr = pjsua2JNI.SipRxData_pjRxData_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_void(cPtr, false);
  }

  public SipRxData() {
    this(pjsua2JNI.new_SipRxData(), true);
  }

}
