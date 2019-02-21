package com.iccsoftware.smartfpscanner.Model;

/**
 * Created by Scarecrow on 1/29/2019.
 */

public class FingerPrintModel {

    private String userId;
    private String rankNumber;
    private String lhth; //Left hand thumb
    private String lhff; //Left hand fore finger
    private String lhif; //Left hand index finger
    private String lhrf; //Left hand ring finger
    private String lhtf; //Left hand tiny finger
    private String rhth; //Right hand thumb
    private String rhff; //Right hand fore finger
    private String rhif; //Right hand index finger
    private String rhrf; //Right hand ring finger
    private String rhtf; //Right hand tiny finger

    public FingerPrintModel() {
    }

    public FingerPrintModel(String userId, String rankNumber, String lhth, String lhff, String lhif, String lhrf, String lhtf, String rhth, String rhff, String rhif, String rhrf, String rhtf) {
        this.userId = userId;
        this.rankNumber = rankNumber;
        this.lhth = lhth;
        this.lhff = lhff;
        this.lhif = lhif;
        this.lhrf = lhrf;
        this.lhtf = lhtf;
        this.rhth = rhth;
        this.rhff = rhff;
        this.rhif = rhif;
        this.rhrf = rhrf;
        this.rhtf = rhtf;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRankNumber() {
        return rankNumber;
    }

    public void setRankNumber(String rankNumber) {
        this.rankNumber = rankNumber;
    }

    public String getLhth() {
        return lhth;
    }

    public void setLhth(String lhth) {
        this.lhth = lhth;
    }

    public String getLhff() {
        return lhff;
    }

    public void setLhff(String lhff) {
        this.lhff = lhff;
    }

    public String getLhif() {
        return lhif;
    }

    public void setLhif(String lhif) {
        this.lhif = lhif;
    }

    public String getLhrf() {
        return lhrf;
    }

    public void setLhrf(String lhrf) {
        this.lhrf = lhrf;
    }

    public String getLhtf() {
        return lhtf;
    }

    public void setLhtf(String lhtf) {
        this.lhtf = lhtf;
    }

    public String getRhth() {
        return rhth;
    }

    public void setRhth(String rhth) {
        this.rhth = rhth;
    }

    public String getRhff() {
        return rhff;
    }

    public void setRhff(String rhff) {
        this.rhff = rhff;
    }

    public String getRhif() {
        return rhif;
    }

    public void setRhif(String rhif) {
        this.rhif = rhif;
    }

    public String getRhrf() {
        return rhrf;
    }

    public void setRhrf(String rhrf) {
        this.rhrf = rhrf;
    }

    public String getRhtf() {
        return rhtf;
    }

    public void setRhtf(String rhtf) {
        this.rhtf = rhtf;
    }
}
