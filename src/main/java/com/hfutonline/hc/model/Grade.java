package com.hfutonline.hc.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author chenliangliang
 * @date 2019/3/18
 */
@Data
public class Grade implements Serializable {

    private static final long serialVersionUID = -7514756472291286408L;

    private String courseName;

    private String courseCode;

    private String classCode;

    private String credit;

    private String gradePoint;

    private String score;

    private String detail="";

}
